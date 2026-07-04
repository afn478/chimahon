package tachiyomi.domain.manga.service

import tachiyomi.domain.manga.model.MangaChapterFlags
import tachiyomi.domain.manga.model.MangaUpdate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MangaChapterFlagUpdatePolicyTest {
    @Test
    fun unreadFilterUpdateOnlyChangesChapterFlags() {
        val update = MangaChapterFlagUpdatePolicy.unreadFilterUpdate(
            mangaId = MANGA_ID,
            currentFlags = MangaChapterFlags.CHAPTER_SHOW_DOWNLOADED,
            flag = MangaChapterFlags.CHAPTER_SHOW_READ,
        )
        val chapterFlags = update.chapterFlags ?: error("Chapter flags must be set")

        assertEquals(MangaUpdate(id = MANGA_ID, chapterFlags = chapterFlags), update)
        assertEquals(
            MangaChapterFlags.CHAPTER_SHOW_READ,
            MangaChapterFlags.unreadFilterRaw(chapterFlags),
        )
        assertEquals(
            MangaChapterFlags.CHAPTER_SHOW_DOWNLOADED,
            MangaChapterFlags.downloadedFilterRaw(chapterFlags),
        )
    }

    @Test
    fun downloadedFilterUpdateOnlyChangesDownloadedFilter() {
        val update = MangaChapterFlagUpdatePolicy.downloadedFilterUpdate(
            mangaId = MANGA_ID,
            currentFlags = MangaChapterFlags.CHAPTER_SHOW_READ,
            flag = MangaChapterFlags.CHAPTER_SHOW_NOT_DOWNLOADED,
        )
        val chapterFlags = update.chapterFlags ?: error("Chapter flags must be set")

        assertEquals(MangaUpdate(id = MANGA_ID, chapterFlags = chapterFlags), update)
        assertEquals(
            MangaChapterFlags.CHAPTER_SHOW_NOT_DOWNLOADED,
            MangaChapterFlags.downloadedFilterRaw(chapterFlags),
        )
        assertEquals(MangaChapterFlags.CHAPTER_SHOW_READ, MangaChapterFlags.unreadFilterRaw(chapterFlags))
    }

    @Test
    fun bookmarkedFilterUpdateOnlyChangesBookmarkedFilter() {
        val update = MangaChapterFlagUpdatePolicy.bookmarkedFilterUpdate(
            mangaId = MANGA_ID,
            currentFlags = MangaChapterFlags.CHAPTER_SHOW_DOWNLOADED,
            flag = MangaChapterFlags.CHAPTER_SHOW_BOOKMARKED,
        )
        val chapterFlags = update.chapterFlags ?: error("Chapter flags must be set")

        assertEquals(MangaUpdate(id = MANGA_ID, chapterFlags = chapterFlags), update)
        assertEquals(
            MangaChapterFlags.CHAPTER_SHOW_BOOKMARKED,
            MangaChapterFlags.bookmarkedFilterRaw(chapterFlags),
        )
        assertEquals(
            MangaChapterFlags.CHAPTER_SHOW_DOWNLOADED,
            MangaChapterFlags.downloadedFilterRaw(chapterFlags),
        )
    }

    @Test
    fun displayModeUpdateOnlyChangesDisplayMode() {
        val update = MangaChapterFlagUpdatePolicy.displayModeUpdate(
            mangaId = MANGA_ID,
            currentFlags = MangaChapterFlags.CHAPTER_SORTING_NUMBER,
            flag = MangaChapterFlags.CHAPTER_DISPLAY_NUMBER,
        )
        val chapterFlags = update.chapterFlags ?: error("Chapter flags must be set")

        assertEquals(MangaUpdate(id = MANGA_ID, chapterFlags = chapterFlags), update)
        assertEquals(MangaChapterFlags.CHAPTER_DISPLAY_NUMBER, MangaChapterFlags.displayMode(chapterFlags))
        assertEquals(MangaChapterFlags.CHAPTER_SORTING_NUMBER, MangaChapterFlags.sorting(chapterFlags))
    }

    @Test
    fun sortingModeUpdateFlipsExistingSortDirection() {
        val currentFlags = MangaChapterFlags.withAll(
            unreadFilter = MangaChapterFlags.SHOW_ALL,
            downloadedFilter = MangaChapterFlags.SHOW_ALL,
            bookmarkedFilter = MangaChapterFlags.SHOW_ALL,
            sortingMode = MangaChapterFlags.CHAPTER_SORTING_NUMBER,
            sortingDirection = MangaChapterFlags.CHAPTER_SORT_ASC,
            displayMode = MangaChapterFlags.CHAPTER_DISPLAY_NAME,
        )
        val update = MangaChapterFlagUpdatePolicy.sortingModeOrFlipOrderUpdate(
            mangaId = MANGA_ID,
            currentFlags = currentFlags,
            flag = MangaChapterFlags.CHAPTER_SORTING_NUMBER,
        )
        val chapterFlags = update.chapterFlags ?: error("Chapter flags must be set")

        assertEquals(MangaUpdate(id = MANGA_ID, chapterFlags = chapterFlags), update)
        assertEquals(MangaChapterFlags.CHAPTER_SORTING_NUMBER, MangaChapterFlags.sorting(chapterFlags))
        assertTrue(MangaChapterFlags.sortDescending(chapterFlags))
    }

    @Test
    fun allFlagsUpdateCombinesEveryPreferenceGroup() {
        val update = MangaChapterFlagUpdatePolicy.allFlagsUpdate(
            mangaId = MANGA_ID,
            unreadFilter = MangaChapterFlags.CHAPTER_SHOW_UNREAD,
            downloadedFilter = MangaChapterFlags.CHAPTER_SHOW_NOT_DOWNLOADED,
            bookmarkedFilter = MangaChapterFlags.CHAPTER_SHOW_BOOKMARKED,
            sortingMode = MangaChapterFlags.CHAPTER_SORTING_UPLOAD_DATE,
            sortingDirection = MangaChapterFlags.CHAPTER_SORT_ASC,
            displayMode = MangaChapterFlags.CHAPTER_DISPLAY_NUMBER,
        )
        val chapterFlags = update.chapterFlags ?: error("Chapter flags must be set")

        assertEquals(MangaUpdate(id = MANGA_ID, chapterFlags = chapterFlags), update)
        assertEquals(MangaChapterFlags.CHAPTER_SHOW_UNREAD, MangaChapterFlags.unreadFilterRaw(chapterFlags))
        assertEquals(
            MangaChapterFlags.CHAPTER_SHOW_NOT_DOWNLOADED,
            MangaChapterFlags.downloadedFilterRaw(chapterFlags),
        )
        assertEquals(
            MangaChapterFlags.CHAPTER_SHOW_BOOKMARKED,
            MangaChapterFlags.bookmarkedFilterRaw(chapterFlags),
        )
        assertEquals(MangaChapterFlags.CHAPTER_SORTING_UPLOAD_DATE, MangaChapterFlags.sorting(chapterFlags))
        assertFalse(MangaChapterFlags.sortDescending(chapterFlags))
        assertEquals(MangaChapterFlags.CHAPTER_DISPLAY_NUMBER, MangaChapterFlags.displayMode(chapterFlags))
    }

    private companion object {
        const val MANGA_ID = 42L
    }
}
