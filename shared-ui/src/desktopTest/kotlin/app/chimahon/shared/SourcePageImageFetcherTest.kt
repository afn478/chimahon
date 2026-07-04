package app.chimahon.shared

import com.sun.net.httpserver.HttpServer
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ScriptHttpSource
import eu.kanade.tachiyomi.source.online.ScriptSourceFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import tachiyomi.core.extensions.ScriptExtensionInvoker
import tachiyomi.core.extensions.ScriptExtensionLoader
import tachiyomi.core.platform.javascript.DesktopJavaScriptRuntimeFactory
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SourcePageImageFetcherTest {
    @Test
    fun cachesDefensiveCopiesAndHonorsRefreshAndBypassPolicies() = runBlocking {
        var loads = 0
        val fetcher = testFetcher { _, _, _ ->
            loads += 1
            byteArrayOf(loads.toByte(), 2, 3)
        }
        val page = Page(index = 0, imageUrl = "https://example.test/0.jpg")

        val first = fetcher.fetch(TEST_SOURCE, page, SourceImageFetchOptions())
        first[0] = 99
        val cached = fetcher.fetch(TEST_SOURCE, page, SourceImageFetchOptions())
        val refreshed = fetcher.fetch(
            TEST_SOURCE,
            page,
            SourceImageFetchOptions(cachePolicy = SourceImageCachePolicy.REFRESH),
        )
        val bypassed = fetcher.fetch(
            TEST_SOURCE,
            page,
            SourceImageFetchOptions(cachePolicy = SourceImageCachePolicy.BYPASS),
        )
        val cachedAfterBypass = fetcher.fetch(TEST_SOURCE, page, SourceImageFetchOptions())

        assertContentEquals(byteArrayOf(1, 2, 3), cached)
        assertContentEquals(byteArrayOf(2, 2, 3), refreshed)
        assertContentEquals(byteArrayOf(3, 2, 3), bypassed)
        assertContentEquals(byteArrayOf(2, 2, 3), cachedAfterBypass)
        assertEquals(3, loads)
    }

    @Test
    fun coalescesConcurrentRequestsForTheSamePage() = runBlocking {
        var loads = 0
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val fetcher = testFetcher { _, _, _ ->
            loads += 1
            started.complete(Unit)
            release.await()
            byteArrayOf(4, 5, 6)
        }
        val page = Page(index = 2, imageUrl = "https://example.test/2.jpg")

        coroutineScope {
            val first = async { fetcher.fetch(TEST_SOURCE, page, SourceImageFetchOptions()) }
            started.await()
            val second = async { fetcher.fetch(TEST_SOURCE, page, SourceImageFetchOptions()) }
            yield()
            release.complete(Unit)

            assertContentEquals(first.await(), second.await())
        }
        assertEquals(1, loads)
    }

    @Test
    fun cacheKeysIncludeHeadersAndReferer() = runBlocking {
        var loads = 0
        val fetcher = testFetcher { _, _, _ ->
            loads += 1
            byteArrayOf(loads.toByte())
        }
        val page = Page(index = 0, imageUrl = "https://example.test/image.jpg")

        fetcher.fetch(TEST_SOURCE, page, SourceImageFetchOptions(referer = "https://one.test"))
        fetcher.fetch(TEST_SOURCE, page, SourceImageFetchOptions(referer = "https://two.test"))
        fetcher.fetch(
            TEST_SOURCE,
            page,
            SourceImageFetchOptions(headers = mapOf("Referer" to "https://one.test")),
        )

        assertEquals(2, loads)
    }

    @Test
    fun keepsTheCacheKeyStableWhenLoadingResolvesTheImageUrl() = runBlocking {
        var loads = 0
        val fetcher = testFetcher { _, page, _ ->
            loads += 1
            page.imageUrl = "https://cdn.example.test/resolved.jpg"
            byteArrayOf(7, 8, 9)
        }
        val page = Page(index = 0, url = "page-token")

        fetcher.fetch(TEST_SOURCE, page, SourceImageFetchOptions())
        fetcher.fetch(TEST_SOURCE, page, SourceImageFetchOptions())

        assertEquals(1, loads)
    }

    @Test
    fun cachesWithResolvedImageUrlWhenPageHasNoStableUrl() = runBlocking {
        var loads = 0
        val fetcher = testFetcher { _, page, _ ->
            loads += 1
            page.imageUrl = "https://cdn.example.test/resolved-$loads.jpg"
            byteArrayOf(7, 8, 9)
        }
        val page = Page(index = 0)

        fetcher.fetch(TEST_SOURCE, page, SourceImageFetchOptions())
        fetcher.fetch(TEST_SOURCE, page, SourceImageFetchOptions())

        val stats = fetcher.stats()

        assertEquals(1, loads)
        assertEquals(1, stats.entryCount)
        assertEquals(3L, stats.byteCount)
    }

    @Test
    fun reportsEmptyResponsesAndInvalidHeadersClearly() = runBlocking {
        val emptyFetcher = testFetcher { _, _, _ -> byteArrayOf() }
        val page = Page(index = 4, imageUrl = "https://example.test/4.jpg")

        val emptyError = assertFailsWith<SourceImageFetchException> {
            emptyFetcher.fetch(TEST_SOURCE, page, SourceImageFetchOptions())
        }
        val headerError = assertFailsWith<SourceImageFetchException> {
            emptyFetcher.fetch(
                TEST_SOURCE,
                page,
                SourceImageFetchOptions(headers = mapOf("X-Test" to "bad\nvalue")),
            )
        }

        assertEquals(SourceImageFetchFailure.EMPTY_RESPONSE, emptyError.failure)
        assertEquals(SourceImageFetchFailure.INVALID_REQUEST, headerError.failure)
        assertEquals(TEST_SOURCE.id, emptyError.sourceId)
        assertEquals(page.index, emptyError.pageIndex)
    }

    @Test
    fun clearsOnlyTheRequestedSourcesCacheEntries() = runBlocking {
        var loads = 0
        val fetcher = testFetcher { _, _, _ ->
            loads += 1
            byteArrayOf(loads.toByte())
        }
        val otherSource = TestCatalogueSource(id = 43L, name = "Other source")
        val page = Page(index = 0, imageUrl = "https://example.test/image.jpg")

        fetcher.fetch(TEST_SOURCE, page, SourceImageFetchOptions())
        fetcher.fetch(otherSource, page, SourceImageFetchOptions())
        fetcher.clear(TEST_SOURCE.id)

        val stats = fetcher.stats()
        fetcher.fetch(TEST_SOURCE, page, SourceImageFetchOptions())
        fetcher.fetch(otherSource, page, SourceImageFetchOptions())

        assertEquals(1, stats.entryCount)
        assertEquals(3, loads)
    }

    @Test
    fun desktopLoaderHonorsScriptImageRequestDescriptor() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/image") { exchange ->
            if (exchange.requestHeaders.getFirst("X-Script-Image") != "allowed") {
                exchange.sendResponseHeaders(403, -1)
                exchange.close()
                return@createContext
            }

            val response = byteArrayOf(2, 4, 6, 8)
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { body ->
                body.write(response)
            }
        }
        server.start()

        try {
            val baseUrl = "http://127.0.0.1:${server.address.port}"
            val source = runBlocking { scriptSource(baseUrl) }
            val page = Page(index = 0, imageUrl = "$baseUrl/image")

            val bytes = runBlocking {
                loadSourcePageImage(source, page, SourceImageFetchOptions())
            }

            assertContentEquals(byteArrayOf(2, 4, 6, 8), bytes)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun desktopLoaderReportsEmptyScriptImageResponses() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/image") { exchange ->
            if (exchange.requestHeaders.getFirst("X-Script-Image") != "allowed") {
                exchange.sendResponseHeaders(403, -1)
                exchange.close()
                return@createContext
            }

            exchange.sendResponseHeaders(200, -1)
            exchange.close()
        }
        server.start()

        try {
            val baseUrl = "http://127.0.0.1:${server.address.port}"
            val source = runBlocking { scriptSource(baseUrl) }
            val page = Page(index = 3, imageUrl = "$baseUrl/image")

            val error = assertFailsWith<SourceImageFetchException> {
                runBlocking {
                    loadSourcePageImage(source, page, SourceImageFetchOptions())
                }
            }

            assertEquals(SourceImageFetchFailure.EMPTY_RESPONSE, error.failure)
            assertEquals(source.id, error.sourceId)
            assertEquals(page.index, error.pageIndex)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun desktopLoaderResolvesRelativeScriptImageUrlsForHeaderOverrides() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/image") { exchange ->
            if (exchange.requestHeaders.getFirst("Referer") != "https://reader.example") {
                exchange.sendResponseHeaders(403, -1)
                exchange.close()
                return@createContext
            }

            val response = byteArrayOf(5, 10, 15, 20)
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { body ->
                body.write(response)
            }
        }
        server.start()

        try {
            val baseUrl = "http://127.0.0.1:${server.address.port}"
            val source = runBlocking { scriptSource(baseUrl) }
            val page = Page(index = 0, imageUrl = "/image")

            val bytes = runBlocking {
                loadSourcePageImage(
                    source = source,
                    page = page,
                    options = SourceImageFetchOptions(referer = "https://reader.example"),
                )
            }

            assertContentEquals(byteArrayOf(5, 10, 15, 20), bytes)
        } finally {
            server.stop(0)
        }
    }

    private fun testFetcher(
        loader: suspend (
            source: CatalogueSource,
            page: Page,
            options: SourceImageFetchOptions,
        ) -> ByteArray,
    ): SourcePageImageFetcher {
        return SourcePageImageFetcher(
            maxCacheEntries = 4,
            maxCacheBytes = 1_024,
            loadUncached = loader,
        )
    }

    private companion object {
        val TEST_SOURCE = TestCatalogueSource(id = 42L, name = "Test source")

        suspend fun scriptSource(baseUrl: String): ScriptHttpSource {
            val loader = ScriptExtensionLoader(DesktopJavaScriptRuntimeFactory)
            val extension = loader.load(
                """
                module.exports = {
                    manifest: {
                        id: "org.chimahon.shared-image-test",
                        name: "Shared Image Test",
                        version: "1.0.0",
                        sources: [{
                            id: 103,
                            name: "Shared Image Source",
                            language: "en",
                            baseUrl: "$baseUrl"
                        }]
                    },
                    sources: {
                        "103": {
                            imageRequest: function (input) {
                                return {
                                    url: input.page.imageUrl,
                                    headers: {
                                        "X-Script-Image": "allowed"
                                    }
                                };
                            }
                        }
                    }
                };
                """.trimIndent(),
            )
            return ScriptSourceFactory(
                extension = extension,
                invoker = ScriptExtensionInvoker(DesktopJavaScriptRuntimeFactory),
            ).createSources().single() as ScriptHttpSource
        }
    }
}

private class TestCatalogueSource(
    override val id: Long,
    override val name: String,
) : CatalogueSource {
    override val lang: String = "en"
    override val supportsLatest: Boolean = false

    override suspend fun getPopularManga(page: Int): MangasPage = unsupported()

    override suspend fun getSearchManga(
        page: Int,
        query: String,
        filters: FilterList,
    ): MangasPage = unsupported()

    override suspend fun getLatestUpdates(page: Int): MangasPage = unsupported()

    override fun getFilterList(): FilterList = FilterList()

    override suspend fun getMangaDetails(manga: SManga): SManga = unsupported()

    override suspend fun getChapterList(manga: SManga): List<SChapter> = unsupported()

    override suspend fun getPageList(chapter: SChapter): List<Page> = unsupported()

    private fun <T> unsupported(): T = error("Not used by source image fetcher tests.")
}
