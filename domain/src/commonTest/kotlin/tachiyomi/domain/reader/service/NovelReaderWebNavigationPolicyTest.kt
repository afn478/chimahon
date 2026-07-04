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
}
