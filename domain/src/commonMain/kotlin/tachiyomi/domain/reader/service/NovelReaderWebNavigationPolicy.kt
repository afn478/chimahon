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
}
