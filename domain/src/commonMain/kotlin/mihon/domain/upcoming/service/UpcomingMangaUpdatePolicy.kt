package mihon.domain.upcoming.service

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import tachiyomi.domain.library.model.LibraryUpdateMangaRestrictions

object UpcomingMangaUpdatePolicy {
    fun shouldUpdate(
        updateStrategy: UpdateStrategy,
        status: Long,
        nextUpdate: Long,
        totalChapters: Long,
        unreadCount: Long,
        hasStarted: Boolean,
        restrictions: Set<String>,
        today: Long,
    ): Boolean {
        return when {
            updateStrategy != UpdateStrategy.ALWAYS_UPDATE -> false
            LibraryUpdateMangaRestrictions.MANGA_NON_COMPLETED in restrictions &&
                status.toInt() == SManga.COMPLETED -> false
            LibraryUpdateMangaRestrictions.MANGA_HAS_UNREAD in restrictions &&
                unreadCount != 0L -> false
            LibraryUpdateMangaRestrictions.MANGA_NON_READ in restrictions &&
                totalChapters > 0L &&
                !hasStarted -> false
            LibraryUpdateMangaRestrictions.MANGA_OUTSIDE_RELEASE_PERIOD in restrictions &&
                nextUpdate < today -> false
            else -> true
        }
    }
}
