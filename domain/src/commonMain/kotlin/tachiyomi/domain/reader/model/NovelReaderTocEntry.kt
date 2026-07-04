package tachiyomi.domain.reader.model

data class NovelReaderTocEntry(
    val label: String,
    val href: String? = null,
    val children: List<NovelReaderTocEntry> = emptyList(),
)
