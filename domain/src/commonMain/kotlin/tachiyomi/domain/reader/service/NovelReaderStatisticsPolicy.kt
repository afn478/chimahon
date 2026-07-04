package tachiyomi.domain.reader.service

import kotlin.math.abs
import tachiyomi.domain.reader.model.NovelReadingStatistic

object NovelReaderStatisticsPolicy {
    private const val SECONDS_PER_HOUR = 3600.0
    private const val LEGACY_MILLISECONDS_SPEED_THRESHOLD = 500.0

    fun defaultStatistic(
        title: String,
        dateKey: String,
    ): NovelReadingStatistic {
        return NovelReadingStatistic(title = title, dateKey = dateKey)
    }

    fun statisticForDate(
        statistics: List<NovelReadingStatistic>,
        title: String,
        dateKey: String,
    ): NovelReadingStatistic {
        return statistics.firstOrNull { it.dateKey == dateKey } ?: defaultStatistic(title, dateKey)
    }

    fun normalizeLoadedStatistics(statistics: List<NovelReadingStatistic>): List<NovelReadingStatistic> {
        return latestByDate(statistics.map(::normalizeLegacyReadingTime))
    }

    fun normalizeLegacyReadingTime(statistic: NovelReadingStatistic): NovelReadingStatistic {
        if (statistic.readingTime <= 0.0) return statistic

        val readingSpeed = statistic.charactersRead / statistic.readingTime * SECONDS_PER_HOUR
        return if (readingSpeed < LEGACY_MILLISECONDS_SPEED_THRESHOLD) {
            statistic.copy(readingTime = statistic.readingTime / 1000.0)
        } else {
            statistic
        }
    }

    fun latestByDate(statistics: List<NovelReadingStatistic>): List<NovelReadingStatistic> {
        return statistics
            .groupBy { it.dateKey }
            .mapValues { (_, entries) -> entries.maxBy { it.lastStatisticModified } }
            .values
            .toList()
    }

    fun statisticsForPersistence(
        existing: List<NovelReadingStatistic>,
        today: NovelReadingStatistic,
    ): List<NovelReadingStatistic> {
        val grouped = latestByDate(existing).associateBy { it.dateKey }.toMutableMap()
        grouped[today.dateKey] = today
        return grouped.values.toList()
    }

    fun allTimeStatistic(
        title: String,
        dateKey: String,
        statistics: List<NovelReadingStatistic>,
    ): NovelReadingStatistic {
        val base = defaultStatistic(title = title, dateKey = dateKey)
        return statistics.fold(base) { total, statistic ->
            val readingTime = total.readingTime + statistic.readingTime
            val charactersRead = total.charactersRead + statistic.charactersRead
            total.copy(
                readingTime = readingTime,
                charactersRead = charactersRead,
                lastReadingSpeed = readingSpeed(
                    charactersRead = charactersRead,
                    readingTime = readingTime,
                ),
            )
        }
    }

    fun updateStatistic(
        statistic: NovelReadingStatistic,
        timeDiffSeconds: Double,
        characterDiff: Int,
        lastStatisticModified: Long,
    ): NovelReadingStatistic {
        val nextReadingTime = statistic.readingTime + timeDiffSeconds
        val nextCharactersRead = (statistic.charactersRead + characterDiff).coerceAtLeast(0)
        val nextReadingSpeed = readingSpeed(
            charactersRead = nextCharactersRead,
            readingTime = nextReadingTime,
        )
        return statistic.copy(
            readingTime = nextReadingTime,
            charactersRead = nextCharactersRead,
            lastReadingSpeed = nextReadingSpeed,
            maxReadingSpeed = maxOf(statistic.maxReadingSpeed, nextReadingSpeed),
            minReadingSpeed = if (statistic.minReadingSpeed != 0) {
                minOf(statistic.minReadingSpeed, nextReadingSpeed)
            } else {
                nextReadingSpeed
            },
            altMinReadingSpeed = if (characterDiff != 0) {
                if (statistic.altMinReadingSpeed != 0) {
                    minOf(statistic.altMinReadingSpeed, nextReadingSpeed)
                } else {
                    nextReadingSpeed
                }
            } else {
                statistic.altMinReadingSpeed
            },
            lastStatisticModified = lastStatisticModified,
        )
    }

    fun clampBackwardCharacterDiff(
        characterDiff: Int,
        sessionCharactersRead: Int,
    ): Int {
        return if (characterDiff < 0 && abs(characterDiff) > sessionCharactersRead) {
            -sessionCharactersRead
        } else {
            characterDiff
        }
    }

    private fun readingSpeed(
        charactersRead: Int,
        readingTime: Double,
    ): Int {
        return if (readingTime > 0.0) {
            (charactersRead.toDouble() / readingTime * SECONDS_PER_HOUR).toInt()
        } else {
            0
        }
    }
}
