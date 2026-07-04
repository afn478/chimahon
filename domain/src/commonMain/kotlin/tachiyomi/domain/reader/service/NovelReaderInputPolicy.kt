package tachiyomi.domain.reader.service

import kotlin.math.abs

object NovelReaderInputPolicy {
    const val MIN_FLING_VELOCITY = 400f
    const val MAX_TAP_MOVEMENT = 20f

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
