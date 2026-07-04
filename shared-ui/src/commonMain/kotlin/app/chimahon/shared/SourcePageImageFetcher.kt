package app.chimahon.shared

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SourcePageImageFetcher(
    private val maxCacheEntries: Int,
    private val maxCacheBytes: Long,
    private val loadUncached: suspend (
        source: CatalogueSource,
        page: Page,
        options: SourceImageFetchOptions,
    ) -> ByteArray,
) {
    private val mutex = Mutex()
    private val cache = LinkedHashMap<CacheKey, ByteArray>()
    private val inFlight = mutableMapOf<CacheKey, CompletableDeferred<ByteArray>>()
    private var cachedByteCount = 0L

    init {
        require(maxCacheEntries > 0) { "maxCacheEntries must be greater than zero." }
        require(maxCacheBytes > 0) { "maxCacheBytes must be greater than zero." }
    }

    suspend fun fetch(
        source: CatalogueSource,
        page: Page,
        options: SourceImageFetchOptions,
    ): ByteArray {
        validateOptions(source, page, options)
        if (options.cachePolicy == SourceImageCachePolicy.BYPASS) {
            return loadAndValidate(source, page, options)
        }

        val key = CacheKey.from(source, page, options)
        val lookup = mutex.withLock {
            if (options.cachePolicy == SourceImageCachePolicy.USE_CACHE) {
                cache.remove(key)?.let { cached ->
                    cache[key] = cached
                    return@withLock Lookup.Cached(cached.copyOf())
                }
            }

            inFlight[key]?.let { return@withLock Lookup.Pending(it) }

            val pending = CompletableDeferred<ByteArray>()
            inFlight[key] = pending
            Lookup.Owner(pending)
        }

        return when (lookup) {
            is Lookup.Cached -> lookup.bytes
            is Lookup.Pending -> lookup.result.await().copyOf()
            is Lookup.Owner -> loadOwned(source, page, options, key, lookup.result)
        }
    }

    suspend fun clear(sourceId: Long?) {
        mutex.withLock {
            if (sourceId == null) {
                cache.clear()
                cachedByteCount = 0L
                return
            }

            val iterator = cache.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.key.sourceId == sourceId) {
                    cachedByteCount -= entry.value.size
                    iterator.remove()
                }
            }
        }
    }

    suspend fun stats(): SourceImageCacheStats {
        return mutex.withLock {
            SourceImageCacheStats(
                entryCount = cache.size,
                byteCount = cachedByteCount,
                inFlightCount = inFlight.size,
            )
        }
    }

    private suspend fun loadOwned(
        source: CatalogueSource,
        page: Page,
        options: SourceImageFetchOptions,
        key: CacheKey,
        result: CompletableDeferred<ByteArray>,
    ): ByteArray {
        try {
            val bytes = loadAndValidate(source, page, options)
            val loadedKey = CacheKey.from(source, page, options)
            mutex.withLock {
                putCacheEntry(loadedKey, bytes)
            }
            result.complete(bytes.copyOf())
            return bytes.copyOf()
        } catch (error: Throwable) {
            result.completeExceptionally(error)
            throw error
        } finally {
            mutex.withLock {
                if (inFlight[key] === result) {
                    inFlight.remove(key)
                }
            }
        }
    }

    private suspend fun loadAndValidate(
        source: CatalogueSource,
        page: Page,
        options: SourceImageFetchOptions,
    ): ByteArray {
        val bytes = try {
            loadUncached(source, page, options)
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

        return validateLoadedSourcePageImage(source, page, bytes)
    }

    private fun putCacheEntry(key: CacheKey, bytes: ByteArray) {
        cache.remove(key)?.let { cachedByteCount -= it.size }
        if (bytes.size > maxCacheBytes) return

        cache[key] = bytes.copyOf()
        cachedByteCount += bytes.size
        while (cache.size > maxCacheEntries || cachedByteCount > maxCacheBytes) {
            val eldest = cache.entries.firstOrNull() ?: break
            cachedByteCount -= eldest.value.size
            cache.remove(eldest.key)
        }
    }

    private fun validateOptions(
        source: CatalogueSource,
        page: Page,
        options: SourceImageFetchOptions,
    ) {
        val invalidHeader = options.headers.entries.firstOrNull { (name, value) ->
            name.isBlank() || name.contains('\r') || name.contains('\n') ||
                value.contains('\r') || value.contains('\n')
        }
        val invalidReferer = options.referer?.let {
            it.isBlank() || it.contains('\r') || it.contains('\n')
        } == true
        if (invalidHeader != null || invalidReferer) {
            throw SourceImageFetchException(
                failure = SourceImageFetchFailure.INVALID_REQUEST,
                sourceId = source.id,
                sourceName = source.name,
                pageIndex = page.index,
                detail = "headers and referer must be non-blank and must not contain line breaks",
            )
        }
    }

    private data class CacheKey(
        val sourceId: Long,
        val pageIndex: Int,
        val pageLocator: String,
        val headers: List<Pair<String, String>>,
    ) {
        companion object {
            fun from(
                source: CatalogueSource,
                page: Page,
                options: SourceImageFetchOptions,
            ): CacheKey {
                val headers = options.effectiveHeaders()
                    .map { (name, value) -> name.lowercase() to value }
                    .sortedWith(compareBy<Pair<String, String>>({ it.first }, { it.second }))
                return CacheKey(
                    sourceId = source.id,
                    pageIndex = page.index,
                    pageLocator = page.url.takeIf(String::isNotBlank)
                        ?: page.imageUrl?.takeIf(String::isNotBlank)
                        ?: "index:${page.index}",
                    headers = headers,
                )
            }
        }
    }

    private sealed interface Lookup {
        data class Cached(val bytes: ByteArray) : Lookup
        data class Pending(val result: CompletableDeferred<ByteArray>) : Lookup
        data class Owner(val result: CompletableDeferred<ByteArray>) : Lookup
    }
}
