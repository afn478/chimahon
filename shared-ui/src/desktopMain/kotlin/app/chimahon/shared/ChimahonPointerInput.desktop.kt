package app.chimahon.shared

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import kotlin.math.abs

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.readerScrollWheelNavigation(
    enabled: Boolean,
    onVerticalScroll: (Float) -> Unit,
): Modifier {
    if (!enabled) return this
    return onPointerEvent(PointerEventType.Scroll) { event ->
        if (event.changes.any { abs(it.scrollDelta.y) > abs(it.scrollDelta.x) }) {
            onVerticalScroll(event.changes.sumOf { it.scrollDelta.y.toDouble() }.toFloat())
        }
    }
}
