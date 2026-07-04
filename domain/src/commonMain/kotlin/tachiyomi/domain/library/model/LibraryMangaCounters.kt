package tachiyomi.domain.library.model

import tachiyomi.domain.manga.model.MangaChapterFlags

object LibraryMangaCounters {
    fun unreadCount(
        totalChapters: Long,
        readCount: Long,
        bookmarkCount: Long,
        bookmarkReadCount: Long,
        chapterFlags: Long,
    ): Long {
        return when {
            chapterFlags and MangaChapterFlags.CHAPTER_SHOW_NOT_BOOKMARKED != 0L -> {
                totalChapters - bookmarkCount - (readCount - bookmarkReadCount)
            }
            chapterFlags and MangaChapterFlags.CHAPTER_SHOW_BOOKMARKED != 0L -> {
                bookmarkCount - bookmarkReadCount
            }
            else -> totalChapters - readCount
        }
    }

    fun hasBookmarks(bookmarkCount: Long): Boolean {
        return bookmarkCount > 0
    }

    fun hasStarted(readCount: Long): Boolean {
        return readCount > 0
    }
}
