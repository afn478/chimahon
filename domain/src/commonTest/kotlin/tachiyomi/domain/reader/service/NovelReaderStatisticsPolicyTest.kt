package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import tachiyomi.domain.reader.model.NovelReadingStatistic

class NovelReaderStatisticsPolicyTest {
    @Test
    fun normalizeLoadedStatisticsConvertsLegacyMillisecondsAndKeepsLatestDateEntry() {
        val oldDuplicate = statistic(
            dateKey = "2026-07-03",
            charactersRead = 100,
            readingTime = 10.0,
            lastStatisticModified = 1,
        )
        val latestDuplicateWithMilliseconds = statistic(
            dateKey = "2026-07-03",
            charactersRead = 1_000,
            readingTime = 3_600_000.0,
            lastStatisticModified = 2,
        )

        val result = NovelReaderStatisticsPolicy.normalizeLoadedStatistics(
            listOf(oldDuplicate, latestDuplicateWithMilliseconds),
        )

        assertEquals(
            listOf(
                latestDuplicateWithMilliseconds.copy(readingTime = 3_600.0),
            ),
            result,
        )
    }

    @Test
    fun updateStatisticAppliesTimeCharactersAndReadingSpeedBounds() {
        val result = NovelReaderStatisticsPolicy.updateStatistic(
            statistic = statistic(
                charactersRead = 100,
                readingTime = 50.0,
                minReadingSpeed = 8_000,
                altMinReadingSpeed = 9_000,
                maxReadingSpeed = 10_000,
            ),
            timeDiffSeconds = 10.0,
            characterDiff = 50,
            lastStatisticModified = 42,
        )

        assertEquals(150, result.charactersRead)
        assertEquals(60.0, result.readingTime)
        assertEquals(9_000, result.lastReadingSpeed)
        assertEquals(10_000, result.maxReadingSpeed)
        assertEquals(8_000, result.minReadingSpeed)
        assertEquals(9_000, result.altMinReadingSpeed)
        assertEquals(42, result.lastStatisticModified)
    }

    @Test
    fun updateStatisticPreservesAltMinimumWhenNoCharactersWereRead() {
        val result = NovelReaderStatisticsPolicy.updateStatistic(
            statistic = statistic(
                charactersRead = 100,
                readingTime = 50.0,
                altMinReadingSpeed = 7_200,
            ),
            timeDiffSeconds = 10.0,
            characterDiff = 0,
            lastStatisticModified = 42,
        )

        assertEquals(7_200, result.altMinReadingSpeed)
    }

    @Test
    fun clampBackwardCharacterDiffLimitsBacktrackingToSessionCharacters() {
        assertEquals(
            -40,
            NovelReaderStatisticsPolicy.clampBackwardCharacterDiff(
                characterDiff = -100,
                sessionCharactersRead = 40,
            ),
        )
        assertEquals(
            -20,
            NovelReaderStatisticsPolicy.clampBackwardCharacterDiff(
                characterDiff = -20,
                sessionCharactersRead = 40,
            ),
        )
    }

    @Test
    fun statisticsForPersistenceReplacesTodayAndDeduplicatesExistingDates() {
        val result = NovelReaderStatisticsPolicy.statisticsForPersistence(
            existing = listOf(
                statistic(dateKey = "2026-07-03", charactersRead = 10, lastStatisticModified = 1),
                statistic(dateKey = "2026-07-03", charactersRead = 20, lastStatisticModified = 2),
                statistic(dateKey = "2026-07-04", charactersRead = 30, lastStatisticModified = 1),
            ),
            today = statistic(dateKey = "2026-07-04", charactersRead = 99, lastStatisticModified = 3),
        )

        assertEquals(
            listOf(
                statistic(dateKey = "2026-07-03", charactersRead = 20, lastStatisticModified = 2),
                statistic(dateKey = "2026-07-04", charactersRead = 99, lastStatisticModified = 3),
            ),
            result,
        )
    }

    @Test
    fun allTimeStatisticSumsReadingTimeAndCharacters() {
        val result = NovelReaderStatisticsPolicy.allTimeStatistic(
            title = "Book",
            dateKey = "2026-07-04",
            statistics = listOf(
                statistic(charactersRead = 100, readingTime = 10.0),
                statistic(charactersRead = 300, readingTime = 30.0),
            ),
        )

        assertEquals("Book", result.title)
        assertEquals("2026-07-04", result.dateKey)
        assertEquals(400, result.charactersRead)
        assertEquals(40.0, result.readingTime)
        assertEquals(36_000, result.lastReadingSpeed)
    }

    @Test
    fun elapsedDurationLabelUsesClockStyleDurations() {
        assertEquals("00:00", NovelReaderStatisticsPolicy.elapsedDurationLabel(0))
        assertEquals("01:05", NovelReaderStatisticsPolicy.elapsedDurationLabel(65))
        assertEquals("1:00:00", NovelReaderStatisticsPolicy.elapsedDurationLabel(3_600))
        assertEquals("27:46:40", NovelReaderStatisticsPolicy.elapsedDurationLabel(100_000))
        assertEquals("00:00", NovelReaderStatisticsPolicy.elapsedDurationLabel(-1))
    }

    @Test
    fun remainingDurationLabelUsesCompactUnits() {
        assertEquals("0s", NovelReaderStatisticsPolicy.remainingDurationLabel(-1.0))
        assertEquals("59s", NovelReaderStatisticsPolicy.remainingDurationLabel(59.9))
        assertEquals("1m 0s", NovelReaderStatisticsPolicy.remainingDurationLabel(60.0))
        assertEquals("1h 1m 1s", NovelReaderStatisticsPolicy.remainingDurationLabel(3_661.8))
    }

    @Test
    fun secondsRemainingUsesReadingSpeedPerHour() {
        assertEquals(
            1_800.0,
            NovelReaderStatisticsPolicy.secondsRemaining(
                remainingCharacters = 1_000,
                readingSpeedPerHour = 2_000,
            ),
        )
        assertEquals(
            0.0,
            NovelReaderStatisticsPolicy.secondsRemaining(
                remainingCharacters = -100,
                readingSpeedPerHour = 2_000,
            ),
        )
        assertEquals(
            0.0,
            NovelReaderStatisticsPolicy.secondsRemaining(
                remainingCharacters = 100,
                readingSpeedPerHour = 0,
            ),
        )
    }

    private fun statistic(
        title: String = "Book",
        dateKey: String = "2026-07-04",
        charactersRead: Int = 0,
        readingTime: Double = 0.0,
        minReadingSpeed: Int = 0,
        altMinReadingSpeed: Int = 0,
        lastReadingSpeed: Int = 0,
        maxReadingSpeed: Int = 0,
        lastStatisticModified: Long = 0,
    ): NovelReadingStatistic {
        return NovelReadingStatistic(
            title = title,
            dateKey = dateKey,
            charactersRead = charactersRead,
            readingTime = readingTime,
            minReadingSpeed = minReadingSpeed,
            altMinReadingSpeed = altMinReadingSpeed,
            lastReadingSpeed = lastReadingSpeed,
            maxReadingSpeed = maxReadingSpeed,
            lastStatisticModified = lastStatisticModified,
        )
    }
}
