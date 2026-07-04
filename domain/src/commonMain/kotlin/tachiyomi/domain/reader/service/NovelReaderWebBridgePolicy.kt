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

    sealed interface TextSelectionAction {
        data object Ignore : TextSelectionAction
        data class ShowSelection(
            val word: String,
            val sentence: String,
            val bounds: NovelReaderWebGeometryPolicy.Bounds,
        ) : TextSelectionAction
    }

    sealed interface TouchTapAction {
        data object Ignore : TouchTapAction
        data class EvaluateScript(val script: String) : TouchTapAction
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

    fun textSelectionAction(
        word: String,
        sentence: String,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        viewportLeft: Double,
        viewportTop: Double,
        scale: Double,
    ): TextSelectionAction {
        if (word.isBlank()) return TextSelectionAction.Ignore

        return TextSelectionAction.ShowSelection(
            word = word,
            sentence = sentence,
            bounds = NovelReaderWebGeometryPolicy.cssBoundsToScreenBounds(
                x = x,
                y = y,
                width = width,
                height = height,
                viewportLeft = viewportLeft,
                viewportTop = viewportTop,
                scale = scale,
            ),
        )
    }

    fun touchTapAction(
        viewportX: Double,
        viewportY: Double,
        totalMovement: Float,
        scale: Double,
        tapMovementThreshold: Float = NovelReaderInputPolicy.MAX_TAP_MOVEMENT,
    ): TouchTapAction {
        if (
            !NovelReaderInputPolicy.shouldHandleTap(
                totalMovement = totalMovement,
                tapMovementThreshold = tapMovementThreshold,
            )
        ) {
            return TouchTapAction.Ignore
        }

        val cssPoint = NovelReaderWebGeometryPolicy.viewportPointToCssPoint(
            x = viewportX,
            y = viewportY,
            scale = scale,
        )

        return TouchTapAction.EvaluateScript(
            NovelReaderWebScriptPolicy.handleTapScript(
                cssX = cssPoint.x.toFloat(),
                cssY = cssPoint.y.toFloat(),
            ),
        )
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
