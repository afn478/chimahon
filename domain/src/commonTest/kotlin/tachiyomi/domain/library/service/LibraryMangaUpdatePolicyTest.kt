package tachiyomi.domain.library.service

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import tachiyomi.domain.library.model.LibraryUpdateMangaRestrictions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryMangaUpdatePolicyTest {
    @Test
    fun skipReasonAllowsMangaWithoutRestrictions() {
        assertNull(librarySkipReason(restrictions = emptySet()))
        assertTrue(libraryShouldUpdate(restrictions = emptySet()))
    }

    @Test
    fun skipReasonRejectsOnlyFetchOnceMangaThatAlreadyHasChapters() {
        assertEquals(
            LibraryMangaUpdatePolicy.SkipReason.ONLY_FETCH_ONCE,
            librarySkipReason(updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE, totalChapters = 1),
        )
        assertNull(librarySkipReason(updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE, totalChapters = 0))
    }

    @Test
    fun skipReasonRejectsCompletedMangaWhenRestricted() {
        assertEquals(
            LibraryMangaUpdatePolicy.SkipReason.COMPLETED,
            librarySkipReason(
                status = SManga.COMPLETED.toLong(),
                restrictions = setOf(LibraryUpdateMangaRestrictions.MANGA_NON_COMPLETED),
            ),
        )
    }

    @Test
    fun skipReasonRejectsMangaWithUnreadChaptersWhenRestricted() {
        assertEquals(
            LibraryMangaUpdatePolicy.SkipReason.HAS_UNREAD,
            librarySkipReason(
                unreadCount = 1,
                restrictions = setOf(LibraryUpdateMangaRestrictions.MANGA_HAS_UNREAD),
            ),
        )
    }

    @Test
    fun skipReasonRejectsUnstartedMangaWithChaptersWhenRestricted() {
        assertEquals(
            LibraryMangaUpdatePolicy.SkipReason.NOT_STARTED,
            librarySkipReason(
                totalChapters = 1,
                hasStarted = false,
                restrictions = setOf(LibraryUpdateMangaRestrictions.MANGA_NON_READ),
            ),
        )
        assertNull(
            librarySkipReason(
                totalChapters = 0,
                hasStarted = false,
                restrictions = setOf(LibraryUpdateMangaRestrictions.MANGA_NON_READ),
            ),
        )
    }

    @Test
    fun skipReasonRejectsMangaAfterFetchWindowWhenRestricted() {
        assertEquals(
            LibraryMangaUpdatePolicy.SkipReason.OUTSIDE_RELEASE_PERIOD,
            librarySkipReason(
                nextUpdate = FETCH_WINDOW_UPPER_BOUND + 1,
                restrictions = setOf(LibraryUpdateMangaRestrictions.MANGA_OUTSIDE_RELEASE_PERIOD),
            ),
        )
        assertNull(
            librarySkipReason(
                nextUpdate = FETCH_WINDOW_UPPER_BOUND,
                restrictions = setOf(LibraryUpdateMangaRestrictions.MANGA_OUTSIDE_RELEASE_PERIOD),
            ),
        )
    }

    @Test
    fun shouldUpdateReturnsFalseWhenSkipReasonExists() {
        assertFalse(
            libraryShouldUpdate(
                unreadCount = 1,
                restrictions = setOf(LibraryUpdateMangaRestrictions.MANGA_HAS_UNREAD),
            ),
        )
    }

    private fun librarySkipReason(
        updateStrategy: UpdateStrategy = UpdateStrategy.ALWAYS_UPDATE,
        status: Long = SManga.ONGOING.toLong(),
        nextUpdate: Long = FETCH_WINDOW_UPPER_BOUND,
        totalChapters: Long = 1,
        unreadCount: Long = 0,
        hasStarted: Boolean = true,
        restrictions: Set<String> = LibraryUpdateMangaRestrictions.default,
    ): LibraryMangaUpdatePolicy.SkipReason? {
        return LibraryMangaUpdatePolicy.skipReason(
            updateStrategy = updateStrategy,
            status = status,
            nextUpdate = nextUpdate,
            totalChapters = totalChapters,
            unreadCount = unreadCount,
            hasStarted = hasStarted,
            restrictions = restrictions,
            fetchWindowUpperBound = FETCH_WINDOW_UPPER_BOUND,
        )
    }

    private fun libraryShouldUpdate(
        updateStrategy: UpdateStrategy = UpdateStrategy.ALWAYS_UPDATE,
        status: Long = SManga.ONGOING.toLong(),
        nextUpdate: Long = FETCH_WINDOW_UPPER_BOUND,
        totalChapters: Long = 1,
        unreadCount: Long = 0,
        hasStarted: Boolean = true,
        restrictions: Set<String> = LibraryUpdateMangaRestrictions.default,
    ): Boolean {
        return LibraryMangaUpdatePolicy.shouldUpdate(
            updateStrategy = updateStrategy,
            status = status,
            nextUpdate = nextUpdate,
            totalChapters = totalChapters,
            unreadCount = unreadCount,
            hasStarted = hasStarted,
            restrictions = restrictions,
            fetchWindowUpperBound = FETCH_WINDOW_UPPER_BOUND,
        )
    }

    private companion object {
        const val FETCH_WINDOW_UPPER_BOUND = 1_000L
    }
}
