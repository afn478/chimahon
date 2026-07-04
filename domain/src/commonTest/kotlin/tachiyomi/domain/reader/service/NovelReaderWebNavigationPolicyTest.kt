package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals

class NovelReaderWebNavigationPolicyTest {
    @Test
    fun pageDirectionMapsForwardAndBackwardNavigation() {
        assertEquals(
            NovelReaderWebScriptPolicy.PageDirection.FORWARD,
            NovelReaderWebNavigationPolicy.pageDirection(forward = true),
        )
        assertEquals(
            NovelReaderWebScriptPolicy.PageDirection.BACKWARD,
            NovelReaderWebNavigationPolicy.pageDirection(forward = false),
        )
    }

    @Test
    fun swipeActionUsesChapterFallbackForImageOnlyChapters() {
        assertEquals(
            NovelReaderWebNavigationPolicy.SwipeAction.UseChapterFallback(forward = true),
            NovelReaderWebNavigationPolicy.swipeAction(
                isImageOnly = true,
                continuousMode = false,
                forward = true,
            ),
        )
        assertEquals(
            NovelReaderWebNavigationPolicy.SwipeAction.UseChapterFallback(forward = false),
            NovelReaderWebNavigationPolicy.swipeAction(
                isImageOnly = true,
                continuousMode = true,
                forward = false,
            ),
        )
    }

    @Test
    fun swipeActionChecksBoundaryForContinuousTextChapters() {
        assertEquals(
            NovelReaderWebNavigationPolicy.SwipeAction.CheckContinuousBoundary(forward = true),
            NovelReaderWebNavigationPolicy.swipeAction(
                isImageOnly = false,
                continuousMode = true,
                forward = true,
            ),
        )
    }

    @Test
    fun swipeActionPaginatesTextChaptersByDirection() {
        assertEquals(
            NovelReaderWebNavigationPolicy.SwipeAction.Paginate(
                forward = true,
                direction = NovelReaderWebScriptPolicy.PageDirection.FORWARD,
            ),
            NovelReaderWebNavigationPolicy.swipeAction(
                isImageOnly = false,
                continuousMode = false,
                forward = true,
            ),
        )
        assertEquals(
            NovelReaderWebNavigationPolicy.SwipeAction.Paginate(
                forward = false,
                direction = NovelReaderWebScriptPolicy.PageDirection.BACKWARD,
            ),
            NovelReaderWebNavigationPolicy.swipeAction(
                isImageOnly = false,
                continuousMode = false,
                forward = false,
            ),
        )
    }

    @Test
    fun continuousBoundaryResultActionFallsBackOnlyAtBoundaryLimit() {
        assertEquals(
            NovelReaderWebNavigationPolicy.ContinuousBoundaryResultAction.UseChapterFallback(forward = true),
            NovelReaderWebNavigationPolicy.continuousBoundaryResultAction(
                result = "\"limit\"",
                forward = true,
            ),
        )
        assertEquals(
            NovelReaderWebNavigationPolicy.ContinuousBoundaryResultAction.UseChapterFallback(forward = false),
            NovelReaderWebNavigationPolicy.continuousBoundaryResultAction(
                result = "\"limit\"",
                forward = false,
            ),
        )
        assertEquals(
            NovelReaderWebNavigationPolicy.ContinuousBoundaryResultAction.LetHostScroll,
            NovelReaderWebNavigationPolicy.continuousBoundaryResultAction(
                result = "\"scrolling\"",
                forward = true,
            ),
        )
    }

    @Test
    fun pagedNavigationResultActionReportsProgressOrFallsBackByResult() {
        assertEquals(
            NovelReaderWebNavigationPolicy.PagedNavigationResultAction.ReportProgress(
                NovelReaderWebScriptPolicy.calculateProgressScript(),
            ),
            NovelReaderWebNavigationPolicy.pagedNavigationResultAction(
                result = "\"scrolled\"",
                forward = true,
            ),
        )
        assertEquals(
            NovelReaderWebNavigationPolicy.PagedNavigationResultAction.UseChapterFallback(forward = true),
            NovelReaderWebNavigationPolicy.pagedNavigationResultAction(
                result = "\"limit\"",
                forward = true,
            ),
        )
        assertEquals(
            NovelReaderWebNavigationPolicy.PagedNavigationResultAction.UseChapterFallback(forward = false),
            NovelReaderWebNavigationPolicy.pagedNavigationResultAction(
                result = null,
                forward = false,
            ),
        )
    }
}
