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
        return href
            ?.substringAfter("#", missingDelimiterValue = "")
            ?.takeIf { it.isNotEmpty() }
    }

    private fun normalizedHrefPath(href: String): String {
        return percentDecode(
            href
                .substringBefore("#")
                .substringBefore("?")
                .replace("\\", "/"),
        )
    }

    private fun normalizedFilePath(pathOrUrl: String): String {
        return percentDecode(
            pathOrUrl
                .substringBefore("#")
                .substringBefore("?")
                .removePrefix("file://")
                .replace("\\", "/"),
        )
    }

    private fun percentDecode(value: String): String {
        if ('%' !in value) return value

        val builder = StringBuilder()
        val bytes = mutableListOf<Byte>()

        fun flushBytes() {
            if (bytes.isEmpty()) return
            builder.append(ByteArray(bytes.size) { bytes[it] }.decodeToString())
            bytes.clear()
        }

        var index = 0
        while (index < value.length) {
            val char = value[index]
            val firstHex = value.getOrNull(index + 1)?.hexValue()
            val secondHex = value.getOrNull(index + 2)?.hexValue()

            if (char == '%' && firstHex != null && secondHex != null) {
                bytes += ((firstHex shl 4) + secondHex).toByte()
                index += 3
            } else {
                flushBytes()
                builder.append(char)
                index += 1
            }
        }

        flushBytes()
        return builder.toString()
    }

    private fun Char.hexValue(): Int? {
        return when (this) {
            in '0'..'9' -> this - '0'
            in 'a'..'f' -> this - 'a' + 10
            in 'A'..'F' -> this - 'A' + 10
            else -> null
        }
    }
}
