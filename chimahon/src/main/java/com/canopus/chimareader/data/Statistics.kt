package com.canopus.chimareader.data

import kotlinx.serialization.Serializable
import tachiyomi.domain.reader.model.NovelReadingStatistic

typealias Statistics = NovelReadingStatistic

@Serializable
enum class StatisticsSyncMode(val value: String) {
    MERGE("Merge"),
    REPLACE("Replace"),
    ;

    companion object {
        fun fromValue(value: String): StatisticsSyncMode {
            return entries.find { it.value == value } ?: MERGE
        }
    }
}

@Serializable
data class AnkiStats(
    val dateKey: String,
    var mangaCards: Int = 0,
    var novelCards: Int = 0,
)

@Serializable
data class MangaStats(
    val dateKey: String,
    var charactersRead: Int = 0,
    var readingTime: Long = 0, // In ms
    var mangaId: Long = 0,
)
