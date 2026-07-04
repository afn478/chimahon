package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import tachiyomi.core.extensions.LoadedScriptExtension
import tachiyomi.core.extensions.ScriptChapter
import tachiyomi.core.extensions.ScriptExtensionInvoker
import tachiyomi.core.extensions.ScriptHttpRequest
import tachiyomi.core.extensions.ScriptHttpResponse
import tachiyomi.core.extensions.ScriptManga
import tachiyomi.core.extensions.ScriptMangasPage
import tachiyomi.core.extensions.ScriptPage
import tachiyomi.core.extensions.ScriptParseInput
import tachiyomi.core.extensions.ScriptSourceManifest

class ScriptSourceFactory(
    private val extension: LoadedScriptExtension,
    private val invoker: ScriptExtensionInvoker,
) : SourceFactory {
    override fun createSources(): List<Source> {
        return extension.manifest.sources.map { source ->
            ScriptHttpSource(extension, source, invoker)
        }
    }
}

internal actual fun createScriptSources(
    extension: LoadedScriptExtension,
    invoker: ScriptExtensionInvoker,
): List<Source> = ScriptSourceFactory(extension, invoker).createSources()

class ScriptHttpSource(
    private val extension: LoadedScriptExtension,
    private val manifest: ScriptSourceManifest,
    private val invoker: ScriptExtensionInvoker,
) : CatalogueSource {
    override val id: Long = manifest.id
    override val name: String = manifest.name
    override val lang: String = manifest.language
    val baseUrl: String = manifest.baseUrl
    override val supportsLatest: Boolean = manifest.supportsLatest
    override val supportsRelatedMangas: Boolean = false

    override suspend fun getPopularManga(page: Int): MangasPage {
        return execute(
            operation = "popularManga",
            arguments = buildJsonObject { put("page", page) },
            deserializer = ScriptMangasPage.serializer(),
        ).toMangasPage()
    }

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        return execute(
            operation = "searchManga",
            arguments = buildJsonObject {
                put("page", page)
                put("query", query)
                put("filters", filters.toJson())
            },
            deserializer = ScriptMangasPage.serializer(),
        ).toMangasPage()
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        return execute(
            operation = "latestUpdates",
            arguments = buildJsonObject { put("page", page) },
            deserializer = ScriptMangasPage.serializer(),
        ).toMangasPage()
    }

    override suspend fun getMangaDetails(manga: SManga): SManga {
        return execute(
            operation = "mangaDetails",
            arguments = buildJsonObject {
                put("manga", manga.toScriptManga().toJson())
            },
            deserializer = ScriptManga.serializer(),
        ).toSManga().apply {
            initialized = true
        }
    }

    override suspend fun getChapterList(manga: SManga): List<SChapter> {
        return execute(
            operation = "chapterList",
            arguments = buildJsonObject {
                put("manga", manga.toScriptManga().toJson())
            },
            deserializer = ListSerializer(ScriptChapter.serializer()),
        ).map(ScriptChapter::toSChapter)
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        return execute(
            operation = "pageList",
            arguments = buildJsonObject {
                put("chapter", chapter.toScriptChapter().toJson())
            },
            deserializer = ListSerializer(ScriptPage.serializer()),
        ).map(ScriptPage::toPage)
    }

    suspend fun getImageUrl(page: Page): String {
        page.imageUrl?.let { return it }
        return execute(
            operation = "imageUrl",
            arguments = buildJsonObject {
                put("page", page.toScriptPage().toJson())
            },
            deserializer = String.serializer(),
        )
    }

    suspend fun getImageBytes(page: Page): ByteArray {
        if (page.imageUrl.isNullOrBlank()) {
            page.imageUrl = getImageUrl(page)
        }
        val arguments = buildJsonObject {
            put("page", page.toScriptPage().toJson())
        }
        val request = runCatching {
            invoker.invoke(
                extension = extension,
                sourceId = id,
                method = "imageRequest",
                arguments = arguments,
                deserializer = ScriptHttpRequest.serializer(),
            )
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            ScriptHttpRequest(url = requireNotNull(page.imageUrl))
        }
        return nativeScriptHttpClient.executeBytes(request.resolveAgainst(baseUrl))
    }

    override fun getFilterList(): FilterList = FilterList()

    private suspend fun <T> execute(
        operation: String,
        arguments: JsonObject,
        deserializer: DeserializationStrategy<T>,
    ): T {
        val request = invoker.invoke(
            extension = extension,
            sourceId = id,
            method = "${operation}Request",
            arguments = arguments,
            deserializer = ScriptHttpRequest.serializer(),
        )
        val response = nativeScriptHttpClient.execute(request.resolveAgainst(baseUrl))
        return invoker.invoke(
            extension = extension,
            sourceId = id,
            method = "${operation}Parse",
            arguments = ScriptParseInput(arguments, response).toJson(),
            deserializer = deserializer,
        )
    }
}

private class NativeScriptHttpClient {
    private val client = HttpClient(CIO) {
        expectSuccess = false
        followRedirects = true
    }

    suspend fun execute(scriptRequest: ScriptHttpRequest): ScriptHttpResponse {
        val response = request(scriptRequest)

        return ScriptHttpResponse(
            status = response.status.value,
            headers = response.headers.entries().associate { (name, values) -> name to values },
            body = response.bodyAsText(),
            finalUrl = response.call.request.url.toString(),
        )
    }

    suspend fun executeBytes(scriptRequest: ScriptHttpRequest): ByteArray {
        return request(scriptRequest).readRawBytes()
    }

    private suspend fun request(scriptRequest: ScriptHttpRequest) = client.request(scriptRequest.url) {
        method = HttpMethod.parse(scriptRequest.method.uppercase())
        headers {
            scriptRequest.headers.forEach { (name, value) ->
                append(name, value)
            }
            if (scriptRequest.headers.keys.none { it.equals(HttpHeaders.UserAgent, ignoreCase = true) }) {
                append(HttpHeaders.UserAgent, NATIVE_USER_AGENT)
            }
        }
        val requestBody = scriptRequest.body
        if (requestBody != null) {
            setBody(requestBody)
            scriptRequest.contentType?.let { contentType ->
                contentType(ContentType.parse(contentType))
            }
        } else if (method.value in METHODS_REQUIRING_BODY) {
            setBody("")
        }
    }
        .also { response ->
            check(response.status.isSuccess()) {
                "HTTP error ${response.status.value}"
            }
        }

    private companion object {
        const val NATIVE_USER_AGENT = "Chimahon Native"
        val METHODS_REQUIRING_BODY = setOf("POST", "PUT", "PATCH")
    }
}

private val nativeScriptHttpClient = NativeScriptHttpClient()

private fun ScriptMangasPage.toMangasPage(): MangasPage = MangasPage(
    mangas = mangas.map(ScriptManga::toSManga),
    hasNextPage = hasNextPage,
)

private fun ScriptManga.toSManga(): SManga = SManga(
    url = url,
    title = title,
    artist = artist,
    author = author,
    description = description,
    genre = genre,
    status = status,
    thumbnail_url = thumbnailUrl,
    initialized = initialized,
)

private fun SManga.toScriptManga(): ScriptManga = ScriptManga(
    url = url,
    title = title,
    artist = artist,
    author = author,
    description = description,
    genre = genre,
    status = status,
    thumbnailUrl = thumbnail_url,
    initialized = initialized,
)

private fun ScriptChapter.toSChapter(): SChapter = SChapter(
    name = name,
    url = url,
    date_upload = dateUpload,
    chapter_number = chapterNumber,
    scanlator = scanlator,
)

private fun SChapter.toScriptChapter(): ScriptChapter = ScriptChapter(
    url = url,
    name = name,
    dateUpload = date_upload,
    chapterNumber = chapter_number,
    scanlator = scanlator,
)

private fun ScriptPage.toPage(): Page = Page(
    index = index,
    url = url,
    imageUrl = imageUrl,
)

private fun Page.toScriptPage(): ScriptPage = ScriptPage(
    index = index,
    url = url,
    imageUrl = imageUrl,
)

private val scriptProtocolJson = Json

private fun ScriptManga.toJson(): JsonObject =
    scriptProtocolJson.encodeToJsonElement(ScriptManga.serializer(), this).jsonObject

private fun ScriptChapter.toJson(): JsonObject =
    scriptProtocolJson.encodeToJsonElement(ScriptChapter.serializer(), this).jsonObject

private fun ScriptPage.toJson(): JsonObject =
    scriptProtocolJson.encodeToJsonElement(ScriptPage.serializer(), this).jsonObject

private fun ScriptParseInput.toJson(): JsonObject =
    scriptProtocolJson.encodeToJsonElement(ScriptParseInput.serializer(), this).jsonObject

private fun FilterList.toJson(): JsonArray = JsonArray(
    map { filter ->
        buildJsonObject {
            put("name", filter.name)
            put("state", filter.state.toJson())
        }
    },
)

private fun Any?.toJson(): JsonElement = when (this) {
    null -> JsonNull
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Filter.Sort.Selection -> buildJsonObject {
        put("index", index)
        put("ascending", ascending)
    }
    is Iterable<*> -> JsonArray(map { it.toJson() })
    else -> JsonPrimitive(toString())
}
