package tachiyomi.domain.reader.service

import tachiyomi.domain.library.model.NovelBookMetadata

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

    sealed interface HostLaunchAction {
        data object Finish : HostLaunchAction
        data class OpenBook(val metadata: NovelBookMetadata) : HostLaunchAction
    }

    enum class HostLifecycleEvent {
        PAUSE,
        RESUME,
        OTHER,
    }

    sealed interface HostLifecycleAction {
        data object Ignore : HostLifecycleAction
        data object MarkReaderBackgrounded : HostLifecycleAction
        data object MarkReaderForegrounded : HostLifecycleAction
    }

    data class SystemBarsState(
        val visible: Boolean,
        val useDarkIcons: Boolean,
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

    fun hostLaunchAction(
        bookDirPath: String?,
        rootExists: Boolean,
        rootIsDirectory: Boolean,
        rootName: String,
        storedMetadata: NovelBookMetadata?,
    ): HostLaunchAction {
        return if (bookDirPath.isNullOrEmpty() || !rootExists || !rootIsDirectory) {
            HostLaunchAction.Finish
        } else {
            HostLaunchAction.OpenBook(
                metadata = storedMetadata ?: NovelBookMetadata(folder = rootName),
            )
        }
    }

    fun hostLifecycleAction(event: HostLifecycleEvent): HostLifecycleAction {
        return when (event) {
            HostLifecycleEvent.PAUSE -> HostLifecycleAction.MarkReaderBackgrounded
            HostLifecycleEvent.RESUME -> HostLifecycleAction.MarkReaderForegrounded
            HostLifecycleEvent.OTHER -> HostLifecycleAction.Ignore
        }
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

    fun hudVisibleAfterToggle(showHud: Boolean): Boolean {
        return !showHud
    }

    fun systemBarsState(
        showHud: Boolean,
        backgroundColor: Int,
    ): SystemBarsState {
        return SystemBarsState(
            visible = showHud,
            useDarkIcons = NovelReaderAppearancePolicy.shouldUseDarkSystemBarIcons(backgroundColor),
        )
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
