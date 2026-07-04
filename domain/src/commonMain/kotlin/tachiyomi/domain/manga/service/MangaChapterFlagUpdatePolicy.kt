package tachiyomi.domain.manga.service

import tachiyomi.domain.manga.model.MangaChapterFlags
import tachiyomi.domain.manga.model.MangaUpdate

object MangaChapterFlagUpdatePolicy {
    fun downloadedFilterUpdate(
        mangaId: Long,
        currentFlags: Long,
        flag: Long,
    ): MangaUpdate {
        return chapterFlagsUpdate(
            mangaId = mangaId,
            chapterFlags = MangaChapterFlags.withDownloadedFilter(currentFlags, flag),
        )
    }

    fun unreadFilterUpdate(
        mangaId: Long,
        currentFlags: Long,
        flag: Long,
    ): MangaUpdate {
        return chapterFlagsUpdate(
            mangaId = mangaId,
            chapterFlags = MangaChapterFlags.withUnreadFilter(currentFlags, flag),
        )
    }

    fun bookmarkedFilterUpdate(
        mangaId: Long,
        currentFlags: Long,
        flag: Long,
    ): MangaUpdate {
        return chapterFlagsUpdate(
            mangaId = mangaId,
            chapterFlags = MangaChapterFlags.withBookmarkedFilter(currentFlags, flag),
        )
    }

    fun displayModeUpdate(
        mangaId: Long,
        currentFlags: Long,
        flag: Long,
    ): MangaUpdate {
        return chapterFlagsUpdate(
            mangaId = mangaId,
            chapterFlags = MangaChapterFlags.withDisplayMode(currentFlags, flag),
        )
    }

    fun sortingModeOrFlipOrderUpdate(
        mangaId: Long,
        currentFlags: Long,
        flag: Long,
    ): MangaUpdate {
        return chapterFlagsUpdate(
            mangaId = mangaId,
            chapterFlags = MangaChapterFlags.withSortingModeOrFlipOrder(currentFlags, flag),
        )
    }

    fun allFlagsUpdate(
        mangaId: Long,
        unreadFilter: Long,
        downloadedFilter: Long,
        bookmarkedFilter: Long,
        sortingMode: Long,
        sortingDirection: Long,
        displayMode: Long,
    ): MangaUpdate {
        return chapterFlagsUpdate(
            mangaId = mangaId,
            chapterFlags = MangaChapterFlags.withAll(
                unreadFilter = unreadFilter,
                downloadedFilter = downloadedFilter,
                bookmarkedFilter = bookmarkedFilter,
                sortingMode = sortingMode,
                sortingDirection = sortingDirection,
                displayMode = displayMode,
            ),
        )
    }

    private fun chapterFlagsUpdate(
        mangaId: Long,
        chapterFlags: Long,
    ): MangaUpdate {
        return MangaUpdate(
            id = mangaId,
            chapterFlags = chapterFlags,
        )
    }
}
