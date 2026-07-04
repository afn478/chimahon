package com.canopus.chimareader.data.epub

import tachiyomi.domain.reader.service.NovelEpubBookPolicy
import tachiyomi.domain.reader.service.NovelReaderCharacterCountPolicy
import tachiyomi.domain.reader.service.NovelReaderProgressPolicy
import java.io.File

data class EpubBook(
    val title: String? = null,
    val author: String? = null,
    val language: String? = null,
    val coverPath: String? = null,
    val metadata: EpubMetadata = EpubMetadata(),
    val manifest: EpubManifest = EpubManifest(),
    val spine: EpubSpine = EpubSpine(),
    val tableOfContents: List<TocEntry> = emptyList(),
    val contentDirectory: String = "",
    val zipPath: String = "",
    val extractedDir: File? = null,
) {
    val coverHref: String?
        get() = NovelEpubBookPolicy.coverHref(
            metadata = metadata,
            manifest = manifest,
            contentDirectory = contentDirectory,
        )

    val linearSpineItems: List<SpineItem>
        get() = NovelEpubBookPolicy.linearSpineItems(spine)

    fun getChapterHref(index: Int): String? {
        return NovelEpubBookPolicy.chapterHref(
            index = index,
            spine = spine,
            manifest = manifest,
            contentDirectory = contentDirectory,
        )
    }

    // Hoshi Shims
    fun title(): String? = title
    fun spine(): EpubSpine = spine
    fun chapterAbsolutePath(index: UInt): String? {
        val href = getChapterHref(index.toInt()) ?: return null
        val baseDir = extractedDir ?: return null
        return File(baseDir, href).absolutePath
    }

    /**
     * Returns the cached image URL for an image-only spine item.
     * The URL was resolved once during book parsing and stored in [SpineItem.imageUrl].
     */
    fun getImageUrl(index: Int): String? {
        return NovelEpubBookPolicy.imageUrl(
            index = index,
            spine = spine,
        )
    }

    // Cache to prevent recalculating Chapter Character length
    private val chapterLengthCache = mutableMapOf<Int, Int>()

    /**
     * Calculates the exploredCharCount of a specific chapter matching TTSU logic
     */
    fun getChapterCharacters(index: Int): Int {
        if (chapterLengthCache.containsKey(index)) return chapterLengthCache[index]!!

        val spineType = linearSpineItems.getOrNull(index)?.type
        if (spineType == SpineItemType.IMAGE_ONLY) {
            chapterLengthCache[index] = 0
            return 0
        }

        return try {
            val content = EpubParser().parseChapter(this, index) ?: return 0

            val charCount = NovelReaderCharacterCountPolicy.countChapterCharacters(content)
            chapterLengthCache[index] = charCount
            charCount
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Converts an absolute character count back into a 
     * specific chapter index and decimal progress (0.0 to 1.0) for that chapter.
     */
    fun convertCharsToProgress(totalExploredChars: Int): Pair<Int, Double> {
        val position = NovelReaderProgressPolicy.chapterProgressForCharacterCount(
            totalExploredCharacters = totalExploredChars,
            chapterCharacterCounts = linearSpineItems.indices.map { getChapterCharacters(it) },
        )
        return Pair(position.chapterIndex, position.progress)
    }
}
