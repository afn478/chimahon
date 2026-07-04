package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.manga.service.MangaChapterFlagUpdatePolicy

class SetMangaChapterFlags(
    private val mangaRepository: MangaRepository,
) {

    suspend fun awaitSetDownloadedFilter(manga: Manga, flag: Long): Boolean {
        return mangaRepository.update(
            MangaChapterFlagUpdatePolicy.downloadedFilterUpdate(
                mangaId = manga.id,
                currentFlags = manga.chapterFlags,
                flag = flag,
            ),
        )
    }

    suspend fun awaitSetUnreadFilter(manga: Manga, flag: Long): Boolean {
        return mangaRepository.update(
            MangaChapterFlagUpdatePolicy.unreadFilterUpdate(
                mangaId = manga.id,
                currentFlags = manga.chapterFlags,
                flag = flag,
            ),
        )
    }

    suspend fun awaitSetBookmarkFilter(manga: Manga, flag: Long): Boolean {
        return mangaRepository.update(
            MangaChapterFlagUpdatePolicy.bookmarkedFilterUpdate(
                mangaId = manga.id,
                currentFlags = manga.chapterFlags,
                flag = flag,
            ),
        )
    }

    suspend fun awaitSetDisplayMode(manga: Manga, flag: Long): Boolean {
        return mangaRepository.update(
            MangaChapterFlagUpdatePolicy.displayModeUpdate(
                mangaId = manga.id,
                currentFlags = manga.chapterFlags,
                flag = flag,
            ),
        )
    }

    suspend fun awaitSetSortingModeOrFlipOrder(manga: Manga, flag: Long): Boolean {
        return mangaRepository.update(
            MangaChapterFlagUpdatePolicy.sortingModeOrFlipOrderUpdate(
                mangaId = manga.id,
                currentFlags = manga.chapterFlags,
                flag = flag,
            ),
        )
    }

    suspend fun awaitSetAllFlags(
        mangaId: Long,
        unreadFilter: Long,
        downloadedFilter: Long,
        bookmarkedFilter: Long,
        sortingMode: Long,
        sortingDirection: Long,
        displayMode: Long,
    ): Boolean {
        return mangaRepository.update(
            MangaChapterFlagUpdatePolicy.allFlagsUpdate(
                mangaId = mangaId,
                unreadFilter = unreadFilter,
                downloadedFilter = downloadedFilter,
                bookmarkedFilter = bookmarkedFilter,
                sortingMode = sortingMode,
                sortingDirection = sortingDirection,
                displayMode = displayMode,
            ),
        )
    }
}
