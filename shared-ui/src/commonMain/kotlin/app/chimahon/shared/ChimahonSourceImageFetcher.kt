package app.chimahon.shared

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal enum class SourceImageCachePolicy {
    USE_CACHE,
    REFRESH,
    BYPASS,
}

/**
 * Leaves source-owned image requests untouched when [headers] and [referer] are empty.
 * Supplying either value requests a direct GET with those overrides.
 */
internal data class SourceImageFetchOptions(
    val cachePolicy: SourceImageCachePolicy = SourceImageCachePolicy.USE_CACHE,
    val headers: Map<String, String> = emptyMap(),
    val referer: String? = null,
) {
    internal fun effectiveHeaders(): Map<String, String> {
        if (referer == null || headers.keys.any { it.equals("Referer", ignoreCase = true) }) {
            return headers
        }
        return headers + ("Referer" to referer)
    }
}

internal enum class SourceImageFetchFailure {
    UNSUPPORTED_SOURCE,
    INVALID_REQUEST,
    IMAGE_URL_RESOLUTION,
    HTTP_ERROR,
    EMPTY_RESPONSE,
    DOWNLOAD,
}

internal class SourceImageFetchException(
    val failure: SourceImageFetchFailure,
    val sourceId: Long,
    val sourceName: String,
    val pageIndex: Int,
    val httpStatus: Int? = null,
    detail: String? = null,
    cause: Throwable? = null,
) : Exception(
    buildString {
        append("Image fetch failed for ")
        append(sourceName)
        append(" page ")
        append(pageIndex + 1)
        append(": ")
        append(
            when (failure) {
                SourceImageFetchFailure.UNSUPPORTED_SOURCE -> "source image API is unavailable"
                SourceImageFetchFailure.INVALID_REQUEST -> "the image request is invalid"
                SourceImageFetchFailure.IMAGE_URL_RESOLUTION -> "image URL resolution failed"
                SourceImageFetchFailure.HTTP_ERROR -> "HTTP request failed"
                SourceImageFetchFailure.EMPTY_RESPONSE -> "the image response was empty"
                SourceImageFetchFailure.DOWNLOAD -> "image download failed"
            },
        )
        httpStatus?.let {
            append(" (HTTP ")
            append(it)
            append(')')
        }
        detail?.takeIf(String::isNotBlank)?.let {
            append(": ")
            append(it)
        }
        append('.')
    },
    cause,
)

internal data class SourceImageCacheStats(
    val entryCount: Int,
    val byteCount: Long,
    val inFlightCount: Int,
)

private val sourcePageImageFetcher = SourcePageImageFetcher(
    maxCacheEntries = 24,
    maxCacheBytes = 32L * 1024L * 1024L,
    loadUncached = ::loadSourcePageImage,
)

internal suspend fun fetchSourcePageImage(
    source: CatalogueSource,
    page: Page,
    options: SourceImageFetchOptions = SourceImageFetchOptions(),
): ByteArray {
    return try {
        sourcePageImageFetcher.fetch(source, page, options).also {
            page.status = Page.State.Ready
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        page.status = Page.State.Error(error)
        throw error
    }
}

internal suspend fun prefetchSourcePageImages(
    source: CatalogueSource,
    pages: List<Page>,
    options: SourceImageFetchOptions = SourceImageFetchOptions(),
    maxConcurrency: Int = 2,
): List<Result<Unit>> {
    require(maxConcurrency > 0) { "maxConcurrency must be greater than zero." }
    val semaphore = Semaphore(maxConcurrency)
    return coroutineScope {
        pages.map { page ->
            async {
                semaphore.withPermit {
                    try {
                        fetchSourcePageImage(source, page, options)
                        Result.success(Unit)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        Result.failure(error)
                    }
                }
            }
        }.awaitAll()
    }
}

internal suspend fun clearSourcePageImageCache(sourceId: Long? = null) {
    sourcePageImageFetcher.clear(sourceId)
}

internal suspend fun sourcePageImageCacheStats(): SourceImageCacheStats {
    return sourcePageImageFetcher.stats()
}

internal fun validateLoadedSourcePageImage(
    source: CatalogueSource,
    page: Page,
    bytes: ByteArray,
): ByteArray {
    if (bytes.isEmpty()) {
        throw SourceImageFetchException(
            failure = SourceImageFetchFailure.EMPTY_RESPONSE,
            sourceId = source.id,
            sourceName = source.name,
            pageIndex = page.index,
        )
    }
    return bytes
}

internal expect suspend fun loadSourcePageImage(
    source: CatalogueSource,
    page: Page,
    options: SourceImageFetchOptions,
): ByteArray
