package tachiyomi.domain.reader.service

object NovelReaderWebBridgePolicy {
    const val DEFAULT_NATIVE_BRIDGE_NAME = "HoshiAndroid"
    const val DEFAULT_READER_BRIDGE_NAME = "hoshiNative"

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
