package app.chimahon.shared

import androidx.compose.ui.Modifier

internal expect fun Modifier.readerScrollWheelNavigation(
    enabled: Boolean,
    onVerticalScroll: (Float) -> Unit,
): Modifier
