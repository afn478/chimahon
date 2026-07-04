package tachiyomi.domain.reader.service

object NovelReaderWebHostPolicy {
    const val RESTORE_COMPLETED_FADE_DURATION_MS = 160L

    enum class CachePolicy {
        NO_CACHE,
    }

    enum class OverScrollPolicy {
        NEVER,
    }

    enum class HostViewVisibility {
        VISIBLE,
        INVISIBLE,
    }

    data class HostSettings(
        val allowFileAccess: Boolean,
        val allowContentAccess: Boolean,
        val allowFileAccessFromFileUrls: Boolean,
        val allowUniversalAccessFromFileUrls: Boolean,
        val javaScriptEnabled: Boolean,
        val domStorageEnabled: Boolean,
        val cachePolicy: CachePolicy,
        val builtInZoomControls: Boolean,
        val displayZoomControls: Boolean,
        val loadWithOverviewMode: Boolean,
        val useWideViewPort: Boolean,
        val allowMixedContent: Boolean,
        val verticalScrollBarEnabled: Boolean,
        val horizontalScrollBarEnabled: Boolean,
        val overScrollPolicy: OverScrollPolicy,
    )

    data class HostViewState(
        val visibility: HostViewVisibility,
        val alpha: Float? = null,
    )

    data class HostViewTransition(
        val visibility: HostViewVisibility,
        val startAlpha: Float,
        val targetAlpha: Float,
        val durationMillis: Long,
    )

    fun defaultHostSettings(): HostSettings {
        return HostSettings(
            allowFileAccess = true,
            allowContentAccess = true,
            allowFileAccessFromFileUrls = true,
            allowUniversalAccessFromFileUrls = true,
            javaScriptEnabled = true,
            domStorageEnabled = true,
            cachePolicy = CachePolicy.NO_CACHE,
            builtInZoomControls = false,
            displayZoomControls = false,
            loadWithOverviewMode = false,
            useWideViewPort = true,
            allowMixedContent = true,
            verticalScrollBarEnabled = false,
            horizontalScrollBarEnabled = false,
            overScrollPolicy = OverScrollPolicy.NEVER,
        )
    }

    fun pageStartedViewState(): HostViewState {
        return HostViewState(
            visibility = HostViewVisibility.VISIBLE,
            alpha = 0f,
        )
    }

    fun chapterLoadingViewState(): HostViewState {
        return HostViewState(
            visibility = HostViewVisibility.INVISIBLE,
        )
    }

    fun chapterChangedViewState(chapterChanged: Boolean): HostViewState? {
        return if (chapterChanged) chapterLoadingViewState() else null
    }

    fun restoreCompletedTransition(): HostViewTransition {
        return HostViewTransition(
            visibility = HostViewVisibility.VISIBLE,
            startAlpha = 0f,
            targetAlpha = 1f,
            durationMillis = RESTORE_COMPLETED_FADE_DURATION_MS,
        )
    }
}
