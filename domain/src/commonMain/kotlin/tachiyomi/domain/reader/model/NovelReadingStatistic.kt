package tachiyomi.domain.reader.model

import kotlinx.serialization.Serializable

@Serializable
data class NovelReadingStatistic(
    val title: String,
    val dateKey: String,
    var charactersRead: Int = 0,
    var readingTime: Double = 0.0,
    var minReadingSpeed: Int = 0,
    var altMinReadingSpeed: Int = 0,
    var lastReadingSpeed: Int = 0,
    var maxReadingSpeed: Int = 0,
    var lastStatisticModified: Long = 0,
    var completedBook: Int? = null,
    var completedData: NovelReadingStatistic? = null,
)
