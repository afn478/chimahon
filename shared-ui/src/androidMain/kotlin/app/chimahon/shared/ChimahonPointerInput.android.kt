package app.chimahon.shared

import androidx.compose.ui.Modifier

internal actual fun Modifier.readerScrollWheelNavigation(
    enabled: Boolean,
    onVerticalScroll: (Float) -> Unit,
): Modifier = this
