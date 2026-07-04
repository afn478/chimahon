package com.canopus.chimareader.data

import kotlinx.serialization.Serializable
import tachiyomi.domain.history.model.AnkiCardStatistic
import tachiyomi.domain.history.model.MangaReadingStatistic
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

typealias AnkiStats = AnkiCardStatistic

typealias MangaStats = MangaReadingStatistic
