package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertTrue

class NovelReaderWebDocumentPolicyTest {
    @Test
    fun viewportMetaScriptReplacesExistingViewport() {
        val script = NovelReaderWebDocumentPolicy.viewportMetaScript()

        assertTrue(script.contains("document.querySelector('meta[name=\"viewport\"]')"))
        assertTrue(script.contains("if (vp) vp.remove();"))
        assertTrue(script.contains("nvp.name = 'viewport';"))
        assertTrue(script.contains("nvp.content = '${NovelReaderWebDocumentPolicy.DEFAULT_VIEWPORT_CONTENT}';"))
        assertTrue(script.contains("document.head.appendChild(nvp);"))
    }

    @Test
    fun viewportMetaScriptEscapesCustomViewportContent() {
        val script = NovelReaderWebDocumentPolicy.viewportMetaScript(
            viewportContent = "width=device-width, user-scalable='no'",
        )

        assertTrue(script.contains("nvp.content = 'width=device-width, user-scalable=\\'no\\'';"))
    }

    @Test
    fun restoreCompletedCallScriptUsesConfigurableNativeBridgeName() {
        val script = NovelReaderWebDocumentPolicy.restoreCompletedCallScript("Native'Bridge")

        assertTrue(script.contains("var nativeBridge = window['Native\\'Bridge'];"))
        assertTrue(script.contains("if (nativeBridge && nativeBridge.restoreCompleted) nativeBridge.restoreCompleted();"))
    }

    @Test
    fun nativeRestoreBridgeScriptRegistersWebkitRestoreHandler() {
        val script = NovelReaderWebDocumentPolicy.nativeRestoreBridgeScript("DesktopBridge")

        assertTrue(script.contains("window.webkit = window.webkit || {};"))
        assertTrue(script.contains("window.webkit.messageHandlers = window.webkit.messageHandlers || {};"))
        assertTrue(script.contains("window.webkit.messageHandlers.restoreCompleted = {"))
        assertTrue(script.contains("postMessage: function(_)"))
        assertTrue(script.contains("var nativeBridge = window['DesktopBridge'];"))
    }

    @Test
    fun contentWrapperScriptCanReturnWhenBodyIsMissing() {
        val script = NovelReaderWebDocumentPolicy.contentWrapperScript(
            missingBodyAction = NovelReaderWebDocumentPolicy.MissingBodyAction.RETURN,
        )

        assertTrue(script.contains("var b = document.body;"))
        assertTrue(script.contains("if (!b) return;"))
        assertTrue(!script.contains("notifyRestoreComplete"))
        assertCommonWrapperScript(script)
    }

    @Test
    fun contentWrapperScriptCanNotifyRestoreWhenBodyIsMissing() {
        val script = NovelReaderWebDocumentPolicy.contentWrapperScript(
            missingBodyAction = NovelReaderWebDocumentPolicy.MissingBodyAction.NOTIFY_RESTORE_COMPLETE,
        )

        assertTrue(script.contains("if (!b) { window.hoshiReader.notifyRestoreComplete(); return; }"))
        assertCommonWrapperScript(script)
    }

    @Test
    fun contentWrapperScriptUsesCustomVariableNames() {
        val script = NovelReaderWebDocumentPolicy.contentWrapperScript(
            bodyVar = "bodyEl",
            wrapperVar = "contentEl",
            missingBodyAction = NovelReaderWebDocumentPolicy.MissingBodyAction.RETURN,
        )

        assertTrue(script.contains("var bodyEl = document.body;"))
        assertTrue(script.contains("if (!bodyEl) return;"))
        assertTrue(script.contains("var contentEl = document.getElementById('${NovelEpubContentPolicy.CONTENT_WRAPPER_ID}');"))
        assertTrue(script.contains("while (bodyEl.firstChild) contentEl.appendChild(bodyEl.firstChild);"))
        assertTrue(script.contains("bodyEl.appendChild(contentEl);"))
    }

    private fun assertCommonWrapperScript(script: String) {
        assertTrue(script.contains("document.getElementById('${NovelEpubContentPolicy.CONTENT_WRAPPER_ID}')"))
        assertTrue(script.contains("wrapper = document.createElement('div');"))
        assertTrue(script.contains("wrapper.id = '${NovelEpubContentPolicy.CONTENT_WRAPPER_ID}';"))
        assertTrue(script.contains("while (b.firstChild) wrapper.appendChild(b.firstChild);"))
        assertTrue(script.contains("b.appendChild(wrapper);"))
    }
}
