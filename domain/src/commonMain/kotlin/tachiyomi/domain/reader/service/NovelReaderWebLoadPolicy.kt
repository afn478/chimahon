package tachiyomi.domain.reader.service

object NovelReaderWebLoadPolicy {
    const val CHAPTER_LOAD_DEFER_DELAY_MS = 100L
    const val DEFAULT_CHAPTER_LOAD_FAILURE_MESSAGE = "Failed to load chapter"

    data class ChapterLoadKey(
        val url: String,
        val width: Int,
        val height: Int,
        val verticalWriting: Boolean,
    )

    sealed interface UrlLoadTarget {
        data class DirectUrl(val url: String) : UrlLoadTarget
        data class LocalFile(val url: String, val localPath: String) : UrlLoadTarget
    }

    sealed interface ChapterLoadAction {
        data class Defer(val delayMillis: Long = CHAPTER_LOAD_DEFER_DELAY_MS) : ChapterLoadAction
        data object SkipDuplicate : ChapterLoadAction
        data class Load(val key: ChapterLoadKey) : ChapterLoadAction
    }

    sealed interface ReceivedErrorAction {
        data object Ignore : ReceivedErrorAction
        data class ReportFailure(val message: String) : ReceivedErrorAction
    }

    data class RendererGoneFailure(
        val reason: String,
        val message: String,
    )

    fun urlLoadTarget(url: String): UrlLoadTarget {
        return if (NovelReaderFileUrlPolicy.isLocalFileUrlOrPath(url)) {
            UrlLoadTarget.LocalFile(
                url = url,
                localPath = NovelReaderFileUrlPolicy.localPathForFileUrlOrPath(url),
            )
        } else {
            UrlLoadTarget.DirectUrl(url)
        }
    }

    fun chapterLoadAction(
        url: String,
        width: Int,
        height: Int,
        verticalWriting: Boolean,
        lastLoadedKey: ChapterLoadKey?,
    ): ChapterLoadAction {
        if (width <= 0 || height <= 0) return ChapterLoadAction.Defer()

        val nextKey = ChapterLoadKey(
            url = url,
            width = width,
            height = height,
            verticalWriting = verticalWriting,
        )

        return if (nextKey == lastLoadedKey) {
            ChapterLoadAction.SkipDuplicate
        } else {
            ChapterLoadAction.Load(nextKey)
        }
    }

    fun localFileMissingMessage(url: String): String {
        return "File not found: $url"
    }

    fun loadExceptionFailureMessage(message: String?): String {
        return message?.takeIf { it.isNotBlank() } ?: DEFAULT_CHAPTER_LOAD_FAILURE_MESSAGE
    }

    fun receivedErrorAction(
        isMainFrame: Boolean,
        description: String?,
    ): ReceivedErrorAction {
        return if (isMainFrame) {
            ReceivedErrorAction.ReportFailure(loadExceptionFailureMessage(description))
        } else {
            ReceivedErrorAction.Ignore
        }
    }

    fun rendererGoneFailure(crashed: Boolean): RendererGoneFailure {
        val reason = if (crashed) {
            "WebView crashed"
        } else {
            "WebView killed by system (OOM)"
        }

        return RendererGoneFailure(
            reason = reason,
            message = "Renderer died ($reason). Try disabling hardware acceleration or 'Avoid page breaks'.",
        )
    }
}
