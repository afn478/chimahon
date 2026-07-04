package tachiyomi.domain.reader.service

object NovelReaderWebBridgePolicy {
    const val DEFAULT_NATIVE_BRIDGE_NAME = "HoshiAndroid"
    const val DEFAULT_READER_BRIDGE_NAME = "hoshiNative"

    sealed interface BackgroundTapAction {
        data object DismissPopup : BackgroundTapAction
        data object ToggleOverlay : BackgroundTapAction
        data class Navigate(val forward: Boolean) : BackgroundTapAction
        data object Ignore : BackgroundTapAction
    }

    fun backgroundTapAction(
        clientX: Double,
        clientY: Double,
        popupActive: Boolean,
        viewportWidth: Int,
        viewportHeight: Int,
        scale: Double,
        tapZonePx: Int,
        tapZonePercent: Int,
        verticalWriting: Boolean,
    ): BackgroundTapAction {
        if (popupActive) return BackgroundTapAction.DismissPopup

        val tapPoint = NovelReaderWebGeometryPolicy.cssPointToViewportPoint(
            x = clientX,
            y = clientY,
            scale = scale,
        )

        return when (
            NovelReaderInputPolicy.backgroundTapAction(
                x = tapPoint.x.toFloat(),
                y = tapPoint.y.toFloat(),
                width = viewportWidth,
                height = viewportHeight,
                tapZonePx = tapZonePx,
                tapZonePercent = tapZonePercent,
                verticalWriting = verticalWriting,
            )
        ) {
            NovelReaderInputPolicy.TapAction.TOGGLE_OVERLAY -> BackgroundTapAction.ToggleOverlay
            NovelReaderInputPolicy.TapAction.FORWARD -> BackgroundTapAction.Navigate(forward = true)
            NovelReaderInputPolicy.TapAction.BACKWARD -> BackgroundTapAction.Navigate(forward = false)
            NovelReaderInputPolicy.TapAction.NONE -> BackgroundTapAction.Ignore
        }
    }

    fun nativeCallbackBridgeScript(
        nativeBridgeName: String = DEFAULT_NATIVE_BRIDGE_NAME,
        readerBridgeName: String = DEFAULT_READER_BRIDGE_NAME,
    ): String {
        val nativeBridgeLiteral = NovelReaderWebScriptPolicy.jsStringLiteral(nativeBridgeName)
        val readerBridgeLiteral = NovelReaderWebScriptPolicy.jsStringLiteral(readerBridgeName)

        return """
            (function() {
                var readerBridge = window[$readerBridgeLiteral] = window[$readerBridgeLiteral] || {};
                readerBridge.restoreCompleted = function() {
                    var nativeBridge = window[$nativeBridgeLiteral];
                    if (nativeBridge && nativeBridge.restoreCompleted) {
                        nativeBridge.restoreCompleted();
                        return;
                    }
                    if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.restoreCompleted) {
                        window.webkit.messageHandlers.restoreCompleted.postMessage(null);
                    }
                };
                readerBridge.onBackgroundTap = function(clientX, clientY) {
                    var nativeBridge = window[$nativeBridgeLiteral];
                    if (nativeBridge && nativeBridge.onBackgroundTap) {
                        nativeBridge.onBackgroundTap(clientX, clientY);
                    }
                };
                readerBridge.onTextSelected = function(word, sentence, x, y, width, height) {
                    var nativeBridge = window[$nativeBridgeLiteral];
                    if (nativeBridge && nativeBridge.onTextSelected) {
                        nativeBridge.onTextSelected(word, sentence, x, y, width, height);
                    }
                };
            })();
        """.trimIndent()
    }

    fun restoreCompletedCallScript(
        readerBridgeName: String = DEFAULT_READER_BRIDGE_NAME,
    ): String {
        val readerBridgeLiteral = NovelReaderWebScriptPolicy.jsStringLiteral(readerBridgeName)

        return """
            (function() {
                var readerBridge = window[$readerBridgeLiteral];
                if (readerBridge && readerBridge.restoreCompleted) readerBridge.restoreCompleted();
            })();
        """.trimIndent()
    }

    fun backgroundTapCallScript(
        clientXVar: String = "clientX",
        clientYVar: String = "clientY",
        readerBridgeName: String = DEFAULT_READER_BRIDGE_NAME,
    ): String {
        val readerBridgeLiteral = NovelReaderWebScriptPolicy.jsStringLiteral(readerBridgeName)

        return """
            var readerBridge = window[$readerBridgeLiteral];
            if (readerBridge && readerBridge.onBackgroundTap) {
                readerBridge.onBackgroundTap($clientXVar, $clientYVar);
            }
        """.trimIndent()
    }

    fun textSelectedCallScript(
        wordVar: String = "word",
        sentenceVar: String = "sentence",
        xVar: String = "x",
        yVar: String = "y",
        widthVar: String = "width",
        heightVar: String = "height",
        readerBridgeName: String = DEFAULT_READER_BRIDGE_NAME,
    ): String {
        val readerBridgeLiteral = NovelReaderWebScriptPolicy.jsStringLiteral(readerBridgeName)

        return """
            var readerBridge = window[$readerBridgeLiteral];
            if (readerBridge && readerBridge.onTextSelected) {
                readerBridge.onTextSelected($wordVar, $sentenceVar, $xVar, $yVar, $widthVar, $heightVar);
            }
        """.trimIndent()
    }
}
