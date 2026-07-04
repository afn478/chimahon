package tachiyomi.domain.reader.service

object NovelReaderScreenPolicy {
    const val BOOK_OPEN_ERROR_MESSAGE = "Could not open book"
    const val MISSING_ROOT_URL_MESSAGE = "Missing root URL"
    const val LOADING_MESSAGE = "Opening..."
    const val BACK_CONTENT_DESCRIPTION = "Back"
    const val HUD_CONTAINER_ALPHA = 0.9f
    const val HUD_SECONDARY_CONTENT_ALPHA = 0.7f
    const val TRACKING_INDICATOR_COLOR = 0xFF4CAF50.toInt()

    data class TopBarState(
        val title: String,
        val backContentDescription: String,
    )

    data class MessageState(
        val text: String,
        val loading: Boolean,
    )

    fun loadingMessageState(): MessageState {
        return MessageState(
            text = LOADING_MESSAGE,
            loading = true,
        )
    }

    fun errorMessageState(message: String?): MessageState {
        return MessageState(
            text = message ?: BOOK_OPEN_ERROR_MESSAGE,
            loading = false,
        )
    }

    fun topBarState(documentTitle: String?): TopBarState {
        return TopBarState(
            title = documentTitle.orEmpty(),
            backContentDescription = BACK_CONTENT_DESCRIPTION,
        )
    }

    fun shouldInitializeSasayakiPlayer(hasPlayer: Boolean): Boolean {
        return !hasPlayer
    }

    fun shouldClearSelectionWhenPopupChanges(isPopupActive: Boolean): Boolean {
        return !isPopupActive
    }

    fun focusModeAfterReaderTap(focusMode: Boolean): Boolean {
        return if (focusMode) false else focusMode
    }

    fun hudVisibleAfterReaderTap(showHud: Boolean): Boolean {
        return !showHud
    }

    fun shouldShowTrackingIndicator(
        showHud: Boolean,
        tracking: Boolean,
    ): Boolean {
        return !showHud && tracking
    }

    fun shouldLockTrackingForSheet(activeSheetOpen: Boolean): Boolean {
        return activeSheetOpen
    }
}
