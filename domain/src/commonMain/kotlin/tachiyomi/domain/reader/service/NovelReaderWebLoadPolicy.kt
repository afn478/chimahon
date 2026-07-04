package tachiyomi.domain.reader.service

object NovelReaderWebLoadPolicy {
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
        data object Defer : ChapterLoadAction
        data object SkipDuplicate : ChapterLoadAction
        data class Load(val key: ChapterLoadKey) : ChapterLoadAction
    }

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
        if (width <= 0 || height <= 0) return ChapterLoadAction.Defer

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
}
