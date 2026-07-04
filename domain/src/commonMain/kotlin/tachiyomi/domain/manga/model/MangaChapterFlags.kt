package tachiyomi.domain.manga.model

object MangaChapterFlags {
    // Generic filter that does not filter anything
    const val SHOW_ALL = 0x00000000L

    const val CHAPTER_SORT_DESC = 0x00000000L
    const val CHAPTER_SORT_ASC = 0x00000001L
    const val CHAPTER_SORT_DIR_MASK = 0x00000001L

    const val CHAPTER_SHOW_UNREAD = 0x00000002L
    const val CHAPTER_SHOW_READ = 0x00000004L
    const val CHAPTER_UNREAD_MASK = 0x00000006L

    const val CHAPTER_SHOW_DOWNLOADED = 0x00000008L
    const val CHAPTER_SHOW_NOT_DOWNLOADED = 0x00000010L
    const val CHAPTER_DOWNLOADED_MASK = 0x00000018L

    const val CHAPTER_SHOW_BOOKMARKED = 0x00000020L
    const val CHAPTER_SHOW_NOT_BOOKMARKED = 0x00000040L
    const val CHAPTER_BOOKMARKED_MASK = 0x00000060L

    const val CHAPTER_SORTING_SOURCE = 0x00000000L
    const val CHAPTER_SORTING_NUMBER = 0x00000100L
    const val CHAPTER_SORTING_UPLOAD_DATE = 0x00000200L
    const val CHAPTER_SORTING_ALPHABET = 0x00000300L
    const val CHAPTER_SORTING_MASK = 0x00000300L

    const val CHAPTER_DISPLAY_NAME = 0x00000000L
    const val CHAPTER_DISPLAY_NUMBER = 0x00100000L
    const val CHAPTER_DISPLAY_MASK = 0x00100000L

    fun sorting(flags: Long): Long {
        return flags and CHAPTER_SORTING_MASK
    }

    fun displayMode(flags: Long): Long {
        return flags and CHAPTER_DISPLAY_MASK
    }

    fun unreadFilterRaw(flags: Long): Long {
        return flags and CHAPTER_UNREAD_MASK
    }

    fun downloadedFilterRaw(flags: Long): Long {
        return flags and CHAPTER_DOWNLOADED_MASK
    }

    fun bookmarkedFilterRaw(flags: Long): Long {
        return flags and CHAPTER_BOOKMARKED_MASK
    }

    fun sortDescending(flags: Long): Boolean {
        return flags and CHAPTER_SORT_DIR_MASK == CHAPTER_SORT_DESC
    }

    fun withDownloadedFilter(flags: Long, flag: Long): Long {
        return flags.setFlag(flag, CHAPTER_DOWNLOADED_MASK)
    }

    fun withUnreadFilter(flags: Long, flag: Long): Long {
        return flags.setFlag(flag, CHAPTER_UNREAD_MASK)
    }

    fun withBookmarkedFilter(flags: Long, flag: Long): Long {
        return flags.setFlag(flag, CHAPTER_BOOKMARKED_MASK)
    }

    fun withDisplayMode(flags: Long, flag: Long): Long {
        return flags.setFlag(flag, CHAPTER_DISPLAY_MASK)
    }

    fun withSortingModeOrFlipOrder(flags: Long, flag: Long): Long {
        return if (sorting(flags) == flag) {
            val orderFlag = if (sortDescending(flags)) {
                CHAPTER_SORT_ASC
            } else {
                CHAPTER_SORT_DESC
            }
            flags.setFlag(orderFlag, CHAPTER_SORT_DIR_MASK)
        } else {
            flags
                .setFlag(flag, CHAPTER_SORTING_MASK)
                .setFlag(CHAPTER_SORT_ASC, CHAPTER_SORT_DIR_MASK)
        }
    }

    fun withAll(
        unreadFilter: Long,
        downloadedFilter: Long,
        bookmarkedFilter: Long,
        sortingMode: Long,
        sortingDirection: Long,
        displayMode: Long,
    ): Long {
        return 0L.setFlag(unreadFilter, CHAPTER_UNREAD_MASK)
            .setFlag(downloadedFilter, CHAPTER_DOWNLOADED_MASK)
            .setFlag(bookmarkedFilter, CHAPTER_BOOKMARKED_MASK)
            .setFlag(sortingMode, CHAPTER_SORTING_MASK)
            .setFlag(sortingDirection, CHAPTER_SORT_DIR_MASK)
            .setFlag(displayMode, CHAPTER_DISPLAY_MASK)
    }

    private fun Long.setFlag(flag: Long, mask: Long): Long {
        return this and mask.inv() or (flag and mask)
    }
}
