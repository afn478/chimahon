package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.NovelReaderChapterJump
import tachiyomi.domain.reader.model.NovelReaderTocEntry
import tachiyomi.domain.reader.model.NovelReaderTocItem

object NovelReaderNavigationPolicy {
    fun chapterTitle(
        chapterHref: String?,
        tableOfContents: List<NovelReaderTocEntry>,
    ): String? {
        if (chapterHref == null) return null
        return tocLabelForHref(tableOfContents, chapterHref)
            ?: chapterHref.substringAfterLast("/").substringBefore(".")
    }

    fun flattenToc(entries: List<NovelReaderTocEntry>): List<NovelReaderTocItem> {
        val flattened = mutableListOf<NovelReaderTocItem>()

        fun flatten(items: List<NovelReaderTocEntry>, depth: Int) {
            items.forEach { entry ->
                flattened += NovelReaderTocItem(
                    label = entry.label,
                    href = entry.href,
                    depth = depth,
                    fragment = fragmentForHref(entry.href),
                )
                flatten(entry.children, depth + 1)
            }
        }

        flatten(entries, depth = 0)
        return flattened
    }

    fun spineIndexForHref(
        href: String,
        chapterHrefs: List<String?>,
    ): Int? {
        val decodedHref = normalizedHrefPath(href)
        val fileName = decodedHref.substringAfterLast("/")

        return chapterHrefs.indexOfFirst { chapterHref ->
            if (chapterHref == null) return@indexOfFirst false
            val normalizedChapterHref = normalizedHrefPath(chapterHref)
            val chapterFileName = normalizedChapterHref.substringAfterLast("/")

            normalizedChapterHref.endsWith(decodedHref) || chapterFileName == fileName
        }.takeIf { it >= 0 }
    }

    fun resolveInternalFileLink(
        url: String,
        chapterPaths: List<String?>,
    ): NovelReaderChapterJump? {
        val targetPath = normalizedFilePath(url)

        val spineIndex = chapterPaths.indexOfFirst { chapterPath ->
            chapterPath != null && normalizedFilePath(chapterPath) == targetPath
        }.takeIf { it >= 0 } ?: return null

        return NovelReaderChapterJump(
            spineIndex = spineIndex,
            fragment = fragmentForHref(url),
        )
    }

    private fun tocLabelForHref(
        toc: List<NovelReaderTocEntry>,
        href: String,
    ): String? {
        val fileName = normalizedHrefPath(href).substringAfterLast("/")
        val fileStem = fileName.substringBefore(".")

        for (entry in toc) {
            val entryHref = entry.href?.let(::normalizedHrefPath)
            if (entryHref != null && (entryHref.endsWith(fileName) || entryHref.contains(fileStem))) {
                return entry.label
            }

            val childLabel = tocLabelForHref(entry.children, href)
            if (childLabel != null) return childLabel
        }

        return null
    }

    private fun fragmentForHref(href: String?): String? {
        return NovelReaderFileUrlPolicy.fragmentForUrl(href)
    }

    private fun normalizedHrefPath(href: String): String {
        return NovelReaderFileUrlPolicy.hrefPathForComparison(href)
    }

    private fun normalizedFilePath(pathOrUrl: String): String {
        return NovelReaderFileUrlPolicy.localPathForFileUrlOrPath(pathOrUrl)
    }
}
