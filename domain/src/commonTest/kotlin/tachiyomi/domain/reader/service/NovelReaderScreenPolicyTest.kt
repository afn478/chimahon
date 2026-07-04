package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun readerInteractionDecisionsRemainStable() {
        assertTrue(NovelReaderScreenPolicy.shouldInitializeSasayakiPlayer(hasPlayer = false))
        assertFalse(NovelReaderScreenPolicy.shouldInitializeSasayakiPlayer(hasPlayer = true))
        assertTrue(NovelReaderScreenPolicy.shouldClearSelectionWhenPopupChanges(isPopupActive = false))
        assertFalse(NovelReaderScreenPolicy.shouldClearSelectionWhenPopupChanges(isPopupActive = true))
        assertFalse(NovelReaderScreenPolicy.focusModeAfterReaderTap(focusMode = true))
        assertFalse(NovelReaderScreenPolicy.focusModeAfterReaderTap(focusMode = false))
        assertFalse(NovelReaderScreenPolicy.hudVisibleAfterReaderTap(showHud = true))
        assertTrue(NovelReaderScreenPolicy.hudVisibleAfterReaderTap(showHud = false))
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
    }
}
