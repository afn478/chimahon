package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.ReaderSettings

object NovelReaderWebInjectionPolicy {
    enum class ReaderMode {
        IMAGE_ONLY,
        CONTINUOUS,
        PAGED,
    }

    sealed interface ReaderInjectionAction {
        data object Defer : ReaderInjectionAction
        data class Inject(val script: String) : ReaderInjectionAction
    }

    fun readerMode(
        isImageOnly: Boolean,
        continuousMode: Boolean,
    ): ReaderMode {
        return when {
            isImageOnly -> ReaderMode.IMAGE_ONLY
            continuousMode -> ReaderMode.CONTINUOUS
            else -> ReaderMode.PAGED
        }
    }

    fun readerInjectionAction(
        width: Int,
        height: Int,
        isImageOnly: Boolean,
        continuousMode: Boolean,
        readerJs: String,
        settings: ReaderSettings,
        pendingProgress: Double,
    ): ReaderInjectionAction {
        if (width <= 0 || height <= 0) return ReaderInjectionAction.Defer

        return ReaderInjectionAction.Inject(
            readerInjectionScript(
                mode = readerMode(
                    isImageOnly = isImageOnly,
                    continuousMode = continuousMode,
                ),
                readerJs = readerJs,
                settings = settings,
                pendingProgress = pendingProgress,
            ),
        )
    }

    fun readerInjectionScript(
        mode: ReaderMode,
        readerJs: String,
        settings: ReaderSettings,
        pendingProgress: Double,
    ): String {
        return when (mode) {
            ReaderMode.IMAGE_ONLY -> imageOnlyScript(settings)
            ReaderMode.CONTINUOUS -> continuousScript(
                readerJs = readerJs,
                settings = settings,
                pendingProgress = pendingProgress,
            )
            ReaderMode.PAGED -> pagedScript(
                readerJs = readerJs,
                settings = settings,
                pendingProgress = pendingProgress,
            )
        }
    }

    fun liveSettingsScript(settings: ReaderSettings): String {
        val backgroundHex = NovelReaderWebStylePolicy.backgroundHex(settings)
        val textHex = NovelReaderWebStylePolicy.textHex(settings)
        val readerAppearanceScript = NovelReaderWebStylePolicy.readerAppearanceScript(
            settings = settings,
            backgroundHex = backgroundHex,
            textHex = textHex,
            includeBodyFontSize = true,
        )
        val contentPaddingScript = NovelReaderWebLayoutPolicy.contentPaddingScript(
            settings = settings,
            declareViewportMetrics = true,
        )
        val contentWrapperScript = NovelReaderWebDocumentPolicy.contentWrapperScript(
            missingBodyAction = NovelReaderWebDocumentPolicy.MissingBodyAction.RETURN,
        )

        return """
            (function() {
                $contentWrapperScript

                $contentPaddingScript

                $readerAppearanceScript
            })();
        """.trimIndent()
    }

    private fun imageOnlyScript(settings: ReaderSettings): String {
        val bg = NovelReaderWebStylePolicy.backgroundHex(settings)
        val nativeCallbackBridgeScript = NovelReaderWebBridgePolicy.nativeCallbackBridgeScript()
        val viewportScript = NovelReaderWebDocumentPolicy.viewportMetaScript()
        val restoreCompletedScript = NovelReaderWebBridgePolicy.restoreCompletedCallScript()
        val imageOnlyReaderApiScript = NovelReaderWebScriptPolicy.imageOnlyReaderApiScript(
            backgroundTapCallScript = NovelReaderWebBridgePolicy.backgroundTapCallScript(),
        )
        val imageOnlyDocumentLayoutScript = NovelReaderWebLayoutPolicy.imageOnlyDocumentLayoutScript(
            backgroundHex = bg,
            restoreCompletedScript = restoreCompletedScript,
        )

        return """
            (function() {
                $nativeCallbackBridgeScript

                $viewportScript

                $imageOnlyReaderApiScript

                $imageOnlyDocumentLayoutScript
            })();
        """.trimIndent()
    }

    private fun continuousScript(
        readerJs: String,
        settings: ReaderSettings,
        pendingProgress: Double,
    ): String {
        val verticalWriting = settings.verticalWriting
        val css = NovelReaderWebStylePolicy.baseCss(settings)
        val bg = NovelReaderWebStylePolicy.backgroundHex(settings)
        val textColor = NovelReaderWebStylePolicy.textHex(settings)
        val baseStyleScript = NovelReaderWebStylePolicy.baseStyleElementScript(css)
        val readerAppearanceScript = NovelReaderWebStylePolicy.readerAppearanceScript(
            settings = settings,
            backgroundHex = bg,
            textHex = textColor,
        )
        val continuousBodyLayoutScript = NovelReaderWebLayoutPolicy.continuousBodyLayoutScript(
            verticalWriting = verticalWriting,
        )
        val nativeCallbackBridgeScript = NovelReaderWebBridgePolicy.nativeCallbackBridgeScript()
        val restoreBridgeScript = NovelReaderWebDocumentPolicy.nativeRestoreBridgeScript()
        val viewportScript = NovelReaderWebDocumentPolicy.viewportMetaScript()
        val viewportMetricsScript = NovelReaderWebLayoutPolicy.viewportMetricsScript(
            settings = settings,
            imageHeightMode = NovelReaderWebLayoutPolicy.ImageHeightMode.FULL_VIEWPORT,
        )
        val contentPaddingScript = NovelReaderWebLayoutPolicy.contentPaddingScript(
            settings = settings,
            declareViewportMetrics = false,
        )
        val mediaStyleScript = NovelReaderWebMediaPolicy.blockMediaStyleScript(
            NovelReaderWebMediaPolicy.MediaMode.CONTINUOUS,
        )
        val classifyMediaAndRestoreProgressScript = NovelReaderWebMediaPolicy.classifyMediaAndRestoreProgressScript(
            pendingProgress = pendingProgress,
            verticalWriting = verticalWriting,
        )
        val contentWrapperScript = NovelReaderWebDocumentPolicy.contentWrapperScript(
            missingBodyAction = NovelReaderWebDocumentPolicy.MissingBodyAction.NOTIFY_RESTORE_COMPLETE,
        )

        return """
            (function() {
                $restoreBridgeScript

                $nativeCallbackBridgeScript

                $viewportScript

                $viewportMetricsScript

                $baseStyleScript

                $mediaStyleScript

                $readerJs

                $contentWrapperScript

                $contentPaddingScript

                $readerAppearanceScript

                $continuousBodyLayoutScript

                window.hoshiReader.registerCopyText();
                window.hoshiReader.continuousMode = true;

                $classifyMediaAndRestoreProgressScript
            })();
        """.trimIndent()
    }

    private fun pagedScript(
        readerJs: String,
        settings: ReaderSettings,
        pendingProgress: Double,
    ): String {
        val verticalWriting = settings.verticalWriting
        val css = NovelReaderWebStylePolicy.baseCss(settings)
        val bg = NovelReaderWebStylePolicy.backgroundHex(settings)
        val textColor = NovelReaderWebStylePolicy.textHex(settings)
        val baseStyleScript = NovelReaderWebStylePolicy.baseStyleElementScript(css)
        val readerAppearanceScript = NovelReaderWebStylePolicy.readerAppearanceScript(
            settings = settings,
            backgroundHex = bg,
            textHex = textColor,
        )
        val pagedOverrideStyleScript = NovelReaderWebLayoutPolicy.pagedOverrideStyleScript()
        val pagedBodyLayoutScript = NovelReaderWebLayoutPolicy.pagedBodyLayoutScript(
            verticalWriting = verticalWriting,
        )
        val nativeCallbackBridgeScript = NovelReaderWebBridgePolicy.nativeCallbackBridgeScript()
        val restoreBridgeScript = NovelReaderWebDocumentPolicy.nativeRestoreBridgeScript()
        val viewportScript = NovelReaderWebDocumentPolicy.viewportMetaScript()
        val viewportMetricsScript = NovelReaderWebLayoutPolicy.viewportMetricsScript(
            settings = settings,
            imageHeightMode = NovelReaderWebLayoutPolicy.ImageHeightMode.PAGED_WITH_VERTICAL_OVERLAP,
        )
        val contentPaddingScript = NovelReaderWebLayoutPolicy.contentPaddingScript(
            settings = settings,
            declareViewportMetrics = false,
        )
        val mediaStyleScript = NovelReaderWebMediaPolicy.blockMediaStyleScript(
            NovelReaderWebMediaPolicy.MediaMode.PAGED,
        )
        val avoidPageBreakStyleScript = NovelReaderWebMediaPolicy.avoidPageBreakStyleScript(
            settings.avoidPageBreak,
        )
        val classifyMediaAndRestoreProgressScript = NovelReaderWebMediaPolicy.classifyMediaAndRestoreProgressScript(
            pendingProgress = pendingProgress,
            verticalWriting = verticalWriting,
        )
        val contentWrapperScript = NovelReaderWebDocumentPolicy.contentWrapperScript(
            missingBodyAction = NovelReaderWebDocumentPolicy.MissingBodyAction.NOTIFY_RESTORE_COMPLETE,
        )

        return """
            (function() {
                $restoreBridgeScript

                $nativeCallbackBridgeScript

                $viewportScript

                $viewportMetricsScript

                $baseStyleScript

                $mediaStyleScript

                $readerJs

                $contentWrapperScript

                $contentPaddingScript

                $readerAppearanceScript

                $avoidPageBreakStyleScript

                $pagedOverrideStyleScript

                $pagedBodyLayoutScript

                window.hoshiReader.registerCopyText();

                $classifyMediaAndRestoreProgressScript
            })();
        """.trimIndent()
    }
}
