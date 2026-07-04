package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.NovelReaderTocItem

object NovelReaderChapterListSheetPolicy {
    const val TITLE = "Chapters"
    const val PENDING_CHARACTER_COUNT_LABEL = "..."

    data class ChapterRow(
        val title: String,
        val spineIndex: Int,
        val fragment: String?,
        val depth: Int,
        val isCurrent: Boolean,
        val characterCountLabel: String,
    )

    fun tocRows(
        toc: List<NovelReaderTocItem>,
        currentSpineIndex: Int,
        characterCountForSpineIndex: (Int) -> Int?,
        spineIndexForHref: (String) -> Int?,
    ): List<ChapterRow> {
        return toc.map { entry ->
            val spineIndex = entry.href?.let(spineIndexForHref) ?: 0
            ChapterRow(
                title = entry.label,
                spineIndex = spineIndex,
                fragment = entry.fragment,
                depth = entry.depth,
                isCurrent = spineIndex == currentSpineIndex,
                characterCountLabel = characterCountLabel(characterCountForSpineIndex(spineIndex)),
            )
        }
    }

    fun fallbackRows(
        chapterCount: Int,
        currentSpineIndex: Int,
        titleForSpineIndex: (Int) -> String?,
        hrefForSpineIndex: (Int) -> String?,
        characterCountForSpineIndex: (Int) -> Int?,
    ): List<ChapterRow> {
        return (0 until chapterCount.coerceAtLeast(0)).map { index ->
            ChapterRow(
                title = fallbackTitle(
                    index = index,
                    title = titleForSpineIndex(index),
                    href = hrefForSpineIndex(index),
                ),
                spineIndex = index,
                fragment = null,
                depth = 0,
                isCurrent = index == currentSpineIndex,
                characterCountLabel = characterCountLabel(characterCountForSpineIndex(index)),
            )
        }
    }

    fun hasTableOfContents(toc: List<NovelReaderTocItem>): Boolean {
        return toc.isNotEmpty()
    }

    private fun fallbackTitle(
        index: Int,
        title: String?,
        href: String?,
    ): String {
        return title ?: href ?: "Chapter $index"
    }

    private fun characterCountLabel(characterCount: Int?): String {
        return characterCount?.toString() ?: PENDING_CHARACTER_COUNT_LABEL
    }
}
