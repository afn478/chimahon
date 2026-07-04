package tachiyomi.domain.reader.service

object NovelReaderWebNavigationPolicy {
    sealed interface SwipeAction {
        data class UseChapterFallback(val forward: Boolean) : SwipeAction
        data class CheckContinuousBoundary(val forward: Boolean) : SwipeAction
        data class Paginate(
            val forward: Boolean,
            val direction: NovelReaderWebScriptPolicy.PageDirection,
        ) : SwipeAction
    }

    sealed interface ContinuousBoundaryResultAction {
        data class UseChapterFallback(val forward: Boolean) : ContinuousBoundaryResultAction
        data object LetHostScroll : ContinuousBoundaryResultAction
    }

    sealed interface PagedNavigationResultAction {
        data class ReportProgress(val progressScript: String) : PagedNavigationResultAction
        data class UseChapterFallback(val forward: Boolean) : PagedNavigationResultAction
    }

    fun pageDirection(forward: Boolean): NovelReaderWebScriptPolicy.PageDirection {
        return if (forward) {
            NovelReaderWebScriptPolicy.PageDirection.FORWARD
        } else {
            NovelReaderWebScriptPolicy.PageDirection.BACKWARD
        }
    }

    fun swipeAction(
        isImageOnly: Boolean,
        continuousMode: Boolean,
        forward: Boolean,
    ): SwipeAction {
        return when {
            isImageOnly -> SwipeAction.UseChapterFallback(forward)
            continuousMode -> SwipeAction.CheckContinuousBoundary(forward)
            else -> SwipeAction.Paginate(
                forward = forward,
                direction = pageDirection(forward),
            )
        }
    }

    fun continuousBoundaryResultAction(
        result: String?,
        forward: Boolean,
    ): ContinuousBoundaryResultAction {
        return when (NovelReaderWebResultPolicy.continuousBoundaryAction(result)) {
            NovelReaderWebResultPolicy.ContinuousBoundaryAction.UseChapterFallback -> {
                ContinuousBoundaryResultAction.UseChapterFallback(forward)
            }
            NovelReaderWebResultPolicy.ContinuousBoundaryAction.LetWebViewScroll -> {
                ContinuousBoundaryResultAction.LetHostScroll
            }
        }
    }

    fun pagedNavigationResultAction(
        result: String?,
        forward: Boolean,
    ): PagedNavigationResultAction {
        return when (NovelReaderWebResultPolicy.pagedNavigationAction(result)) {
            NovelReaderWebResultPolicy.PagedNavigationAction.ReportProgress -> {
                PagedNavigationResultAction.ReportProgress(
                    NovelReaderWebScriptPolicy.calculateProgressScript(),
                )
            }
            NovelReaderWebResultPolicy.PagedNavigationAction.UseChapterFallback -> {
                PagedNavigationResultAction.UseChapterFallback(forward)
            }
        }
    }
}
