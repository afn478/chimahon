package tachiyomi.domain.reader.model

import kotlinx.serialization.Serializable

@Serializable
data class NovelReaderBookmark(
    val chapterIndex: Int,
    val progress: Double,
    val characterCount: Int,
    val lastModified: Long? = null,
)
