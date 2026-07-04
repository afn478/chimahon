package mihon.domain.upcoming.service

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import mihon.domain.upcoming.model.UpcomingMangaCandidate
import tachiyomi.domain.library.model.LibraryUpdateMangaRestrictions
import kotlin.test.Test
import kotlin.test.assertEquals

class UpcomingMangaSelectionPolicyTest {
    @Test
    fun selectMangaIdsAppliesCategoryFiltersAndSortsByNextUpdate() {
        val selectedIds = UpcomingMangaSelectionPolicy.selectMangaIdsForUpdate(
            candidates = listOf(
                candidate(mangaId = 1, categoryIds = listOf(INCLUDED_CATEGORY), nextUpdate = TODAY + 300),
                candidate(mangaId = 2, categoryIds = listOf(EXCLUDED_CATEGORY), nextUpdate = TODAY + 100),
                candidate(mangaId = 3, categoryIds = listOf(INCLUDED_CATEGORY), nextUpdate = TODAY + 200),
                candidate(mangaId = 4, categoryIds = listOf(OTHER_CATEGORY), nextUpdate = TODAY + 50),
            ),
            includedCategoryIds = setOf(INCLUDED_CATEGORY),
            excludedCategoryIds = setOf(EXCLUDED_CATEGORY),
            restrictions = emptySet(),
            today = TODAY,
        )

        assertEquals(listOf(3L, 1L), selectedIds)
    }

    @Test
    fun categoryFilteringRunsBeforeDuplicateRemoval() {
        val selectedIds = UpcomingMangaSelectionPolicy.selectMangaIdsForUpdate(
            candidates = listOf(
                candidate(mangaId = 1, categoryIds = listOf(EXCLUDED_CATEGORY), nextUpdate = TODAY + 100),
                candidate(mangaId = 1, categoryIds = listOf(INCLUDED_CATEGORY), nextUpdate = TODAY + 200),
            ),
            includedCategoryIds = setOf(INCLUDED_CATEGORY),
            excludedCategoryIds = setOf(EXCLUDED_CATEGORY),
            restrictions = emptySet(),
            today = TODAY,
        )

        assertEquals(listOf(1L), selectedIds)
    }

    @Test
    fun duplicateRemovalRunsBeforeUpdateRestrictions() {
        val selectedIds = UpcomingMangaSelectionPolicy.selectMangaIdsForUpdate(
            candidates = listOf(
                candidate(mangaId = 1, updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE),
                candidate(mangaId = 1, updateStrategy = UpdateStrategy.ALWAYS_UPDATE),
            ),
            includedCategoryIds = emptySet(),
            excludedCategoryIds = emptySet(),
            restrictions = emptySet(),
            today = TODAY,
        )

        assertEquals(emptyList(), selectedIds)
    }

    @Test
    fun selectMangaIdsAppliesUpdateRestrictions() {
        val selectedIds = UpcomingMangaSelectionPolicy.selectMangaIdsForUpdate(
            candidates = listOf(
                candidate(mangaId = 1, nextUpdate = TODAY - 1),
                candidate(mangaId = 2, nextUpdate = TODAY),
            ),
            includedCategoryIds = emptySet(),
            excludedCategoryIds = emptySet(),
            restrictions = setOf(LibraryUpdateMangaRestrictions.MANGA_OUTSIDE_RELEASE_PERIOD),
            today = TODAY,
        )

        assertEquals(listOf(2L), selectedIds)
    }

    private fun candidate(
        mangaId: Long,
        categoryIds: List<Long> = emptyList(),
        updateStrategy: UpdateStrategy = UpdateStrategy.ALWAYS_UPDATE,
        status: Long = SManga.ONGOING.toLong(),
        nextUpdate: Long = TODAY,
        totalChapters: Long = 1L,
        unreadCount: Long = 0L,
        hasStarted: Boolean = true,
    ): UpcomingMangaCandidate {
        return UpcomingMangaCandidate(
            mangaId = mangaId,
            categoryIds = categoryIds,
            updateStrategy = updateStrategy,
            status = status,
            nextUpdate = nextUpdate,
            totalChapters = totalChapters,
            unreadCount = unreadCount,
            hasStarted = hasStarted,
        )
    }

    private companion object {
        const val TODAY = 1_000L
        const val INCLUDED_CATEGORY = 10L
        const val EXCLUDED_CATEGORY = 20L
        const val OTHER_CATEGORY = 30L
    }
}
