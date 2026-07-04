package tachiyomi.domain.reader.service

object NovelReaderWebResultPolicy {
    sealed interface ContinuousBoundaryAction {
        data object UseChapterFallback : ContinuousBoundaryAction
        data object LetWebViewScroll : ContinuousBoundaryAction
    }

    sealed interface PagedNavigationAction {
        data object ReportProgress : PagedNavigationAction
        data object UseChapterFallback : PagedNavigationAction
    }

    fun progressResult(result: String?): Double? {
        return decodedResult(result)?.toDoubleOrNull()
    }

    fun continuousBoundaryAction(result: String?): ContinuousBoundaryAction {
        return if (decodedResult(result) == "limit") {
            ContinuousBoundaryAction.UseChapterFallback
        } else {
            ContinuousBoundaryAction.LetWebViewScroll
        }
    }

    fun pagedNavigationAction(result: String?): PagedNavigationAction {
        return if (decodedResult(result) == "scrolled") {
            PagedNavigationAction.ReportProgress
        } else {
            PagedNavigationAction.UseChapterFallback
        }
    }

    private fun decodedResult(result: String?): String? {
        return result
            ?.trim()
            ?.trim('"')
    }
}
