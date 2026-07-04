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

    @Test
    fun pageStartedViewStateShowsTransparentReaderHost() {
        assertEquals(
            NovelReaderWebHostPolicy.HostViewState(
                visibility = NovelReaderWebHostPolicy.HostViewVisibility.VISIBLE,
                alpha = 0f,
            ),
            NovelReaderWebHostPolicy.pageStartedViewState(),
        )
    }

    @Test
    fun chapterLoadingViewStateHidesReaderHostWithoutChangingAlpha() {
        assertEquals(
            NovelReaderWebHostPolicy.HostViewState(
                visibility = NovelReaderWebHostPolicy.HostViewVisibility.INVISIBLE,
                alpha = null,
            ),
            NovelReaderWebHostPolicy.chapterLoadingViewState(),
        )
        assertEquals(
            NovelReaderWebHostPolicy.chapterLoadingViewState(),
            NovelReaderWebHostPolicy.chapterChangedViewState(chapterChanged = true),
        )
        assertEquals(
            null,
            NovelReaderWebHostPolicy.chapterChangedViewState(chapterChanged = false),
        )
    }

    @Test
    fun restoreCompletedTransitionFadesReaderHostIn() {
        assertEquals(
            NovelReaderWebHostPolicy.HostViewTransition(
                visibility = NovelReaderWebHostPolicy.HostViewVisibility.VISIBLE,
                startAlpha = 0f,
                targetAlpha = 1f,
                durationMillis = NovelReaderWebHostPolicy.RESTORE_COMPLETED_FADE_DURATION_MS,
            ),
            NovelReaderWebHostPolicy.restoreCompletedTransition(),
        )
    }
}
