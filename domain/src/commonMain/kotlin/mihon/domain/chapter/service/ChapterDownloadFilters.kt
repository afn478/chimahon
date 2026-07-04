package mihon.domain.chapter.service

import tachiyomi.domain.chapter.model.Chapter

object ChapterDownloadFilters {
    const val DEFAULT_CATEGORY_ID = 0L

    fun shouldDownloadNewChapters(
        favorite: Boolean,
        categoryIds: Collection<Long>,
        includedCategoryIds: Collection<Long>,
        excludedCategoryIds: Collection<Long>,
    ): Boolean {
        if (!favorite) return false

        val effectiveCategoryIds = categoryIds.ifEmpty { listOf(DEFAULT_CATEGORY_ID) }

        return when {
            includedCategoryIds.isEmpty() && excludedCategoryIds.isEmpty() -> true
            effectiveCategoryIds.any { it in excludedCategoryIds } -> false
            includedCategoryIds.isEmpty() -> true
            else -> effectiveCategoryIds.any { it in includedCategoryIds }
        }
    }

    fun withoutAlreadyReadChapterNumbers(
        newChapters: List<Chapter>,
        existingChapters: List<Chapter>,
    ): List<Chapter> {
        val readChapterNumbers = existingChapters
            .asSequence()
            .filter { it.read && it.isRecognizedNumber }
            .map { it.chapterNumber }
            .toSet()

        return newChapters.filterNot { it.chapterNumber in readChapterNumbers }
    }
}
