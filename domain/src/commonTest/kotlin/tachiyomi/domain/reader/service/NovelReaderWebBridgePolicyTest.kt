package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertTrue

class NovelReaderWebBridgePolicyTest {
    @Test
    fun nativeCallbackBridgeScriptInstallsReaderBridgeCallbacks() {
        val script = NovelReaderWebBridgePolicy.nativeCallbackBridgeScript(
            nativeBridgeName = "Native'Bridge",
            readerBridgeName = "reader-bridge",
        )

        assertTrue(script.contains("var readerBridge = window['reader-bridge'] = window['reader-bridge'] || {};"))
        assertTrue(script.contains("var nativeBridge = window['Native\\'Bridge'];"))
        assertTrue(script.contains("readerBridge.restoreCompleted = function()"))
        assertTrue(script.contains("nativeBridge.restoreCompleted();"))
        assertTrue(script.contains("window.webkit.messageHandlers.restoreCompleted.postMessage(null);"))
        assertTrue(script.contains("readerBridge.onBackgroundTap = function(clientX, clientY)"))
        assertTrue(script.contains("nativeBridge.onBackgroundTap(clientX, clientY);"))
        assertTrue(script.contains("readerBridge.onTextSelected = function(word, sentence, x, y, width, height)"))
        assertTrue(script.contains("nativeBridge.onTextSelected(word, sentence, x, y, width, height);"))
    }

    @Test
    fun restoreCompletedCallScriptTargetsConfiguredReaderBridge() {
        val script = NovelReaderWebBridgePolicy.restoreCompletedCallScript("reader'bridge")

        assertTrue(script.contains("var readerBridge = window['reader\\'bridge'];"))
        assertTrue(script.contains("if (readerBridge && readerBridge.restoreCompleted) readerBridge.restoreCompleted();"))
    }

    @Test
    fun backgroundTapCallScriptUsesConfiguredVariables() {
        val script = NovelReaderWebBridgePolicy.backgroundTapCallScript(
            clientXVar = "x",
            clientYVar = "y",
            readerBridgeName = "readerBridge",
        )

        assertTrue(script.contains("var readerBridge = window['readerBridge'];"))
        assertTrue(script.contains("readerBridge.onBackgroundTap(x, y);"))
    }

    @Test
    fun textSelectedCallScriptUsesConfiguredVariables() {
        val script = NovelReaderWebBridgePolicy.textSelectedCallScript(
            wordVar = "selectedWord",
            sentenceVar = "selectedSentence",
            xVar = "minX",
            yVar = "minY",
            widthVar = "popupWidth",
            heightVar = "popupHeight",
            readerBridgeName = "readerBridge",
        )

        assertTrue(script.contains("var readerBridge = window['readerBridge'];"))
        assertTrue(
            script.contains(
                "readerBridge.onTextSelected(selectedWord, selectedSentence, minX, minY, popupWidth, popupHeight);",
            ),
        )
    }
}
