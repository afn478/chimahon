package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NovelReaderWebBridgePolicyTest {
    @Test
    fun backgroundTapActionDismissesPopupBeforeReaderTapZones() {
        assertEquals(
            NovelReaderWebBridgePolicy.BackgroundTapAction.DismissPopup,
            NovelReaderWebBridgePolicy.backgroundTapAction(
                clientX = 2.0,
                clientY = 120.0,
                popupActive = true,
                viewportWidth = 400,
                viewportHeight = 800,
                scale = 1.0,
                tapZonePx = 96,
                tapZonePercent = 20,
                verticalWriting = true,
            ),
        )
    }

    @Test
    fun backgroundTapActionScalesCssPointBeforeMappingReaderZones() {
        assertEquals(
            NovelReaderWebBridgePolicy.BackgroundTapAction.Navigate(forward = true),
            NovelReaderWebBridgePolicy.backgroundTapAction(
                clientX = 15.0,
                clientY = 100.0,
                popupActive = false,
                viewportWidth = 400,
                viewportHeight = 800,
                scale = 2.0,
                tapZonePx = 96,
                tapZonePercent = 20,
                verticalWriting = true,
            ),
        )
        assertEquals(
            NovelReaderWebBridgePolicy.BackgroundTapAction.ToggleOverlay,
            NovelReaderWebBridgePolicy.backgroundTapAction(
                clientX = 200.0,
                clientY = 40.0,
                popupActive = false,
                viewportWidth = 400,
                viewportHeight = 800,
                scale = 2.0,
                tapZonePx = 96,
                tapZonePercent = 20,
                verticalWriting = true,
            ),
        )
    }

    @Test
    fun backgroundTapActionIgnoresCenterAndInvalidViewportTaps() {
        assertEquals(
            NovelReaderWebBridgePolicy.BackgroundTapAction.Ignore,
            NovelReaderWebBridgePolicy.backgroundTapAction(
                clientX = 200.0,
                clientY = 200.0,
                popupActive = false,
                viewportWidth = 400,
                viewportHeight = 800,
                scale = 1.0,
                tapZonePx = 96,
                tapZonePercent = 20,
                verticalWriting = false,
            ),
        )
        assertEquals(
            NovelReaderWebBridgePolicy.BackgroundTapAction.Ignore,
            NovelReaderWebBridgePolicy.backgroundTapAction(
                clientX = 2.0,
                clientY = 120.0,
                popupActive = false,
                viewportWidth = 0,
                viewportHeight = 800,
                scale = 1.0,
                tapZonePx = 96,
                tapZonePercent = 20,
                verticalWriting = true,
            ),
        )
    }

    @Test
    fun textSelectionActionIgnoresBlankWords() {
        assertEquals(
            NovelReaderWebBridgePolicy.TextSelectionAction.Ignore,
            NovelReaderWebBridgePolicy.textSelectionAction(
                word = "   ",
                sentence = "Ignored sentence",
                x = 10.0,
                y = 20.0,
                width = 30.0,
                height = 40.0,
                viewportLeft = 100.0,
                viewportTop = 200.0,
                scale = 2.0,
            ),
        )
    }

    @Test
    fun textSelectionActionMapsCssBoundsToScreenBounds() {
        assertEquals(
            NovelReaderWebBridgePolicy.TextSelectionAction.ShowSelection(
                word = "word",
                sentence = "Selected word in sentence",
                bounds = NovelReaderWebGeometryPolicy.Bounds(
                    x = 120.0,
                    y = 240.0,
                    width = 60.0,
                    height = 80.0,
                ),
            ),
            NovelReaderWebBridgePolicy.textSelectionAction(
                word = "word",
                sentence = "Selected word in sentence",
                x = 10.0,
                y = 20.0,
                width = 30.0,
                height = 40.0,
                viewportLeft = 100.0,
                viewportTop = 200.0,
                scale = 2.0,
            ),
        )
    }

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
