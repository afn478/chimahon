package mihon.domain.upcoming.service

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import tachiyomi.domain.library.model.LibraryUpdateMangaRestrictions
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpcomingMangaUpdatePolicyTest {
    @Test
    fun shouldUpdateAllowsMangaWithoutRestrictions() {
        assertTrue(upcomingShouldUpdate(restrictions = emptySet()))
    }

    @Test
    fun shouldUpdateRejectsMangaThatDoesNotAlwaysUpdate() {
        assertFalse(upcomingShouldUpdate(updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE))
    }

    @Test
    fun shouldUpdateRejectsCompletedMangaWhenRestricted() {
        assertFalse(
            upcomingShouldUpdate(
                status = SManga.COMPLETED.toLong(),
                restrictions = setOf(LibraryUpdateMangaRestrictions.MANGA_NON_COMPLETED),
            ),
        )
    }

    @Test
    fun shouldUpdateRejectsMangaWithUnreadChaptersWhenRestricted() {
        assertFalse(
            upcomingShouldUpdate(
                unreadCount = 1,
                restrictions = setOf(LibraryUpdateMangaRestrictions.MANGA_HAS_UNREAD),
            ),
        )
    }

    @Test
    fun shouldUpdateRejectsUnstartedMangaWithChaptersWhenRestricted() {
        assertFalse(
            upcomingShouldUpdate(
                totalChapters = 1,
                hasStarted = false,
                restrictions = setOf(LibraryUpdateMangaRestrictions.MANGA_NON_READ),
            ),
        )
        assertTrue(
            upcomingShouldUpdate(
                totalChapters = 0,
                hasStarted = false,
                restrictions = setOf(LibraryUpdateMangaRestrictions.MANGA_NON_READ),
            ),
        )
    }

    @Test
    fun shouldUpdateRejectsMangaBeforeReleasePeriodWhenRestricted() {
        assertFalse(
            upcomingShouldUpdate(
                nextUpdate = TODAY - 1,
                restrictions = setOf(LibraryUpdateMangaRestrictions.MANGA_OUTSIDE_RELEASE_PERIOD),
            ),
        )
        assertTrue(
            upcomingShouldUpdate(
                nextUpdate = TODAY,
                restrictions = setOf(LibraryUpdateMangaRestrictions.MANGA_OUTSIDE_RELEASE_PERIOD),
            ),
        )
    }

    private fun upcomingShouldUpdate(
        updateStrategy: UpdateStrategy = UpdateStrategy.ALWAYS_UPDATE,
        status: Long = SManga.ONGOING.toLong(),
        nextUpdate: Long = TODAY,
        totalChapters: Long = 1,
        unreadCount: Long = 0,
        hasStarted: Boolean = true,
        restrictions: Set<String> = LibraryUpdateMangaRestrictions.default,
    ): Boolean {
        return UpcomingMangaUpdatePolicy.shouldUpdate(
            updateStrategy = updateStrategy,
            status = status,
            nextUpdate = nextUpdate,
            totalChapters = totalChapters,
            unreadCount = unreadCount,
            hasStarted = hasStarted,
            restrictions = restrictions,
            today = TODAY,
        )
    }

    private companion object {
        const val TODAY = 1_000L
    }
}
