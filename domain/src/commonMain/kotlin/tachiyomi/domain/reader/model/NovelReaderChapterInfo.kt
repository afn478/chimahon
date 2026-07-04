package tachiyomi.domain.reader.model

import kotlinx.serialization.Serializable

@Serializable
data class NovelReaderChapterInfo(
    val spineIndex: Int?,
    val currentTotal: Int,
    val chapterCount: Int,
)
