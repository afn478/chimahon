package app.chimahon.shared

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.ScriptHttpSource
import eu.kanade.tachiyomi.source.online.resolveScriptSourceUrl
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

internal actual suspend fun loadSourcePageImage(
    source: CatalogueSource,
    page: Page,
    options: SourceImageFetchOptions,
): ByteArray {
    val scriptSource = source as? ScriptHttpSource
        ?: throw SourceImageFetchException(
            failure = SourceImageFetchFailure.UNSUPPORTED_SOURCE,
            sourceId = source.id,
            sourceName = source.name,
            pageIndex = page.index,
            detail = "iOS requires ScriptHttpSource",
        )

    if (page.imageUrl.isNullOrBlank()) {
        page.status = Page.State.LoadPage
        page.imageUrl = try {
            scriptSource.getImageUrl(page)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw SourceImageFetchException(
                failure = SourceImageFetchFailure.IMAGE_URL_RESOLUTION,
                sourceId = source.id,
                sourceName = source.name,
                pageIndex = page.index,
                detail = error.message,
                cause = error,
            )
        }
    }

    val imageUrl = page.imageUrl?.takeIf(String::isNotBlank)
        ?: throw SourceImageFetchException(
            failure = SourceImageFetchFailure.INVALID_REQUEST,
            sourceId = source.id,
            sourceName = source.name,
            pageIndex = page.index,
            detail = "source returned a blank image URL",
        )

    page.status = Page.State.DownloadImage
    return try {
        val usesSourceOwnedRequest = options.headers.isEmpty() && options.referer == null
        val bytes = if (usesSourceOwnedRequest) {
            scriptSource.getImageBytes(page)
        } else {
            val requestHeaders = options.effectiveHeaders()
            val requestUrl = resolveScriptSourceUrl(scriptSource.baseUrl, imageUrl) ?: imageUrl
            val response = sourceImageHttpClient.request(requestUrl) {
                headers {
                    requestHeaders.forEach { (name, value) ->
                        append(name, value)
                    }
                    if (requestHeaders.keys.none { it.equals(HttpHeaders.UserAgent, ignoreCase = true) }) {
                        append(HttpHeaders.UserAgent, "Chimahon iOS")
                    }
                }
            }
            if (!response.status.isSuccess()) {
                throw SourceImageFetchException(
                    failure = SourceImageFetchFailure.HTTP_ERROR,
                    sourceId = source.id,
                    sourceName = source.name,
                    pageIndex = page.index,
                    httpStatus = response.status.value,
                )
            }
            response.readRawBytes()
        }
        validateLoadedSourcePageImage(source, page, bytes)
    } catch (error: CancellationException) {
        throw error
    } catch (error: SourceImageFetchException) {
        throw error
    } catch (error: Throwable) {
        throw SourceImageFetchException(
            failure = SourceImageFetchFailure.DOWNLOAD,
            sourceId = source.id,
            sourceName = source.name,
            pageIndex = page.index,
            detail = error.message,
            cause = error,
        )
    }
}

private val sourceImageHttpClient = HttpClient(CIO) {
    expectSuccess = false
    followRedirects = true
}
