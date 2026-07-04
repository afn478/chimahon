package com.canopus.chimareader.ui.reader

import android.R.attr.overScrollMode
import android.R.attr.visibility
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.common.io.Files.append
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.File
import tachiyomi.domain.reader.model.NovelReaderWebCommand
import tachiyomi.domain.reader.model.ReaderSettings
import tachiyomi.domain.reader.service.NovelReaderInputPolicy
import tachiyomi.domain.reader.service.NovelReaderNavigationPolicy
import tachiyomi.domain.reader.service.NovelReaderProgressPolicy
import tachiyomi.domain.reader.service.NovelReaderWebBridgePolicy
import tachiyomi.domain.reader.service.NovelReaderWebCommandActionPolicy
import tachiyomi.domain.reader.service.NovelReaderWebCommandPolicy
import tachiyomi.domain.reader.service.NovelReaderWebGeometryPolicy
import tachiyomi.domain.reader.service.NovelReaderWebHostPolicy
import tachiyomi.domain.reader.service.NovelReaderWebInjectionPolicy
import tachiyomi.domain.reader.service.NovelReaderWebLoadPolicy
import tachiyomi.domain.reader.service.NovelReaderWebNavigationPolicy
import tachiyomi.domain.reader.service.NovelReaderWebResultPolicy
import tachiyomi.domain.reader.service.NovelReaderWebScriptPolicy
import tachiyomi.domain.reader.service.NovelReaderWebSettingsPolicy

@Composable
fun ReaderWebView(
    modifier: Modifier = Modifier,
    bridge: WebViewBridge,
    continuousMode: Boolean = false,
    isImageOnly: Boolean = false,
    readerSettings: ReaderSettings = ReaderSettings(),
    focusMode: Boolean = false,
    onNextChapter: () -> Boolean,
    onPreviousChapter: () -> Boolean,
    onProgressChanged: (Double) -> Unit,
    onLoadFailed: (String) -> Unit,
    onTap: () -> Unit = {},
    onTapTop: () -> Unit = {},
    onTapBottom: () -> Unit = {},
    swipeThreshold: Int = 96,
    tapZonePx: Int = 100,
    isPopupActive: Boolean = false,
    onTextSelected: (word: String, sentence: String, x: Float, y: Float, w: Float, h: Float) -> Unit = { _, _, _, _, _, _ -> },
    onSentenceReady: (sentence: String) -> Unit = {},
    onDismissPopupRequested: () -> Unit = {},
    onInternalLinkClicked: (url: String) -> Unit = {},
    onSelectionRectsReceived: ((String) -> Unit)? = null,
) {
    val pendingCommands = remember(bridge) { bridge.pendingCommands }

    LaunchedEffect(bridge.chapterUrl) {
        val chapterUrl = bridge.chapterUrl ?: return@LaunchedEffect
        if (pendingCommands.isEmpty()) {
            bridge.send(NovelReaderWebCommand.LoadChapter(chapterUrl, bridge.progress))
        }
    }

    val isFirstContinuous = remember { mutableStateOf(true) }
    LaunchedEffect(continuousMode) {
        if (isFirstContinuous.value) {
            isFirstContinuous.value = false
            return@LaunchedEffect
        }
        bridge.chapterUrl?.let { url ->
            bridge.send(NovelReaderWebCommand.LoadChapter(url, bridge.progress))
        }
    }

    LaunchedEffect(focusMode) {
        bridge.send(NovelReaderWebCommand.ChangeFocusMode(focusMode))
    }

    val isFirstComposition = remember { mutableStateOf(true) }
    LaunchedEffect(readerSettings) {
        if (isFirstComposition.value) {
            isFirstComposition.value = false
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(100)
        bridge.send(NovelReaderWebCommand.ApplySettings(readerSettings))
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val hostSettings = NovelReaderWebHostPolicy.defaultHostSettings()
            ReaderAndroidWebView(
                context = context,
                readerJs = loadAssetText(context, "novel/reader.js"),
                continuousMode = continuousMode,
                isImageOnly = isImageOnly,
                readerSettings = readerSettings,
                focusMode = focusMode,
                onNextChapter = onNextChapter,
                onPreviousChapter = onPreviousChapter,
                onProgressChanged = onProgressChanged,
                onLoadFailed = onLoadFailed,
                onTap = onTap,
                onTapTop = { if (!isPopupActive) onTapTop() },
                onTapBottom = { if (!isPopupActive) onTapBottom() },
                isPopupActive = isPopupActive,
                onTextSelectedCallback = onTextSelected,
                onSentenceReadyCallback = onSentenceReady,
                onDismissPopupRequested = onDismissPopupRequested,
                onInternalLinkClicked = onInternalLinkClicked,
            ).apply {
                setSelectionRectsCallback(onSelectionRectsReceived)
                settings.allowFileAccess = hostSettings.allowFileAccess
                settings.allowContentAccess = hostSettings.allowContentAccess
                settings.allowFileAccessFromFileURLs = hostSettings.allowFileAccessFromFileUrls
                settings.allowUniversalAccessFromFileURLs = hostSettings.allowUniversalAccessFromFileUrls
                settings.javaScriptEnabled = hostSettings.javaScriptEnabled
                settings.domStorageEnabled = hostSettings.domStorageEnabled
                settings.cacheMode = hostSettings.cacheMode()
                settings.builtInZoomControls = hostSettings.builtInZoomControls
                settings.displayZoomControls = hostSettings.displayZoomControls
                settings.loadWithOverviewMode = hostSettings.loadWithOverviewMode
                settings.useWideViewPort = hostSettings.useWideViewPort
                settings.mixedContentMode = hostSettings.mixedContentMode()
                isVerticalScrollBarEnabled = hostSettings.verticalScrollBarEnabled
                isHorizontalScrollBarEnabled = hostSettings.horizontalScrollBarEnabled
                overScrollMode = hostSettings.overScrollMode()
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                        Log.d("HoshiReader", "${message.message()} [line ${message.lineNumber()}]")
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    private fun handleUrlLoading(url: String): Boolean {
                        val action = NovelReaderNavigationPolicy.webLinkAction(
                            currentUrl = currentUrl,
                            targetUrl = url,
                        )

                        Log.d("ReaderWebView", "shouldOverrideUrlLoading: url=$url action=$action")

                        when (action) {
                            is NovelReaderNavigationPolicy.WebLinkAction.SameChapter -> {
                                action.fragment?.let { fragment ->
                                    val js = NovelReaderWebScriptPolicy.scrollToFragmentScript(fragment)
                                    post { evaluateJavascript(js, null) }
                                }
                            }
                            is NovelReaderNavigationPolicy.WebLinkAction.NavigateToUrl -> {
                                post { onInternalLinkClicked(action.url) }
                            }
                        }

                        return true
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        return handleUrlLoading(url)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        Log.d("ReaderWebView", "onPageStarted: url=$url")
                        alpha = 0f
                        visibility = View.VISIBLE
                        lastLoadedChapterKey = null
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        Log.d("ReaderWebView", "onPageFinished: url=$url, size=${view?.width}x${view?.height}")
                        injectReader()
                    }

                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: android.webkit.RenderProcessGoneDetail?,
                    ): Boolean {
                        val failure = NovelReaderWebLoadPolicy.rendererGoneFailure(
                            crashed = detail?.didCrash() == true,
                        )
                        Log.e("ReaderWebView", "onRenderProcessGone: ${failure.reason}")
                        post {
                            onLoadFailed(failure.message)
                        }
                        return true
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        Log.e("ReaderWebView", "onReceivedError: url=${request?.url}, error=$error")
                        if (request?.isForMainFrame == true) {
                            onLoadFailed(error?.description?.toString() ?: "Failed to load chapter")
                        }
                    }
                }
            }
        },
        update = { androidWebView ->
            val v = androidWebView as ReaderAndroidWebView
            v.isImageOnly = isImageOnly
            v.continuousMode = continuousMode
            v.readerSettings = readerSettings
            v.focusMode = focusMode
            v.isPopupActive = isPopupActive
            v.setSelectionRectsCallback(onSelectionRectsReceived)
            v.setBackgroundColor(readerSettings.backgroundColor)

            if (pendingCommands.isEmpty()) return@AndroidView

            val commands = pendingCommands.toList()
            pendingCommands.clear()

            val deduped = NovelReaderWebCommandPolicy.collapseConsecutiveLoads(commands)

            deduped.forEach { command ->
                when (
                    val action = NovelReaderWebCommandActionPolicy.commandAction(
                        command = command,
                        currentUrl = v.currentUrl,
                        lastAppliedSettings = v.lastAppliedSettings,
                    )
                ) {
                    is NovelReaderWebCommandActionPolicy.CommandAction.LoadChapter -> {
                        v.pendingProgress = action.progress
                        v.currentUrl = action.url
                        v.loadChapter(action.url)
                    }
                    is NovelReaderWebCommandActionPolicy.CommandAction.ReloadCurrentChapter -> {
                        v.continuousMode = action.continuousMode
                        v.loadChapter(action.url)
                    }
                    is NovelReaderWebCommandActionPolicy.CommandAction.ApplySettings -> {
                        val new = action.settings
                        v.readerSettings = new
                        v.lastAppliedSettings = new

                        when (action.settingsAction) {
                            NovelReaderWebSettingsPolicy.SettingsCommandAction.ReinjectReader -> {
                                v.injectReader()
                            }
                            NovelReaderWebSettingsPolicy.SettingsCommandAction.ApplyLiveSettings -> {
                                v.applySettings(new)
                            }
                        }
                    }
                    is NovelReaderWebCommandActionPolicy.CommandAction.SetFocusMode -> {
                        v.focusMode = action.focusMode
                    }
                    is NovelReaderWebCommandActionPolicy.CommandAction.Paginate -> {
                        v.paginate(action.forward)
                    }
                    is NovelReaderWebCommandActionPolicy.CommandAction.EvaluateScript -> {
                        v.evaluateJavascript(action.script, null)
                    }
                    is NovelReaderWebCommandActionPolicy.CommandAction.RequestSelectionRects -> {
                        v.evaluateJavascript(action.script) { result ->
                            val loc = IntArray(2)
                            v.getLocationOnScreen(loc)
                            val json = NovelReaderWebGeometryPolicy.selectionRectsToScreenJson(
                                json = result,
                                viewportLeft = loc[0].toDouble(),
                                viewportTop = loc[1].toDouble(),
                                scale = v.scale.toDouble(),
                            )
                            onSelectionRectsReceived?.invoke(json)
                        }
                    }
                    NovelReaderWebCommandActionPolicy.CommandAction.Ignore -> Unit
                }
            }
        },
    )
}

private class ReaderAndroidWebView(
    context: Context,
    private val readerJs: String,
    var continuousMode: Boolean = false,
    var isImageOnly: Boolean = false,
    var readerSettings: ReaderSettings = ReaderSettings(),
    var focusMode: Boolean = false,
    private val onNextChapter: () -> Boolean,
    private val onPreviousChapter: () -> Boolean,
    private val onProgressChanged: (Double) -> Unit,
    private val onLoadFailed: (String) -> Unit,
    private val onTap: () -> Unit = {},
    private val onTapTop: () -> Unit = {},
    private val onTapBottom: () -> Unit = {},
    private val swipeThreshold: Int = 96,
    private val tapZonePx: Int = 100,
    var isPopupActive: Boolean = false,
    private val onTextSelectedCallback: (word: String, sentence: String, x: Float, y: Float, w: Float, h: Float) -> Unit = { _, _, _, _, _, _ -> },
    private val onSentenceReadyCallback: (sentence: String) -> Unit = {},
    private val onDismissPopupRequested: () -> Unit = {},
    internal val onInternalLinkClicked: (url: String) -> Unit = {},
) : WebView(context) {

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var totalMovement = 0f

    var currentUrl: String? = null
    var pendingProgress: Double = 0.0
    var lastAppliedSettings: ReaderSettings = readerSettings

    internal var lastLoadedChapterKey: NovelReaderWebLoadPolicy.ChapterLoadKey? = null

    private var lastProgressReportTime = 0L
    private val reportProgressRunnable = Runnable {
        if (continuousMode && !isImageOnly) {
            evaluateJavascript(NovelReaderWebScriptPolicy.calculateProgressScript()) { p ->
                NovelReaderWebResultPolicy.progressResult(p)?.let {
                    pendingProgress = it
                    onProgressChanged(it)
                }
            }
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)

        if (isPopupActive) {
            onDismissPopupRequested()
        }

        when (
            val action = NovelReaderProgressPolicy.scrollProgressReportAction(
                continuousMode = continuousMode,
                imageOnly = isImageOnly,
                nowMillis = System.currentTimeMillis(),
                lastReportMillis = lastProgressReportTime,
            )
        ) {
            NovelReaderProgressPolicy.ScrollProgressReportAction.Ignore -> Unit
            is NovelReaderProgressPolicy.ScrollProgressReportAction.ReportNow -> {
                lastProgressReportTime = action.reportTimeMillis
                removeCallbacks(reportProgressRunnable)
                post(reportProgressRunnable)
            }
            is NovelReaderProgressPolicy.ScrollProgressReportAction.ScheduleDelayed -> {
                removeCallbacks(reportProgressRunnable)
                postDelayed(reportProgressRunnable, action.delayMillis)
            }
        }
    }

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent): Boolean = true

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float,
            ): Boolean {
                val start = e1 ?: return false
                val deltaX = e2.x - start.x
                val deltaY = e2.y - start.y
                val forward = NovelReaderInputPolicy.swipeForward(
                    deltaX = deltaX,
                    deltaY = deltaY,
                    velocityX = velocityX,
                    velocityY = velocityY,
                    swipeThreshold = swipeThreshold.toFloat(),
                    continuousMode = continuousMode,
                    verticalWriting = readerSettings.verticalWriting,
                ) ?: return false

                return handleSwipe(forward = forward)
            }
        },
    )

    private val jsBridge = ReaderJavascriptBridge(
        onRestoreCompleted = {
            post {
                alpha = 0f
                visibility = View.VISIBLE
                animate().cancel()
                animate().alpha(1f).setDuration(160).start()
            }
        },
        onTextSelectedCallback = { word, sentence, x, y, w, h ->
            post {
                val density = context.resources.displayMetrics.density
                val loc = IntArray(2)
                getLocationOnScreen(loc)
                val bounds = NovelReaderWebGeometryPolicy.cssBoundsToScreenBounds(
                    x = x.toDouble(),
                    y = y.toDouble(),
                    width = w.toDouble(),
                    height = h.toDouble(),
                    viewportLeft = loc[0].toDouble(),
                    viewportTop = loc[1].toDouble(),
                    scale = density.toDouble(),
                )
                onTextSelectedCallback(
                    word,
                    sentence,
                    bounds.x.toFloat(),
                    bounds.y.toFloat(),
                    bounds.width.toFloat(),
                    bounds.height.toFloat(),
                )
            }
        },
        onBackgroundTap = { x, y ->
            post {
                if (isPopupActive) {
                    onDismissPopupRequested()
                } else {
                    val density = context.resources.displayMetrics.density
                    val tapPoint = NovelReaderWebGeometryPolicy.cssPointToViewportPoint(
                        x = x.toDouble(),
                        y = y.toDouble(),
                        scale = density.toDouble(),
                    )
                    when (
                        NovelReaderInputPolicy.backgroundTapAction(
                            x = tapPoint.x.toFloat(),
                            y = tapPoint.y.toFloat(),
                            width = width,
                            height = height,
                            tapZonePx = tapZonePx,
                            tapZonePercent = readerSettings.tapZonePercent,
                            verticalWriting = readerSettings.verticalWriting,
                        )
                    ) {
                        NovelReaderInputPolicy.TapAction.TOGGLE_OVERLAY -> onTapTop()
                        NovelReaderInputPolicy.TapAction.FORWARD -> handleSwipe(forward = true)
                        NovelReaderInputPolicy.TapAction.BACKWARD -> handleSwipe(forward = false)
                        NovelReaderInputPolicy.TapAction.NONE -> Unit
                    }
                }
            }
        },
        onSentenceReadyCallback = { sentence ->
            post { onSentenceReadyCallback(sentence) }
        },
    )

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        addJavascriptInterface(jsBridge, NovelReaderWebBridgePolicy.DEFAULT_NATIVE_BRIDGE_NAME)
        setBackgroundColor(readerSettings.backgroundColor)
    }

    /** Set callback for selection rects from JS (used by Compose highlight overlay). */
    fun setSelectionRectsCallback(callback: ((String) -> Unit)?) {
        jsBridge.onSelectionRectsCallback = callback
    }

    fun loadChapter(url: String) {
        Log.d("ReaderWebView", "loadChapter: url=$url size=${width}x$height")
        currentUrl = url

        when (
            val action = NovelReaderWebLoadPolicy.chapterLoadAction(
                url = url,
                width = width,
                height = height,
                verticalWriting = readerSettings.verticalWriting,
                lastLoadedKey = lastLoadedChapterKey,
            )
        ) {
            is NovelReaderWebLoadPolicy.ChapterLoadAction.Defer -> {
                postDelayed({ loadChapter(url) }, action.delayMillis)
                return
            }
            NovelReaderWebLoadPolicy.ChapterLoadAction.SkipDuplicate -> {
                Log.d("ReaderWebView", "loadChapter skipped: duplicate")
                return
            }
            is NovelReaderWebLoadPolicy.ChapterLoadAction.Load -> {
                lastLoadedChapterKey = action.key
            }
        }

        visibility = View.INVISIBLE

        try {
            when (val target = NovelReaderWebLoadPolicy.urlLoadTarget(url)) {
                is NovelReaderWebLoadPolicy.UrlLoadTarget.DirectUrl -> loadUrl(target.url)
                is NovelReaderWebLoadPolicy.UrlLoadTarget.LocalFile -> {
                    val file = File(target.localPath)
                    if (file.exists()) {
                        loadUrl(target.url)
                    } else {
                        val message = NovelReaderWebLoadPolicy.localFileMissingMessage(target.url)
                        Log.e("ReaderWebView", message)
                        onLoadFailed(message)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ReaderWebView", "loadChapter error", e)
            onLoadFailed(e.message ?: "Failed to load chapter")
        }
    }

    fun injectReader() {
        Log.d("ReaderWebView", "injectReader: ${width}x$height continuous=$continuousMode imageOnly=$isImageOnly")

        if (height <= 0 || width <= 0) {
            post { injectReader() }
            return
        }

        val script = NovelReaderWebInjectionPolicy.readerInjectionScript(
            mode = NovelReaderWebInjectionPolicy.readerMode(
                isImageOnly = isImageOnly,
                continuousMode = continuousMode,
            ),
            readerJs = readerJs,
            settings = readerSettings,
            pendingProgress = pendingProgress,
        )
        evaluateJavascript(script, null)
    }

    fun applySettings(settings: ReaderSettings) {
        when (NovelReaderWebSettingsPolicy.liveSettingsAction(continuousMode, settings)) {
            NovelReaderWebSettingsPolicy.LiveSettingsAction.ReloadCurrentChapter -> {
                continuousMode = settings.continuousMode
                currentUrl?.let { url ->
                    evaluateJavascript(NovelReaderWebScriptPolicy.calculateProgressScript()) { p ->
                        pendingProgress = NovelReaderWebResultPolicy.progressResult(p) ?: 0.0
                        loadChapter(url)
                    }
                }
                return
            }
            NovelReaderWebSettingsPolicy.LiveSettingsAction.ApplyDomUpdates -> Unit
        }

        readerSettings = settings
        setBackgroundColor(settings.backgroundColor)
        evaluateJavascript(NovelReaderWebInjectionPolicy.liveSettingsScript(settings), null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                totalMovement = 0f
            }
            MotionEvent.ACTION_MOVE -> {
                totalMovement += NovelReaderInputPolicy.pointerMoveDistance(
                    previousX = touchStartX,
                    previousY = touchStartY,
                    currentX = event.x,
                    currentY = event.y,
                )
                touchStartX = event.x
                touchStartY = event.y
            }
            MotionEvent.ACTION_UP -> {
                if (NovelReaderInputPolicy.shouldHandleTap(totalMovement)) {
                    val cssPoint = NovelReaderWebGeometryPolicy.viewportPointToCssPoint(
                        x = event.x.toDouble(),
                        y = event.y.toDouble(),
                        scale = resources.displayMetrics.density.toDouble(),
                    )
                    evaluateJavascript(
                        NovelReaderWebScriptPolicy.handleTapScript(
                            cssPoint.x.toFloat(),
                            cssPoint.y.toFloat(),
                        ),
                        null,
                    )
                }
                performClick()
            }
        }
        val gestureHandled = gestureDetector.onTouchEvent(event)
        val webViewHandled = super.onTouchEvent(event)
        return gestureHandled || webViewHandled
    }

    override fun performClick(): Boolean = super.performClick()

    fun paginate(forward: Boolean) {
        navigate(
            direction = NovelReaderWebNavigationPolicy.pageDirection(forward),
            fallback = chapterFallback(forward),
        )
    }

    private fun handleSwipe(forward: Boolean): Boolean {
        return when (
            val action = NovelReaderWebNavigationPolicy.swipeAction(
                isImageOnly = isImageOnly,
                continuousMode = continuousMode,
                forward = forward,
            )
        ) {
            is NovelReaderWebNavigationPolicy.SwipeAction.UseChapterFallback -> {
                val changed = chapterFallback(action.forward).invoke()
                if (changed) visibility = View.INVISIBLE
                true
            }
            is NovelReaderWebNavigationPolicy.SwipeAction.CheckContinuousBoundary -> {
                navigateContinuous(action.forward)
            }
            is NovelReaderWebNavigationPolicy.SwipeAction.Paginate -> {
                navigate(
                    direction = action.direction,
                    fallback = chapterFallback(action.forward),
                )
            }
        }
    }

    private fun chapterFallback(forward: Boolean): () -> Boolean {
        return if (forward) onNextChapter else onPreviousChapter
    }

    /**
     * For continuous mode: check via JS whether we are at the scroll boundary.
     * If yes, call the chapter callback; if not, let the WebView handle the scroll.
     */
    private fun navigateContinuous(forward: Boolean): Boolean {
        evaluateJavascript(NovelReaderWebScriptPolicy.continuousBoundaryScript(forward)) { result ->
            when (NovelReaderWebResultPolicy.continuousBoundaryAction(result)) {
                NovelReaderWebResultPolicy.ContinuousBoundaryAction.UseChapterFallback -> {
                    val changed = if (forward) onNextChapter() else onPreviousChapter()
                    if (changed) visibility = View.INVISIBLE
                }
                NovelReaderWebResultPolicy.ContinuousBoundaryAction.LetWebViewScroll -> Unit
            }
        }
        return true
    }

    private fun navigate(
        direction: NovelReaderWebScriptPolicy.PageDirection,
        fallback: () -> Boolean,
    ): Boolean {
        evaluateJavascript(NovelReaderWebScriptPolicy.paginateScript(direction)) { result ->
            when (NovelReaderWebResultPolicy.pagedNavigationAction(result)) {
                NovelReaderWebResultPolicy.PagedNavigationAction.ReportProgress -> {
                    evaluateJavascript(
                        NovelReaderWebScriptPolicy.calculateProgressScript(),
                    ) { progressResult ->
                        NovelReaderWebResultPolicy.progressResult(progressResult)?.let {
                            pendingProgress = it
                            onProgressChanged(it)
                        }
                    }
                }
                NovelReaderWebResultPolicy.PagedNavigationAction.UseChapterFallback -> {
                    val chapterChanged = fallback()
                    if (chapterChanged) {
                        visibility = View.INVISIBLE
                    }
                }
            }
        }
        return true
    }
}

private class ReaderJavascriptBridge(
    private val onRestoreCompleted: () -> Unit,
    private val onTextSelectedCallback: (word: String, sentence: String, x: Float, y: Float, w: Float, h: Float) -> Unit = { _, _, _, _, _, _ -> },
    private val onBackgroundTap: (x: Float, y: Float) -> Unit = { _, _ -> },
    private val onSentenceReadyCallback: (sentence: String) -> Unit = {},
) {
    /** Callback for selection rects from JS. Set by the hosting Activity. */
    var onSelectionRectsCallback: ((String) -> Unit)? = null

    @JavascriptInterface
    fun restoreCompleted() {
        onRestoreCompleted()
    }

    @JavascriptInterface
    fun onTextSelected(word: String, sentence: String, x: Float, y: Float, w: Float, h: Float) {
        if (word.isNotBlank()) onTextSelectedCallback.invoke(word, sentence, x, y, w, h)
    }

    @JavascriptInterface
    fun onBackgroundTap(x: Float, y: Float) {
        onBackgroundTap.invoke(x, y)
    }

    @JavascriptInterface
    fun onSentenceReady(sentence: String) {
        onSentenceReadyCallback.invoke(sentence)
    }

    @JavascriptInterface
    fun onSelectionRects(json: String) {
        onSelectionRectsCallback?.invoke(json)
    }
}

private fun loadAssetText(context: Context, path: String): String {
    return context.assets.open(path).use { input ->
        BufferedReader(input.reader()).readText()
    }
}

private fun NovelReaderWebHostPolicy.HostSettings.cacheMode(): Int {
    return when (cachePolicy) {
        NovelReaderWebHostPolicy.CachePolicy.NO_CACHE -> WebSettings.LOAD_NO_CACHE
    }
}

private fun NovelReaderWebHostPolicy.HostSettings.mixedContentMode(): Int {
    return if (allowMixedContent) {
        WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    } else {
        WebSettings.MIXED_CONTENT_NEVER_ALLOW
    }
}

private fun NovelReaderWebHostPolicy.HostSettings.overScrollMode(): Int {
    return when (overScrollPolicy) {
        NovelReaderWebHostPolicy.OverScrollPolicy.NEVER -> WebView.OVER_SCROLL_NEVER
    }
}
