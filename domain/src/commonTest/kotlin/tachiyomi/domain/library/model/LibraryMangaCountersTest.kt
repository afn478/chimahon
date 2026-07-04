package tachiyomi.domain.library.model

import tachiyomi.domain.manga.model.MangaChapterFlags
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryMangaCountersTest {
    @Test
    fun unreadCountUsesTotalMinusReadByDefault() {
        val count = LibraryMangaCounters.unreadCount(
            totalChapters = 12,
            readCount = 5,
            bookmarkCount = 4,
            bookmarkReadCount = 2,
            chapterFlags = MangaChapterFlags.SHOW_ALL,
        )

        assertEquals(7, count)
    }

    @Test
    fun unreadCountUsesNotBookmarkedChaptersWhenFiltered() {
        val count = LibraryMangaCounters.unreadCount(
            totalChapters = 12,
            readCount = 5,
            bookmarkCount = 4,
            bookmarkReadCount = 2,
            chapterFlags = MangaChapterFlags.CHAPTER_SHOW_NOT_BOOKMARKED,
        )

        assertEquals(5, count)
    }

    @Test
    fun unreadCountUsesBookmarkedChaptersWhenFiltered() {
        val count = LibraryMangaCounters.unreadCount(
            totalChapters = 12,
            readCount = 5,
            bookmarkCount = 4,
            bookmarkReadCount = 2,
            chapterFlags = MangaChapterFlags.CHAPTER_SHOW_BOOKMARKED,
        )

        assertEquals(2, count)
    }

    @Test
    fun notBookmarkedFilterKeepsExistingPrecedenceIfBothBookmarkBitsAreSet() {
        val count = LibraryMangaCounters.unreadCount(
            totalChapters = 12,
            readCount = 5,
            bookmarkCount = 4,
            bookmarkReadCount = 2,
            chapterFlags = MangaChapterFlags.CHAPTER_SHOW_NOT_BOOKMARKED or
                MangaChapterFlags.CHAPTER_SHOW_BOOKMARKED,
        )

        assertEquals(5, count)
    }

    @Test
    fun bookmarkAndStartedFlagsUseCounts() {
        assertFalse(LibraryMangaCounters.hasBookmarks(0))
        assertTrue(LibraryMangaCounters.hasBookmarks(1))
        assertFalse(LibraryMangaCounters.hasStarted(0))
        assertTrue(LibraryMangaCounters.hasStarted(1))
    }
}
