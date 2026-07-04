package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import tachiyomi.domain.library.model.NovelBookMetadata
import tachiyomi.domain.reader.model.NovelEpubSpineItemType
import tachiyomi.domain.reader.model.NovelReaderBookmark

class NovelReaderSessionPolicyTest {
    @Test
    fun bookOpenActionPersistsLastAccessOnlyWhenRootIsAvailable() {
        val book = NovelBookMetadata(
            id = "book",
            title = "Title",
            folder = "folder",
            lastAccess = 10L,
            dateAdded = 5L,
        )

        assertEquals(
            NovelReaderSessionPolicy.BookOpenAction.PersistLastAccess(
                book.copy(lastAccess = 100L),
            ),
            NovelReaderSessionPolicy.bookOpenAction(
                book = book,
                rootAvailable = true,
                nowMillis = 100L,
            ),
        )
        assertEquals(
            NovelReaderSessionPolicy.BookOpenAction.Ignore,
            NovelReaderSessionPolicy.bookOpenAction(
                book = book,
                rootAvailable = false,
                nowMillis = 100L,
            ),
        )
    }

    @Test
    fun restoredBookmarkStateUsesBookmarkPositionAndCalculatedCharacters() {
        val state = NovelReaderSessionPolicy.restoredBookmarkState(
            bookmark = NovelReaderBookmark(
                chapterIndex = 2,
                progress = 0.5,
                characterCount = 999,
            ),
            chapterCharacterCount = listOf(100, 200, 300)::get,
        )

        assertEquals(
            NovelReaderSessionPolicy.RestoredBookmarkState(
                chapterIndex = 2,
                progress = 0.5,
                characterCount = 450,
            ),
            state,
        )
    }

    @Test
    fun restoredBookmarkStateDefaultsToStartWhenBookmarkIsMissing() {
        assertEquals(
            NovelReaderSessionPolicy.RestoredBookmarkState(
                chapterIndex = 0,
                progress = 0.0,
                characterCount = 0,
            ),
            NovelReaderSessionPolicy.restoredBookmarkState(
                bookmark = null,
                chapterCharacterCount = listOf(100, 200)::get,
            ),
        )
    }

    @Test
    fun readerSessionDefaultsRemainStable() {
        assertEquals("Unknown", NovelReaderSessionPolicy.statisticsTitle(null))
        assertEquals("Book", NovelReaderSessionPolicy.statisticsTitle("Book"))
        assertEquals(0.0001, NovelReaderSessionPolicy.BOOKMARK_PROGRESS_EPSILON)
        assertTrue(NovelReaderSessionPolicy.isImageOnlySpineItem(NovelEpubSpineItemType.IMAGE_ONLY))
        assertFalse(NovelReaderSessionPolicy.isImageOnlySpineItem(NovelEpubSpineItemType.TEXT))
        assertFalse(NovelReaderSessionPolicy.isImageOnlySpineItem(null))
    }

    @Test
    fun statisticsUpdateGateRequiresUnlockedForegroundTracking() {
        assertTrue(
            NovelReaderSessionPolicy.shouldUpdateStatistics(
                updateTracker = true,
                trackingLocked = false,
                appBackgrounded = false,
            ),
        )
        assertFalse(
            NovelReaderSessionPolicy.shouldUpdateStatistics(
                updateTracker = false,
                trackingLocked = false,
                appBackgrounded = false,
            ),
        )
        assertFalse(
            NovelReaderSessionPolicy.shouldUpdateStatistics(
                updateTracker = true,
                trackingLocked = true,
                appBackgrounded = false,
            ),
        )
        assertFalse(
            NovelReaderSessionPolicy.shouldUpdateStatistics(
                updateTracker = true,
                trackingLocked = false,
                appBackgrounded = true,
            ),
        )
    }

    @Test
    fun trackingLockTransitionUpdatesBeforeLockAndResetsAfterUnlock() {
        assertEquals(
            NovelReaderSessionPolicy.TrackingLockTransition(
                updateBeforeLock = true,
                resetBaselineAfterUnlock = false,
                locked = true,
            ),
            NovelReaderSessionPolicy.trackingLockTransition(
                locked = true,
                currentlyTracking = true,
            ),
        )
        assertEquals(
            NovelReaderSessionPolicy.TrackingLockTransition(
                updateBeforeLock = false,
                resetBaselineAfterUnlock = false,
                locked = true,
            ),
            NovelReaderSessionPolicy.trackingLockTransition(
                locked = true,
                currentlyTracking = false,
            ),
        )
        assertEquals(
            NovelReaderSessionPolicy.TrackingLockTransition(
                updateBeforeLock = false,
                resetBaselineAfterUnlock = true,
                locked = false,
            ),
            NovelReaderSessionPolicy.trackingLockTransition(
                locked = false,
                currentlyTracking = true,
            ),
        )
    }

    @Test
    fun chapterChangeStateCarriesPositionAndStatisticsGate() {
        assertEquals(
            NovelReaderSessionPolicy.ChapterChangeState(
                index = 3,
                progress = 0.25,
                baselineCharacterCount = 400,
                updateStatisticsBeforeChange = true,
            ),
            NovelReaderSessionPolicy.chapterChangeState(
                newIndex = 3,
                progress = 0.25,
                baselineCharacterCount = 400,
                trackingLocked = false,
                appBackgrounded = false,
            ),
        )
        assertFalse(
            NovelReaderSessionPolicy.chapterChangeState(
                newIndex = 3,
                progress = 0.25,
                baselineCharacterCount = 400,
                trackingLocked = true,
                appBackgrounded = false,
            ).updateStatisticsBeforeChange,
        )
    }
}
