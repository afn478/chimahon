package tachiyomi.domain.reader.service

import kotlin.math.abs

object NovelReaderInputPolicy {
    const val MIN_FLING_VELOCITY = 400f
    const val MAX_TAP_MOVEMENT = 20f

    enum class HardwareKey {
        VOLUME_UP,
        VOLUME_DOWN,
        DPAD_UP,
        DPAD_DOWN,
        DPAD_LEFT,
        DPAD_RIGHT,
        PAGE_UP,
        PAGE_DOWN,
        NEXT,
        PREVIOUS,
        MENU,
        OTHER,
    }

    sealed interface HardwareKeyAction {
        data object Ignore : HardwareKeyAction
        data object Consume : HardwareKeyAction
        data class HandleVolumeKey(val forward: Boolean) : HardwareKeyAction
        data class Paginate(val forward: Boolean) : HardwareKeyAction
        data class ChangeChapter(val forward: Boolean) : HardwareKeyAction
        data object ToggleHud : HardwareKeyAction
    }

    enum class TapAction {
        NONE,
        TOGGLE_OVERLAY,
        FORWARD,
        BACKWARD,
    }

    fun swipeForward(
        deltaX: Float,
        deltaY: Float,
        velocityX: Float,
        velocityY: Float,
        swipeThreshold: Float,
        continuousMode: Boolean,
        verticalWriting: Boolean,
    ): Boolean? {
        val isVerticalSwipe = abs(deltaY) > abs(deltaX)
        val expectsVerticalSwipe = continuousMode && !verticalWriting
        if (isVerticalSwipe != expectsVerticalSwipe) return null

        val primaryDelta = if (isVerticalSwipe) deltaY else deltaX
        val primaryVelocity = if (isVerticalSwipe) velocityY else velocityX
        if (abs(primaryDelta) < swipeThreshold) return null
        if (abs(primaryVelocity) < MIN_FLING_VELOCITY) return null

        return when {
            expectsVerticalSwipe -> deltaY < 0f
            verticalWriting -> deltaX > 0f
            else -> deltaX < 0f
        }
    }

    fun backgroundTapAction(
        x: Float,
        y: Float,
        width: Int,
        height: Int,
        tapZonePx: Int,
        tapZonePercent: Int,
        verticalWriting: Boolean,
    ): TapAction {
        if (width <= 0 || height <= 0) return TapAction.NONE

        if (y < tapZonePx || y > height - tapZonePx) {
            return TapAction.TOGGLE_OVERLAY
        }

        val sideZoneFraction = tapZonePercent.coerceIn(0, 50) / 100f
        val leftZoneEnd = width * sideZoneFraction
        val rightZoneStart = width * (1f - sideZoneFraction)

        return when {
            x < leftZoneEnd -> if (verticalWriting) TapAction.FORWARD else TapAction.BACKWARD
            x > rightZoneStart -> if (verticalWriting) TapAction.BACKWARD else TapAction.FORWARD
            else -> TapAction.NONE
        }
    }

    fun hardwareKeyDownAction(
        key: HardwareKey,
        popupActive: Boolean,
    ): HardwareKeyAction {
        if (popupActive) return HardwareKeyAction.Ignore

        return when (key) {
            HardwareKey.DPAD_LEFT,
            HardwareKey.DPAD_RIGHT,
            HardwareKey.DPAD_UP,
            HardwareKey.DPAD_DOWN,
            HardwareKey.PAGE_UP,
            HardwareKey.PAGE_DOWN,
            HardwareKey.MENU,
            -> HardwareKeyAction.Consume
            HardwareKey.VOLUME_UP,
            HardwareKey.VOLUME_DOWN,
            HardwareKey.NEXT,
            HardwareKey.PREVIOUS,
            HardwareKey.OTHER,
            -> HardwareKeyAction.Ignore
        }
    }

    fun hardwareKeyUpAction(
        key: HardwareKey,
        popupActive: Boolean,
        ctrlPressed: Boolean,
    ): HardwareKeyAction {
        if (popupActive) return HardwareKeyAction.Ignore

        return when (key) {
            HardwareKey.VOLUME_UP -> HardwareKeyAction.HandleVolumeKey(forward = false)
            HardwareKey.VOLUME_DOWN -> HardwareKeyAction.HandleVolumeKey(forward = true)
            HardwareKey.DPAD_UP,
            HardwareKey.PAGE_UP,
            -> HardwareKeyAction.Paginate(forward = false)
            HardwareKey.DPAD_DOWN,
            HardwareKey.PAGE_DOWN,
            -> HardwareKeyAction.Paginate(forward = true)
            HardwareKey.DPAD_LEFT -> if (ctrlPressed) {
                HardwareKeyAction.ChangeChapter(forward = false)
            } else {
                HardwareKeyAction.Paginate(forward = false)
            }
            HardwareKey.DPAD_RIGHT -> if (ctrlPressed) {
                HardwareKeyAction.ChangeChapter(forward = true)
            } else {
                HardwareKeyAction.Paginate(forward = true)
            }
            HardwareKey.NEXT -> HardwareKeyAction.ChangeChapter(forward = true)
            HardwareKey.PREVIOUS -> HardwareKeyAction.ChangeChapter(forward = false)
            HardwareKey.MENU -> HardwareKeyAction.ToggleHud
            HardwareKey.OTHER -> HardwareKeyAction.Ignore
        }
    }

    fun pointerMoveDistance(
        previousX: Float,
        previousY: Float,
        currentX: Float,
        currentY: Float,
    ): Float {
        return abs(currentX - previousX) + abs(currentY - previousY)
    }

    fun shouldHandleTap(
        totalMovement: Float,
        tapMovementThreshold: Float = MAX_TAP_MOVEMENT,
    ): Boolean {
        return totalMovement < tapMovementThreshold
    }
}
