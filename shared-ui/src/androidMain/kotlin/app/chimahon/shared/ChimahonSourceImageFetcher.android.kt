package app.chimahon.shared

import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.ScriptHttpSource
import eu.kanade.tachiyomi.source.online.resolveScriptSourceUrl
import kotlinx.coroutines.CancellationException
import okhttp3.Request

internal actual suspend fun loadSourcePageImage(
    source: CatalogueSource,
    page: Page,
    options: SourceImageFetchOptions,
): ByteArray {
    val httpSource = source as? HttpSource
        ?: throw SourceImageFetchException(
            failure = SourceImageFetchFailure.UNSUPPORTED_SOURCE,
            sourceId = source.id,
            sourceName = source.name,
            pageIndex = page.index,
            detail = "Android requires HttpSource",
        )

    if (page.imageUrl.isNullOrBlank()) {
        page.status = Page.State.LoadPage
        page.imageUrl = try {
            httpSource.getImageUrl(page)
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
        val bytes = if (usesSourceOwnedRequest && httpSource is ScriptHttpSource) {
            httpSource.getImageBytes(page)
        } else {
            val response = if (usesSourceOwnedRequest) {
                httpSource.getImage(page)
            } else {
                val headers = httpSource.headers.newBuilder().apply {
                    options.effectiveHeaders().forEach { (name, value) ->
                        set(name, value)
                    }
                }.build()
                val requestUrl = if (httpSource is ScriptHttpSource) {
                    resolveScriptSourceUrl(httpSource.baseUrl, imageUrl) ?: imageUrl
                } else {
                    imageUrl
                }
                val request = Request.Builder()
                    .url(requestUrl)
                    .headers(headers)
                    .get()
                    .build()
                httpSource.client.newCall(request).awaitSuccess()
            }
            response.use { it.body.bytes() }
        }
        validateLoadedSourcePageImage(source, page, bytes)
    } catch (error: CancellationException) {
        throw error
    } catch (error: HttpException) {
        throw SourceImageFetchException(
            failure = SourceImageFetchFailure.HTTP_ERROR,
            sourceId = source.id,
            sourceName = source.name,
            pageIndex = page.index,
            httpStatus = error.code,
            cause = error,
        )
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
