package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import tachiyomi.domain.library.model.NovelBookMetadata

class NovelReaderScreenPolicyTest {
    @Test
    fun messageStatesExposeStableReaderLoadText() {
        assertEquals(
            NovelReaderScreenPolicy.MessageState(
                text = "Opening...",
                loading = true,
            ),
            NovelReaderScreenPolicy.loadingMessageState(),
        )
        assertEquals(
            NovelReaderScreenPolicy.MessageState(
                text = "No spine",
                loading = false,
            ),
            NovelReaderScreenPolicy.errorMessageState("No spine"),
        )
        assertEquals(
            NovelReaderScreenPolicy.MessageState(
                text = "Could not open book",
                loading = false,
            ),
            NovelReaderScreenPolicy.errorMessageState(null),
        )
    }

    @Test
    fun topBarStateUsesDocumentTitleAndBackLabel() {
        assertEquals(
            NovelReaderScreenPolicy.TopBarState(
                title = "Book",
                backContentDescription = "Back",
            ),
            NovelReaderScreenPolicy.topBarState("Book"),
        )
        assertEquals(
            NovelReaderScreenPolicy.TopBarState(
                title = "",
                backContentDescription = "Back",
            ),
            NovelReaderScreenPolicy.topBarState(null),
        )
    }

    @Test
    fun hostLaunchActionFinishesForMissingOrInvalidRoots() {
        assertEquals(
            NovelReaderScreenPolicy.HostLaunchAction.Finish,
            NovelReaderScreenPolicy.hostLaunchAction(
                bookDirPath = null,
                rootExists = true,
                rootIsDirectory = true,
                rootName = "Book",
                storedMetadata = NovelBookMetadata(folder = "Book"),
            ),
        )
        assertEquals(
            NovelReaderScreenPolicy.HostLaunchAction.Finish,
            NovelReaderScreenPolicy.hostLaunchAction(
                bookDirPath = "/books/Book",
                rootExists = false,
                rootIsDirectory = true,
                rootName = "Book",
                storedMetadata = NovelBookMetadata(folder = "Book"),
            ),
        )
        assertEquals(
            NovelReaderScreenPolicy.HostLaunchAction.Finish,
            NovelReaderScreenPolicy.hostLaunchAction(
                bookDirPath = "/books/Book",
                rootExists = true,
                rootIsDirectory = false,
                rootName = "Book",
                storedMetadata = NovelBookMetadata(folder = "Book"),
            ),
        )
    }

    @Test
    fun hostLaunchActionUsesStoredMetadataOrFolderFallback() {
        val storedMetadata = NovelBookMetadata(
            id = "book-id",
            title = "Stored",
            folder = "stored-folder",
        )

        assertEquals(
            NovelReaderScreenPolicy.HostLaunchAction.OpenBook(storedMetadata),
            NovelReaderScreenPolicy.hostLaunchAction(
                bookDirPath = "/books/Book",
                rootExists = true,
                rootIsDirectory = true,
                rootName = "Book",
                storedMetadata = storedMetadata,
            ),
        )
        val fallbackAction = NovelReaderScreenPolicy.hostLaunchAction(
            bookDirPath = "/books/Book",
            rootExists = true,
            rootIsDirectory = true,
            rootName = "Book",
            storedMetadata = null,
        )
        assertTrue(fallbackAction is NovelReaderScreenPolicy.HostLaunchAction.OpenBook)
        assertEquals("Book", fallbackAction.metadata.folder)
        assertEquals(null, fallbackAction.metadata.title)
    }

    @Test
    fun hostLifecycleActionMapsPauseResumeOnly() {
        assertEquals(
            NovelReaderScreenPolicy.HostLifecycleAction.MarkReaderBackgrounded,
            NovelReaderScreenPolicy.hostLifecycleAction(NovelReaderScreenPolicy.HostLifecycleEvent.PAUSE),
        )
        assertEquals(
            NovelReaderScreenPolicy.HostLifecycleAction.MarkReaderForegrounded,
            NovelReaderScreenPolicy.hostLifecycleAction(NovelReaderScreenPolicy.HostLifecycleEvent.RESUME),
        )
        assertEquals(
            NovelReaderScreenPolicy.HostLifecycleAction.Ignore,
            NovelReaderScreenPolicy.hostLifecycleAction(NovelReaderScreenPolicy.HostLifecycleEvent.OTHER),
        )
    }

    @Test
    fun readerInteractionDecisionsRemainStable() {
        assertTrue(NovelReaderScreenPolicy.shouldInitializeSasayakiPlayer(hasPlayer = false))
        assertFalse(NovelReaderScreenPolicy.shouldInitializeSasayakiPlayer(hasPlayer = true))
        assertTrue(NovelReaderScreenPolicy.shouldClearSelectionWhenPopupChanges(isPopupActive = false))
        assertFalse(NovelReaderScreenPolicy.shouldClearSelectionWhenPopupChanges(isPopupActive = true))
        assertFalse(NovelReaderScreenPolicy.focusModeAfterReaderTap(focusMode = true))
        assertFalse(NovelReaderScreenPolicy.focusModeAfterReaderTap(focusMode = false))
        assertFalse(NovelReaderScreenPolicy.hudVisibleAfterReaderTap(showHud = true))
        assertTrue(NovelReaderScreenPolicy.hudVisibleAfterReaderTap(showHud = false))
        assertFalse(NovelReaderScreenPolicy.hudVisibleAfterToggle(showHud = true))
        assertTrue(NovelReaderScreenPolicy.hudVisibleAfterToggle(showHud = false))
    }

    @Test
    fun overlayAndSheetDecisionsRemainStable() {
        assertTrue(
            NovelReaderScreenPolicy.shouldShowTrackingIndicator(
                showHud = false,
                tracking = true,
            ),
        )
        assertFalse(
            NovelReaderScreenPolicy.shouldShowTrackingIndicator(
                showHud = true,
                tracking = true,
            ),
        )
        assertFalse(
            NovelReaderScreenPolicy.shouldShowTrackingIndicator(
                showHud = false,
                tracking = false,
            ),
        )
        assertTrue(NovelReaderScreenPolicy.shouldLockTrackingForSheet(activeSheetOpen = true))
        assertFalse(NovelReaderScreenPolicy.shouldLockTrackingForSheet(activeSheetOpen = false))
    }

    @Test
    fun visualConstantsRemainStable() {
        assertEquals(0.9f, NovelReaderScreenPolicy.HUD_CONTAINER_ALPHA)
        assertEquals(0.7f, NovelReaderScreenPolicy.HUD_SECONDARY_CONTENT_ALPHA)
        assertEquals(0xFF4CAF50.toInt(), NovelReaderScreenPolicy.TRACKING_INDICATOR_COLOR)
        assertEquals("Missing root URL", NovelReaderScreenPolicy.MISSING_ROOT_URL_MESSAGE)
        assertEquals(
            NovelReaderScreenPolicy.SystemBarsState(
                visible = true,
                useDarkIcons = true,
            ),
            NovelReaderScreenPolicy.systemBarsState(
                showHud = true,
                backgroundColor = 0xFFFFFFFF.toInt(),
            ),
        )
        assertEquals(
            NovelReaderScreenPolicy.SystemBarsState(
                visible = false,
                useDarkIcons = false,
            ),
            NovelReaderScreenPolicy.systemBarsState(
                showHud = false,
                backgroundColor = 0xFF000000.toInt(),
            ),
        )
    }
}
