package tachiyomi.domain.library.model

import tachiyomi.domain.manga.model.Manga

data class LibraryManga(
    val manga: Manga,
    val categories: List<Long>,
    val totalChapters: Long,
    val readCount: Long,
    val bookmarkCount: Long,
    // KMK -->
    val bookmarkReadCount: Long,
    val chapterFlags: Long,
    // KMK <--
    val latestUpload: Long,
    val chapterFetchedAt: Long,
    val lastRead: Long,
) {
    val id: Long = manga.id

    val unreadCount
        get() = LibraryMangaCounters.unreadCount(
            totalChapters = totalChapters,
            readCount = readCount,
            bookmarkCount = bookmarkCount,
            bookmarkReadCount = bookmarkReadCount,
            chapterFlags = chapterFlags,
        )

    val hasBookmarks
        get() = LibraryMangaCounters.hasBookmarks(bookmarkCount)

    val hasStarted = LibraryMangaCounters.hasStarted(readCount)
}
