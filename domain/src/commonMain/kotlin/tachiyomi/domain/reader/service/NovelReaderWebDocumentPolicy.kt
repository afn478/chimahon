package tachiyomi.domain.reader.service

object NovelReaderWebDocumentPolicy {
    const val DEFAULT_NATIVE_BRIDGE_NAME = NovelReaderWebBridgePolicy.DEFAULT_NATIVE_BRIDGE_NAME
    const val DEFAULT_VIEWPORT_CONTENT = "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"

    enum class MissingBodyAction {
        RETURN,
        NOTIFY_RESTORE_COMPLETE,
    }

    fun viewportMetaScript(
        viewportContent: String = DEFAULT_VIEWPORT_CONTENT,
    ): String {
        return """
            var vp = document.querySelector('meta[name="viewport"]');
            if (vp) vp.remove();
            var nvp = document.createElement('meta');
            nvp.name = 'viewport';
            nvp.content = ${NovelReaderWebScriptPolicy.jsStringLiteral(viewportContent)};
            document.head.appendChild(nvp);
        """.trimIndent()
    }

    fun nativeRestoreBridgeScript(
        nativeBridgeName: String = DEFAULT_NATIVE_BRIDGE_NAME,
    ): String {
        return """
            window.webkit = window.webkit || {};
            window.webkit.messageHandlers = window.webkit.messageHandlers || {};
            window.webkit.messageHandlers.restoreCompleted = {
                postMessage: function(_) {
                    ${restoreCompletedCallScript(nativeBridgeName)}
                }
            };
        """.trimIndent()
    }

    fun restoreCompletedCallScript(
        nativeBridgeName: String = DEFAULT_NATIVE_BRIDGE_NAME,
    ): String {
        return """
            (function() {
                var nativeBridge = window[${NovelReaderWebScriptPolicy.jsStringLiteral(nativeBridgeName)}];
                if (nativeBridge && nativeBridge.restoreCompleted) nativeBridge.restoreCompleted();
            })();
        """.trimIndent()
    }

    fun contentWrapperScript(
        bodyVar: String = "b",
        wrapperVar: String = "wrapper",
        missingBodyAction: MissingBodyAction,
    ): String {
        val missingBodyScript = when (missingBodyAction) {
            MissingBodyAction.RETURN -> "if (!$bodyVar) return;"
            MissingBodyAction.NOTIFY_RESTORE_COMPLETE -> {
                "if (!$bodyVar) { window.hoshiReader.notifyRestoreComplete(); return; }"
            }
        }

        return """
            var $bodyVar = document.body;
            $missingBodyScript

            // Ensure content wrapper existence for consistent styling.
            var $wrapperVar = document.getElementById('${NovelEpubContentPolicy.CONTENT_WRAPPER_ID}');
            if (!$wrapperVar) {
                $wrapperVar = document.createElement('div');
                $wrapperVar.id = '${NovelEpubContentPolicy.CONTENT_WRAPPER_ID}';
                while ($bodyVar.firstChild) $wrapperVar.appendChild($bodyVar.firstChild);
                $bodyVar.appendChild($wrapperVar);
            }
        """.trimIndent()
    }
}
