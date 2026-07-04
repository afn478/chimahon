package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaChapterFlags
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository

class SetMangaChapterFlags(
    private val mangaRepository: MangaRepository,
) {

    suspend fun awaitSetDownloadedFilter(manga: Manga, flag: Long): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                chapterFlags = MangaChapterFlags.withDownloadedFilter(manga.chapterFlags, flag),
            ),
        )
    }

    suspend fun awaitSetUnreadFilter(manga: Manga, flag: Long): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                chapterFlags = MangaChapterFlags.withUnreadFilter(manga.chapterFlags, flag),
            ),
        )
    }

    suspend fun awaitSetBookmarkFilter(manga: Manga, flag: Long): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                chapterFlags = MangaChapterFlags.withBookmarkedFilter(manga.chapterFlags, flag),
            ),
        )
    }

    suspend fun awaitSetDisplayMode(manga: Manga, flag: Long): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                chapterFlags = MangaChapterFlags.withDisplayMode(manga.chapterFlags, flag),
            ),
        )
    }

    suspend fun awaitSetSortingModeOrFlipOrder(manga: Manga, flag: Long): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                chapterFlags = MangaChapterFlags.withSortingModeOrFlipOrder(manga.chapterFlags, flag),
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
            MangaUpdate(
                id = mangaId,
                chapterFlags = MangaChapterFlags.withAll(
                    unreadFilter = unreadFilter,
                    downloadedFilter = downloadedFilter,
                    bookmarkedFilter = bookmarkedFilter,
                    sortingMode = sortingMode,
                    sortingDirection = sortingDirection,
                    displayMode = displayMode,
                ),
            ),
        )
    }
}
