package tachiyomi.domain.reader.model

data class NovelReaderTocItem(
    val label: String,
    val href: String?,
    val depth: Int,
    val fragment: String?,
)
