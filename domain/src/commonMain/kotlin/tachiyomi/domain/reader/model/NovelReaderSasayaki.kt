package tachiyomi.domain.reader.model

import kotlinx.serialization.Serializable

@Serializable
data class NovelReaderSasayakiCue(
    val id: String,
    val startTime: Double,
    val endTime: Double,
    val text: String,
)

@Serializable
data class NovelReaderSasayakiMatch(
    val id: String,
    val startTime: Double,
    val endTime: Double,
    val text: String,
    val chapterIndex: Int,
    val start: Int,
    val length: Int,
)

@Serializable
data class NovelReaderSasayakiCueRange(
    val id: String,
    val start: Int,
    val length: Int,
)

@Serializable
data class NovelReaderSasayakiMatchData(
    val matches: List<NovelReaderSasayakiMatch>,
    val unmatched: Int,
)

@Serializable
data class NovelReaderSasayakiPlaybackData(
    var lastPosition: Double,
    var delay: Double = 0.0,
    var rate: Float = 1f,
    var audioBookmark: String? = null,
)
