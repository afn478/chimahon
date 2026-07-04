package tachiyomi.domain.reader.service

object NovelReaderWebHostPolicy {
    enum class CachePolicy {
        NO_CACHE,
    }

    enum class OverScrollPolicy {
        NEVER,
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
}
