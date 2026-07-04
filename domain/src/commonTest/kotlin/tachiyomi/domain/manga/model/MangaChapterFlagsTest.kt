package tachiyomi.domain.manga.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MangaChapterFlagsTest {
    @Test
    fun filtersReplaceOnlyTheirMaskedBits() {
        var flags = MangaChapterFlags.withUnreadFilter(
            flags = MangaChapterFlags.CHAPTER_SHOW_DOWNLOADED,
            flag = MangaChapterFlags.CHAPTER_SHOW_UNREAD,
        )
        flags = MangaChapterFlags.withBookmarkedFilter(flags, MangaChapterFlags.CHAPTER_SHOW_BOOKMARKED)

        assertEquals(MangaChapterFlags.CHAPTER_SHOW_UNREAD, MangaChapterFlags.unreadFilterRaw(flags))
        assertEquals(MangaChapterFlags.CHAPTER_SHOW_DOWNLOADED, MangaChapterFlags.downloadedFilterRaw(flags))
        assertEquals(MangaChapterFlags.CHAPTER_SHOW_BOOKMARKED, MangaChapterFlags.bookmarkedFilterRaw(flags))

        flags = MangaChapterFlags.withUnreadFilter(flags, MangaChapterFlags.CHAPTER_SHOW_READ)

        assertEquals(MangaChapterFlags.CHAPTER_SHOW_READ, MangaChapterFlags.unreadFilterRaw(flags))
        assertEquals(MangaChapterFlags.CHAPTER_SHOW_DOWNLOADED, MangaChapterFlags.downloadedFilterRaw(flags))
        assertEquals(MangaChapterFlags.CHAPTER_SHOW_BOOKMARKED, MangaChapterFlags.bookmarkedFilterRaw(flags))
    }

    @Test
    fun sortingModeOrFlipOrderSwitchesNewModeToAscending() {
        val flags = MangaChapterFlags.withSortingModeOrFlipOrder(
            flags = MangaChapterFlags.CHAPTER_SORT_DESC,
            flag = MangaChapterFlags.CHAPTER_SORTING_NUMBER,
        )

        assertEquals(MangaChapterFlags.CHAPTER_SORTING_NUMBER, MangaChapterFlags.sorting(flags))
        assertFalse(MangaChapterFlags.sortDescending(flags))
    }

    @Test
    fun sortingModeOrFlipOrderFlipsExistingModeDirection() {
        val ascendingFlags = MangaChapterFlags.withAll(
            unreadFilter = MangaChapterFlags.SHOW_ALL,
            downloadedFilter = MangaChapterFlags.SHOW_ALL,
            bookmarkedFilter = MangaChapterFlags.SHOW_ALL,
            sortingMode = MangaChapterFlags.CHAPTER_SORTING_NUMBER,
            sortingDirection = MangaChapterFlags.CHAPTER_SORT_ASC,
            displayMode = MangaChapterFlags.CHAPTER_DISPLAY_NAME,
        )

        val descendingFlags = MangaChapterFlags.withSortingModeOrFlipOrder(
            flags = ascendingFlags,
            flag = MangaChapterFlags.CHAPTER_SORTING_NUMBER,
        )

        assertEquals(MangaChapterFlags.CHAPTER_SORTING_NUMBER, MangaChapterFlags.sorting(descendingFlags))
        assertTrue(MangaChapterFlags.sortDescending(descendingFlags))
    }

    @Test
    fun withAllCombinesEveryChapterFlagGroup() {
        val flags = MangaChapterFlags.withAll(
            unreadFilter = MangaChapterFlags.CHAPTER_SHOW_UNREAD,
            downloadedFilter = MangaChapterFlags.CHAPTER_SHOW_NOT_DOWNLOADED,
            bookmarkedFilter = MangaChapterFlags.CHAPTER_SHOW_BOOKMARKED,
            sortingMode = MangaChapterFlags.CHAPTER_SORTING_UPLOAD_DATE,
            sortingDirection = MangaChapterFlags.CHAPTER_SORT_ASC,
            displayMode = MangaChapterFlags.CHAPTER_DISPLAY_NUMBER,
        )

        assertEquals(MangaChapterFlags.CHAPTER_SHOW_UNREAD, MangaChapterFlags.unreadFilterRaw(flags))
        assertEquals(MangaChapterFlags.CHAPTER_SHOW_NOT_DOWNLOADED, MangaChapterFlags.downloadedFilterRaw(flags))
        assertEquals(MangaChapterFlags.CHAPTER_SHOW_BOOKMARKED, MangaChapterFlags.bookmarkedFilterRaw(flags))
        assertEquals(MangaChapterFlags.CHAPTER_SORTING_UPLOAD_DATE, MangaChapterFlags.sorting(flags))
        assertFalse(MangaChapterFlags.sortDescending(flags))
        assertEquals(MangaChapterFlags.CHAPTER_DISPLAY_NUMBER, MangaChapterFlags.displayMode(flags))
    }
}
