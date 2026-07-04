package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import tachiyomi.domain.reader.model.NovelReaderTocItem

class NovelReaderChapterListSheetPolicyTest {
    @Test
    fun tocRowsResolveSpineIndexAndCharacterLabels() {
        val rows = NovelReaderChapterListSheetPolicy.tocRows(
            toc = listOf(
                NovelReaderTocItem(
                    label = "Start",
                    href = "chapter-1.xhtml",
                    depth = 0,
                    fragment = "p1",
                ),
                NovelReaderTocItem(
                    label = "Aside",
                    href = null,
                    depth = 1,
                    fragment = null,
                ),
            ),
            currentSpineIndex = 2,
            characterCountForSpineIndex = { index -> if (index == 2) 1200 else null },
            spineIndexForHref = { href -> if (href == "chapter-1.xhtml") 2 else null },
        )

        assertEquals(
            listOf(
                NovelReaderChapterListSheetPolicy.ChapterRow(
                    title = "Start",
                    spineIndex = 2,
                    fragment = "p1",
                    depth = 0,
                    isCurrent = true,
                    characterCountLabel = "1200",
                ),
                NovelReaderChapterListSheetPolicy.ChapterRow(
                    title = "Aside",
                    spineIndex = 0,
                    fragment = null,
                    depth = 1,
                    isCurrent = false,
                    characterCountLabel = "...",
                ),
            ),
            rows,
        )
    }

    @Test
    fun fallbackRowsPreferTitleThenHrefThenGeneratedChapterName() {
        val rows = NovelReaderChapterListSheetPolicy.fallbackRows(
            chapterCount = 3,
            currentSpineIndex = 1,
            titleForSpineIndex = { index -> if (index == 0) "Cover" else null },
            hrefForSpineIndex = { index -> if (index == 1) "chapter-2.xhtml" else null },
            characterCountForSpineIndex = { index -> if (index == 2) 900 else null },
        )

        assertEquals(
            listOf(
                NovelReaderChapterListSheetPolicy.ChapterRow(
                    title = "Cover",
                    spineIndex = 0,
                    fragment = null,
                    depth = 0,
                    isCurrent = false,
                    characterCountLabel = "...",
                ),
                NovelReaderChapterListSheetPolicy.ChapterRow(
                    title = "chapter-2.xhtml",
                    spineIndex = 1,
                    fragment = null,
                    depth = 0,
                    isCurrent = true,
                    characterCountLabel = "...",
                ),
                NovelReaderChapterListSheetPolicy.ChapterRow(
                    title = "Chapter 2",
                    spineIndex = 2,
                    fragment = null,
                    depth = 0,
                    isCurrent = false,
                    characterCountLabel = "900",
                ),
            ),
            rows,
        )
    }

    @Test
    fun hasTableOfContentsTracksWhetherTocRowsAreAvailable() {
        assertFalse(NovelReaderChapterListSheetPolicy.hasTableOfContents(emptyList()))
        assertTrue(
            NovelReaderChapterListSheetPolicy.hasTableOfContents(
                listOf(NovelReaderTocItem("Chapter", href = null, depth = 0, fragment = null)),
            ),
        )
    }
}
