package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import tachiyomi.domain.reader.model.NovelReaderChapterJump
import tachiyomi.domain.reader.model.NovelReaderTocEntry
import tachiyomi.domain.reader.model.NovelReaderTocItem

class NovelReaderNavigationPolicyTest {
    @Test
    fun chapterTitleUsesMatchingTocLabelBeforeFileNameFallback() {
        val toc = listOf(
            tocEntry(
                label = "Part 1",
                href = "text/part1.xhtml#top",
                children = listOf(tocEntry(label = "Chapter 2", href = "text/chapter2.xhtml#middle")),
            ),
        )

        assertEquals(
            "Chapter 2",
            NovelReaderNavigationPolicy.chapterTitle(
                chapterHref = "OPS/text/chapter2.xhtml",
                tableOfContents = toc,
            ),
        )
        assertEquals(
            "chapter3",
            NovelReaderNavigationPolicy.chapterTitle(
                chapterHref = "OPS/text/chapter3.xhtml",
                tableOfContents = toc,
            ),
        )
    }

    @Test
    fun flattenTocPreservesHierarchyDepthAndFragments() {
        val toc = listOf(
            tocEntry(
                label = "Part 1",
                href = "part1.xhtml",
                children = listOf(
                    tocEntry(label = "Chapter 1", href = "chapter1.xhtml#section-a"),
                    tocEntry(label = "Chapter 2", href = null),
                ),
            ),
        )

        assertEquals(
            listOf(
                NovelReaderTocItem(label = "Part 1", href = "part1.xhtml", depth = 0, fragment = null),
                NovelReaderTocItem(label = "Chapter 1", href = "chapter1.xhtml#section-a", depth = 1, fragment = "section-a"),
                NovelReaderTocItem(label = "Chapter 2", href = null, depth = 1, fragment = null),
            ),
            NovelReaderNavigationPolicy.flattenToc(toc),
        )
    }

    @Test
    fun spineIndexForHrefMatchesDecodedRelativeHrefAndFileName() {
        val chapterHrefs = listOf(
            "OPS/Text/Chapter 1.xhtml",
            "OPS/Text/Chapter 2.xhtml",
            "OPS/Text/Interlude.xhtml",
        )

        assertEquals(
            1,
            NovelReaderNavigationPolicy.spineIndexForHref(
                href = "../Text/Chapter%202.xhtml#start",
                chapterHrefs = chapterHrefs,
            ),
        )
        assertEquals(
            2,
            NovelReaderNavigationPolicy.spineIndexForHref(
                href = "Interlude.xhtml?nav=1",
                chapterHrefs = chapterHrefs,
            ),
        )
    }

    @Test
    fun resolveInternalFileLinkMatchesDecodedFileUrlAndKeepsFragment() {
        val chapterPaths = listOf(
            "/books/Book/Text/Chapter 1.xhtml",
            "/books/Book/Text/Chapter 2.xhtml",
        )

        assertEquals(
            NovelReaderChapterJump(spineIndex = 1, fragment = "paragraph-4"),
            NovelReaderNavigationPolicy.resolveInternalFileLink(
                url = "file:///books/Book/Text/Chapter%202.xhtml#paragraph-4",
                chapterPaths = chapterPaths,
            ),
        )
    }

    @Test
    fun resolveInternalFileLinkReturnsNullForExternalOrMissingLinks() {
        assertEquals(
            null,
            NovelReaderNavigationPolicy.resolveInternalFileLink(
                url = "https://example.com/Chapter%202.xhtml",
                chapterPaths = listOf("/books/Book/Text/Chapter 2.xhtml"),
            ),
        )
    }

    @Test
    fun webLinkActionKeepsSameChapterFragmentInCurrentWebView() {
        assertEquals(
            NovelReaderNavigationPolicy.WebLinkAction.SameChapter(fragment = "paragraph-4"),
            NovelReaderNavigationPolicy.webLinkAction(
                currentUrl = "file:///books/Book/Text/Chapter%202.xhtml",
                targetUrl = "file:///books/Book/Text/Chapter 2.xhtml#paragraph-4",
            ),
        )
    }

    @Test
    fun webLinkActionKeepsSameChapterWithoutFragmentInCurrentWebView() {
        assertEquals(
            NovelReaderNavigationPolicy.WebLinkAction.SameChapter(fragment = null),
            NovelReaderNavigationPolicy.webLinkAction(
                currentUrl = "file:///books/Book/Text/Chapter%202.xhtml",
                targetUrl = "file:///books/Book/Text/Chapter 2.xhtml",
            ),
        )
    }

    @Test
    fun webLinkActionNavigatesForDifferentChapterOrMissingCurrentUrl() {
        assertEquals(
            NovelReaderNavigationPolicy.WebLinkAction.NavigateToUrl(
                url = "file:///books/Book/Text/Chapter%203.xhtml#start",
            ),
            NovelReaderNavigationPolicy.webLinkAction(
                currentUrl = "file:///books/Book/Text/Chapter%202.xhtml",
                targetUrl = "file:///books/Book/Text/Chapter%203.xhtml#start",
            ),
        )
        assertEquals(
            NovelReaderNavigationPolicy.WebLinkAction.NavigateToUrl(
                url = "file:///books/Book/Text/Chapter%202.xhtml#start",
            ),
            NovelReaderNavigationPolicy.webLinkAction(
                currentUrl = null,
                targetUrl = "file:///books/Book/Text/Chapter%202.xhtml#start",
            ),
        )
    }

    private fun tocEntry(
        label: String,
        href: String?,
        children: List<NovelReaderTocEntry> = emptyList(),
    ): NovelReaderTocEntry {
        return NovelReaderTocEntry(
            label = label,
            href = href,
            children = children,
        )
    }
}
