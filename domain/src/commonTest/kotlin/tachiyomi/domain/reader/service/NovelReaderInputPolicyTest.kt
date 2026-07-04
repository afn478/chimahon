package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NovelReaderInputPolicyTest {
    @Test
    fun hardwareKeyDownConsumesReaderNavigationKeys() {
        listOf(
            NovelReaderInputPolicy.HardwareKey.DPAD_LEFT,
            NovelReaderInputPolicy.HardwareKey.DPAD_RIGHT,
            NovelReaderInputPolicy.HardwareKey.DPAD_UP,
            NovelReaderInputPolicy.HardwareKey.DPAD_DOWN,
            NovelReaderInputPolicy.HardwareKey.PAGE_UP,
            NovelReaderInputPolicy.HardwareKey.PAGE_DOWN,
            NovelReaderInputPolicy.HardwareKey.MENU,
        ).forEach { key ->
            assertEquals(
                NovelReaderInputPolicy.HardwareKeyAction.Consume,
                NovelReaderInputPolicy.hardwareKeyDownAction(
                    key = key,
                    popupActive = false,
                ),
            )
        }
    }

    @Test
    fun hardwareKeyDownIgnoresNonNavigationKeysAndPopupState() {
        listOf(
            NovelReaderInputPolicy.HardwareKey.VOLUME_UP,
            NovelReaderInputPolicy.HardwareKey.VOLUME_DOWN,
            NovelReaderInputPolicy.HardwareKey.NEXT,
            NovelReaderInputPolicy.HardwareKey.PREVIOUS,
            NovelReaderInputPolicy.HardwareKey.OTHER,
        ).forEach { key ->
            assertEquals(
                NovelReaderInputPolicy.HardwareKeyAction.Ignore,
                NovelReaderInputPolicy.hardwareKeyDownAction(
                    key = key,
                    popupActive = false,
                ),
            )
        }

        assertEquals(
            NovelReaderInputPolicy.HardwareKeyAction.Ignore,
            NovelReaderInputPolicy.hardwareKeyDownAction(
                key = NovelReaderInputPolicy.HardwareKey.DPAD_DOWN,
                popupActive = true,
            ),
        )
    }

    @Test
    fun hardwareKeyUpMapsPagingChapterAndHudActions() {
        assertEquals(
            NovelReaderInputPolicy.HardwareKeyAction.HandleVolumeKey(forward = false),
            NovelReaderInputPolicy.hardwareKeyUpAction(
                key = NovelReaderInputPolicy.HardwareKey.VOLUME_UP,
                popupActive = false,
                ctrlPressed = false,
            ),
        )
        assertEquals(
            NovelReaderInputPolicy.HardwareKeyAction.HandleVolumeKey(forward = true),
            NovelReaderInputPolicy.hardwareKeyUpAction(
                key = NovelReaderInputPolicy.HardwareKey.VOLUME_DOWN,
                popupActive = false,
                ctrlPressed = false,
            ),
        )
        assertEquals(
            NovelReaderInputPolicy.HardwareKeyAction.Paginate(forward = false),
            NovelReaderInputPolicy.hardwareKeyUpAction(
                key = NovelReaderInputPolicy.HardwareKey.DPAD_UP,
                popupActive = false,
                ctrlPressed = false,
            ),
        )
        assertEquals(
            NovelReaderInputPolicy.HardwareKeyAction.Paginate(forward = true),
            NovelReaderInputPolicy.hardwareKeyUpAction(
                key = NovelReaderInputPolicy.HardwareKey.PAGE_DOWN,
                popupActive = false,
                ctrlPressed = false,
            ),
        )
        assertEquals(
            NovelReaderInputPolicy.HardwareKeyAction.Paginate(forward = false),
            NovelReaderInputPolicy.hardwareKeyUpAction(
                key = NovelReaderInputPolicy.HardwareKey.DPAD_LEFT,
                popupActive = false,
                ctrlPressed = false,
            ),
        )
        assertEquals(
            NovelReaderInputPolicy.HardwareKeyAction.Paginate(forward = true),
            NovelReaderInputPolicy.hardwareKeyUpAction(
                key = NovelReaderInputPolicy.HardwareKey.DPAD_RIGHT,
                popupActive = false,
                ctrlPressed = false,
            ),
        )
        assertEquals(
            NovelReaderInputPolicy.HardwareKeyAction.ChangeChapter(forward = false),
            NovelReaderInputPolicy.hardwareKeyUpAction(
                key = NovelReaderInputPolicy.HardwareKey.DPAD_LEFT,
                popupActive = false,
                ctrlPressed = true,
            ),
        )
        assertEquals(
            NovelReaderInputPolicy.HardwareKeyAction.ChangeChapter(forward = true),
            NovelReaderInputPolicy.hardwareKeyUpAction(
                key = NovelReaderInputPolicy.HardwareKey.DPAD_RIGHT,
                popupActive = false,
                ctrlPressed = true,
            ),
        )
        assertEquals(
            NovelReaderInputPolicy.HardwareKeyAction.ChangeChapter(forward = true),
            NovelReaderInputPolicy.hardwareKeyUpAction(
                key = NovelReaderInputPolicy.HardwareKey.NEXT,
                popupActive = false,
                ctrlPressed = false,
            ),
        )
        assertEquals(
            NovelReaderInputPolicy.HardwareKeyAction.ChangeChapter(forward = false),
            NovelReaderInputPolicy.hardwareKeyUpAction(
                key = NovelReaderInputPolicy.HardwareKey.PREVIOUS,
                popupActive = false,
                ctrlPressed = false,
            ),
        )
        assertEquals(
            NovelReaderInputPolicy.HardwareKeyAction.ToggleHud,
            NovelReaderInputPolicy.hardwareKeyUpAction(
                key = NovelReaderInputPolicy.HardwareKey.MENU,
                popupActive = false,
                ctrlPressed = false,
            ),
        )
    }

    @Test
    fun hardwareKeyUpIgnoresPopupStateAndUnknownKeys() {
        assertEquals(
            NovelReaderInputPolicy.HardwareKeyAction.Ignore,
            NovelReaderInputPolicy.hardwareKeyUpAction(
                key = NovelReaderInputPolicy.HardwareKey.MENU,
                popupActive = true,
                ctrlPressed = false,
            ),
        )
        assertEquals(
            NovelReaderInputPolicy.HardwareKeyAction.Ignore,
            NovelReaderInputPolicy.hardwareKeyUpAction(
                key = NovelReaderInputPolicy.HardwareKey.OTHER,
                popupActive = false,
                ctrlPressed = true,
            ),
        )
    }

    @Test
    fun swipeForwardUsesVerticalWritingHorizontalRtlDirection() {
        assertEquals(
            true,
            NovelReaderInputPolicy.swipeForward(
                deltaX = 120f,
                deltaY = 10f,
                velocityX = 500f,
                velocityY = 0f,
                swipeThreshold = 96f,
                continuousMode = false,
                verticalWriting = true,
            ),
        )
        assertEquals(
            false,
            NovelReaderInputPolicy.swipeForward(
                deltaX = -120f,
                deltaY = 10f,
                velocityX = -500f,
                velocityY = 0f,
                swipeThreshold = 96f,
                continuousMode = false,
                verticalWriting = true,
            ),
        )
    }

    @Test
    fun swipeForwardUsesHorizontalLtrDirection() {
        assertEquals(
            true,
            NovelReaderInputPolicy.swipeForward(
                deltaX = -120f,
                deltaY = 10f,
                velocityX = -500f,
                velocityY = 0f,
                swipeThreshold = 96f,
                continuousMode = false,
                verticalWriting = false,
            ),
        )
        assertEquals(
            false,
            NovelReaderInputPolicy.swipeForward(
                deltaX = 120f,
                deltaY = 10f,
                velocityX = 500f,
                velocityY = 0f,
                swipeThreshold = 96f,
                continuousMode = false,
                verticalWriting = false,
            ),
        )
    }

    @Test
    fun swipeForwardUsesVerticalContinuousDirectionForHorizontalWriting() {
        assertEquals(
            true,
            NovelReaderInputPolicy.swipeForward(
                deltaX = 10f,
                deltaY = -120f,
                velocityX = 0f,
                velocityY = -500f,
                swipeThreshold = 96f,
                continuousMode = true,
                verticalWriting = false,
            ),
        )
        assertEquals(
            false,
            NovelReaderInputPolicy.swipeForward(
                deltaX = 10f,
                deltaY = 120f,
                velocityX = 0f,
                velocityY = 500f,
                swipeThreshold = 96f,
                continuousMode = true,
                verticalWriting = false,
            ),
        )
    }

    @Test
    fun swipeForwardRejectsWrongAxisShortDistanceAndSlowVelocity() {
        assertNull(
            NovelReaderInputPolicy.swipeForward(
                deltaX = 10f,
                deltaY = 120f,
                velocityX = 0f,
                velocityY = 500f,
                swipeThreshold = 96f,
                continuousMode = false,
                verticalWriting = false,
            ),
        )
        assertNull(
            NovelReaderInputPolicy.swipeForward(
                deltaX = -95f,
                deltaY = 10f,
                velocityX = -500f,
                velocityY = 0f,
                swipeThreshold = 96f,
                continuousMode = false,
                verticalWriting = false,
            ),
        )
        assertNull(
            NovelReaderInputPolicy.swipeForward(
                deltaX = -120f,
                deltaY = 10f,
                velocityX = -399f,
                velocityY = 0f,
                swipeThreshold = 96f,
                continuousMode = false,
                verticalWriting = false,
            ),
        )
    }

    @Test
    fun backgroundTapActionUsesTopAndBottomOverlayZones() {
        assertEquals(
            NovelReaderInputPolicy.TapAction.TOGGLE_OVERLAY,
            NovelReaderInputPolicy.backgroundTapAction(
                x = 250f,
                y = 20f,
                width = 500,
                height = 800,
                tapZonePx = 64,
                tapZonePercent = 20,
                verticalWriting = true,
            ),
        )
        assertEquals(
            NovelReaderInputPolicy.TapAction.TOGGLE_OVERLAY,
            NovelReaderInputPolicy.backgroundTapAction(
                x = 250f,
                y = 780f,
                width = 500,
                height = 800,
                tapZonePx = 64,
                tapZonePercent = 20,
                verticalWriting = true,
            ),
        )
    }

    @Test
    fun backgroundTapActionMapsSideZonesForVerticalWriting() {
        assertEquals(
            NovelReaderInputPolicy.TapAction.FORWARD,
            NovelReaderInputPolicy.backgroundTapAction(
                x = 50f,
                y = 400f,
                width = 500,
                height = 800,
                tapZonePx = 64,
                tapZonePercent = 20,
                verticalWriting = true,
            ),
        )
        assertEquals(
            NovelReaderInputPolicy.TapAction.BACKWARD,
            NovelReaderInputPolicy.backgroundTapAction(
                x = 450f,
                y = 400f,
                width = 500,
                height = 800,
                tapZonePx = 64,
                tapZonePercent = 20,
                verticalWriting = true,
            ),
        )
    }

    @Test
    fun backgroundTapActionMapsSideZonesForHorizontalWriting() {
        assertEquals(
            NovelReaderInputPolicy.TapAction.BACKWARD,
            NovelReaderInputPolicy.backgroundTapAction(
                x = 50f,
                y = 400f,
                width = 500,
                height = 800,
                tapZonePx = 64,
                tapZonePercent = 20,
                verticalWriting = false,
            ),
        )
        assertEquals(
            NovelReaderInputPolicy.TapAction.FORWARD,
            NovelReaderInputPolicy.backgroundTapAction(
                x = 450f,
                y = 400f,
                width = 500,
                height = 800,
                tapZonePx = 64,
                tapZonePercent = 20,
                verticalWriting = false,
            ),
        )
    }

    @Test
    fun backgroundTapActionReturnsNoneForCenterOrInvalidSize() {
        assertEquals(
            NovelReaderInputPolicy.TapAction.NONE,
            NovelReaderInputPolicy.backgroundTapAction(
                x = 250f,
                y = 400f,
                width = 500,
                height = 800,
                tapZonePx = 64,
                tapZonePercent = 20,
                verticalWriting = true,
            ),
        )
        assertEquals(
            NovelReaderInputPolicy.TapAction.NONE,
            NovelReaderInputPolicy.backgroundTapAction(
                x = 10f,
                y = 10f,
                width = 0,
                height = 800,
                tapZonePx = 64,
                tapZonePercent = 20,
                verticalWriting = true,
            ),
        )
    }

    @Test
    fun pointerMoveDistanceUsesManhattanMovementForTouchSlop() {
        assertEquals(
            7f,
            NovelReaderInputPolicy.pointerMoveDistance(
                previousX = 1f,
                previousY = 2f,
                currentX = 5f,
                currentY = -1f,
            ),
        )
    }

    @Test
    fun shouldHandleTapRequiresMovementBelowThreshold() {
        assertTrue(NovelReaderInputPolicy.shouldHandleTap(totalMovement = 19.9f))
        assertFalse(NovelReaderInputPolicy.shouldHandleTap(totalMovement = NovelReaderInputPolicy.MAX_TAP_MOVEMENT))
        assertFalse(
            NovelReaderInputPolicy.shouldHandleTap(
                totalMovement = 6f,
                tapMovementThreshold = 5f,
            ),
        )
    }
}
