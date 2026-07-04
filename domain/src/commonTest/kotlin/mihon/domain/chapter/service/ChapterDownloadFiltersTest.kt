package mihon.domain.chapter.service

import tachiyomi.domain.chapter.model.Chapter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChapterDownloadFiltersTest {
    @Test
    fun shouldDownloadNewChaptersRejectsNonFavoriteManga() {
        assertFalse(
            ChapterDownloadFilters.shouldDownloadNewChapters(
                favorite = false,
                categoryIds = listOf(1),
                includedCategoryIds = emptySet(),
                excludedCategoryIds = emptySet(),
            ),
        )
    }

    @Test
    fun shouldDownloadNewChaptersAllowsAllCategoriesWhenNoCategoryPreferencesAreSet() {
        assertTrue(
            ChapterDownloadFilters.shouldDownloadNewChapters(
                favorite = true,
                categoryIds = listOf(1),
                includedCategoryIds = emptySet(),
                excludedCategoryIds = emptySet(),
            ),
        )
    }

    @Test
    fun shouldDownloadNewChaptersUsesDefaultCategoryForUncategorizedManga() {
        assertTrue(
            ChapterDownloadFilters.shouldDownloadNewChapters(
                favorite = true,
                categoryIds = emptyList(),
                includedCategoryIds = setOf(ChapterDownloadFilters.DEFAULT_CATEGORY_ID),
                excludedCategoryIds = emptySet(),
            ),
        )
    }

    @Test
    fun shouldDownloadNewChaptersLetsExcludedCategoriesOverrideIncludedCategories() {
        assertFalse(
            ChapterDownloadFilters.shouldDownloadNewChapters(
                favorite = true,
                categoryIds = listOf(1, 2),
                includedCategoryIds = setOf(1),
                excludedCategoryIds = setOf(2),
            ),
        )
    }

    @Test
    fun shouldDownloadNewChaptersAllowsNonExcludedCategoriesWhenIncludeListIsEmpty() {
        assertTrue(
            ChapterDownloadFilters.shouldDownloadNewChapters(
                favorite = true,
                categoryIds = listOf(1),
                includedCategoryIds = emptySet(),
                excludedCategoryIds = setOf(2),
            ),
        )
    }

    @Test
    fun shouldDownloadNewChaptersRequiresIncludedCategoryWhenIncludeListIsSet() {
        assertFalse(
            ChapterDownloadFilters.shouldDownloadNewChapters(
                favorite = true,
                categoryIds = listOf(1),
                includedCategoryIds = setOf(2),
                excludedCategoryIds = emptySet(),
            ),
        )
        assertTrue(
            ChapterDownloadFilters.shouldDownloadNewChapters(
                favorite = true,
                categoryIds = listOf(1, 2),
                includedCategoryIds = setOf(2),
                excludedCategoryIds = emptySet(),
            ),
        )
    }

    @Test
    fun withoutAlreadyReadChapterNumbersRemovesNewChaptersWithReadExistingNumbers() {
        val newChapterOne = chapter(number = 1.0)
        val newChapterTwo = chapter(number = 2.0)
        val newChapterThree = chapter(number = 3.0)
        val existingChapters = listOf(
            chapter(number = 1.0, read = true),
            chapter(number = 2.0, read = false),
            chapter(number = 3.0, read = true, recognized = false),
        )

        val filtered = ChapterDownloadFilters.withoutAlreadyReadChapterNumbers(
            newChapters = listOf(newChapterOne, newChapterTwo, newChapterThree),
            existingChapters = existingChapters,
        )

        assertEquals(listOf(newChapterTwo, newChapterThree), filtered)
    }

    private fun chapter(
        number: Double,
        read: Boolean = false,
        recognized: Boolean = true,
    ): Chapter {
        return Chapter.create().copy(
            read = read,
            chapterNumber = if (recognized) number else -1.0,
        )
    }
}
