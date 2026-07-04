package tachiyomi.domain.library.model

object LibraryUpdateMangaRestrictions {
    const val MANGA_NON_COMPLETED = "manga_ongoing"
    const val MANGA_HAS_UNREAD = "manga_fully_read"
    const val MANGA_NON_READ = "manga_started"
    const val MANGA_OUTSIDE_RELEASE_PERIOD = "manga_outside_release_period"

    val default = setOf(
        MANGA_HAS_UNREAD,
        MANGA_NON_COMPLETED,
        MANGA_NON_READ,
        MANGA_OUTSIDE_RELEASE_PERIOD,
    )
}
