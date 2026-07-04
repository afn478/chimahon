package tachiyomi.domain.manga.model

object MangaViewerFlags {
    const val READING_MODE_MASK = 0x00000007L
    const val ORIENTATION_MASK = 0x00000038L

    fun readingMode(flags: Long): Long {
        return flags and READING_MODE_MASK
    }

    fun orientation(flags: Long): Long {
        return flags and ORIENTATION_MASK
    }

    fun withReadingMode(flags: Long, flag: Long): Long {
        return flags.setFlag(flag, READING_MODE_MASK)
    }

    fun withOrientation(flags: Long, flag: Long): Long {
        return flags.setFlag(flag, ORIENTATION_MASK)
    }

    private fun Long.setFlag(flag: Long, mask: Long): Long {
        return this and mask.inv() or (flag and mask)
    }
}
