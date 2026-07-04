package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals

class NovelReaderWebHostPolicyTest {
    @Test
    fun defaultHostSettingsKeepReaderWebContractStable() {
        assertEquals(
            NovelReaderWebHostPolicy.HostSettings(
                allowFileAccess = true,
                allowContentAccess = true,
                allowFileAccessFromFileUrls = true,
                allowUniversalAccessFromFileUrls = true,
                javaScriptEnabled = true,
                domStorageEnabled = true,
                cachePolicy = NovelReaderWebHostPolicy.CachePolicy.NO_CACHE,
                builtInZoomControls = false,
                displayZoomControls = false,
                loadWithOverviewMode = false,
                useWideViewPort = true,
                allowMixedContent = true,
                verticalScrollBarEnabled = false,
                horizontalScrollBarEnabled = false,
                overScrollPolicy = NovelReaderWebHostPolicy.OverScrollPolicy.NEVER,
            ),
            NovelReaderWebHostPolicy.defaultHostSettings(),
        )
    }
}
