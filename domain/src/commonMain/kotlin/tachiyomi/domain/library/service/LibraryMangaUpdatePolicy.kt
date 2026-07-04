package tachiyomi.domain.library.service

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import tachiyomi.domain.library.model.LibraryUpdateMangaRestrictions

object LibraryMangaUpdatePolicy {
    enum class SkipReason {
        ONLY_FETCH_ONCE,
        COMPLETED,
        HAS_UNREAD,
        NOT_STARTED,
        OUTSIDE_RELEASE_PERIOD,
    }

    fun skipReason(
        updateStrategy: UpdateStrategy,
        status: Long,
        nextUpdate: Long,
        totalChapters: Long,
        unreadCount: Long,
        hasStarted: Boolean,
        restrictions: Set<String>,
        fetchWindowUpperBound: Long,
    ): SkipReason? {
        return when {
            updateStrategy == UpdateStrategy.ONLY_FETCH_ONCE && totalChapters > 0L -> {
                SkipReason.ONLY_FETCH_ONCE
            }
            LibraryUpdateMangaRestrictions.MANGA_NON_COMPLETED in restrictions &&
                status.toInt() == SManga.COMPLETED -> {
                SkipReason.COMPLETED
            }
            LibraryUpdateMangaRestrictions.MANGA_HAS_UNREAD in restrictions &&
                unreadCount != 0L -> {
                SkipReason.HAS_UNREAD
            }
            LibraryUpdateMangaRestrictions.MANGA_NON_READ in restrictions &&
                totalChapters > 0L &&
                !hasStarted -> {
                SkipReason.NOT_STARTED
            }
            LibraryUpdateMangaRestrictions.MANGA_OUTSIDE_RELEASE_PERIOD in restrictions &&
                nextUpdate > fetchWindowUpperBound -> {
                SkipReason.OUTSIDE_RELEASE_PERIOD
            }
            else -> null
        }
    }

    fun shouldUpdate(
        updateStrategy: UpdateStrategy,
        status: Long,
        nextUpdate: Long,
        totalChapters: Long,
        unreadCount: Long,
        hasStarted: Boolean,
        restrictions: Set<String>,
        fetchWindowUpperBound: Long,
    ): Boolean {
        return skipReason(
            updateStrategy = updateStrategy,
            status = status,
            nextUpdate = nextUpdate,
            totalChapters = totalChapters,
            unreadCount = unreadCount,
            hasStarted = hasStarted,
            restrictions = restrictions,
            fetchWindowUpperBound = fetchWindowUpperBound,
        ) == null
    }
}
