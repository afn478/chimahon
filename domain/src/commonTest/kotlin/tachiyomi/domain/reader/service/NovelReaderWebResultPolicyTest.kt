package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals

class NovelReaderWebResultPolicyTest {
    @Test
    fun progressResultParsesPlainOrQuotedJavascriptNumbers() {
        assertEquals(0.42, NovelReaderWebResultPolicy.progressResult("0.42"))
        assertEquals(0.75, NovelReaderWebResultPolicy.progressResult("  \"0.75\"  "))
    }

    @Test
    fun progressResultRejectsMissingOrNonNumericResults() {
        assertEquals(null, NovelReaderWebResultPolicy.progressResult(null))
        assertEquals(null, NovelReaderWebResultPolicy.progressResult("\"scrolled\""))
    }

    @Test
    fun continuousBoundaryActionUsesChapterFallbackOnlyAtLimit() {
        assertEquals(
            NovelReaderWebResultPolicy.ContinuousBoundaryAction.UseChapterFallback,
            NovelReaderWebResultPolicy.continuousBoundaryAction("\"limit\""),
        )
        assertEquals(
            NovelReaderWebResultPolicy.ContinuousBoundaryAction.LetWebViewScroll,
            NovelReaderWebResultPolicy.continuousBoundaryAction("\"scrolling\""),
        )
        assertEquals(
            NovelReaderWebResultPolicy.ContinuousBoundaryAction.LetWebViewScroll,
            NovelReaderWebResultPolicy.continuousBoundaryAction(null),
        )
    }

    @Test
    fun pagedNavigationActionReportsProgressOnlyWhenPageScrolled() {
        assertEquals(
            NovelReaderWebResultPolicy.PagedNavigationAction.ReportProgress,
            NovelReaderWebResultPolicy.pagedNavigationAction("\"scrolled\""),
        )
        assertEquals(
            NovelReaderWebResultPolicy.PagedNavigationAction.UseChapterFallback,
            NovelReaderWebResultPolicy.pagedNavigationAction("\"limit\""),
        )
        assertEquals(
            NovelReaderWebResultPolicy.PagedNavigationAction.UseChapterFallback,
            NovelReaderWebResultPolicy.pagedNavigationAction(null),
        )
    }
}
