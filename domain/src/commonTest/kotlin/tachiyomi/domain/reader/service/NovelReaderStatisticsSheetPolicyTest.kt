package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import tachiyomi.domain.reader.model.NovelReaderStatisticsState
import tachiyomi.domain.reader.model.NovelReadingStatistic

class NovelReaderStatisticsSheetPolicyTest {
    @Test
    fun sheetStateUsesCurrentPositionForTrackingProjection() {
        val state = NovelReaderStatisticsSheetPolicy.sheetState(
            trackerState = NovelReaderStatisticsState(
                isTracking = true,
                session = NovelReadingStatistic(
                    title = "Book",
                    dateKey = "2026-07-04",
                    charactersRead = 100,
                    readingTime = 60.0,
                    lastReadingSpeed = 3600,
                ),
                today = NovelReadingStatistic(
                    title = "Book",
                    dateKey = "2026-07-04",
                    charactersRead = 200,
                    readingTime = 120.0,
                    lastReadingSpeed = 6000,
                ),
                allTime = NovelReadingStatistic(
                    title = "Book",
                    dateKey = "2026-07-04",
                    charactersRead = 300,
                    readingTime = 180.0,
                    lastReadingSpeed = 7200,
                ),
            ),
            currentCharacter = 3600,
            frozenPosition = 7000,
            totalCharacters = 7200,
            currentChapterEndCharacter = 5400,
        )

        assertEquals("Statistics", state.title)
        assertEquals("Pause Timer", state.toggleContentDescription)
        assertTrue(state.showPauseIcon)
        assertEquals(
            listOf(
                NovelReaderStatisticsSheetPolicy.Section(
                    title = "Session",
                    rows = listOf(
                        NovelReaderStatisticsSheetPolicy.Row("Characters Read", "100"),
                        NovelReaderStatisticsSheetPolicy.Row("Reading Speed", "3600 / h"),
                        NovelReaderStatisticsSheetPolicy.Row("Reading Time", "01:00"),
                        NovelReaderStatisticsSheetPolicy.Row("Time to finish Book", "1h 0m 0s"),
                        NovelReaderStatisticsSheetPolicy.Row("Time to finish Chapter", "30m 0s"),
                    ),
                ),
                NovelReaderStatisticsSheetPolicy.Section(
                    title = "Today",
                    rows = listOf(
                        NovelReaderStatisticsSheetPolicy.Row("Characters Read", "200"),
                        NovelReaderStatisticsSheetPolicy.Row("Reading Speed", "6000 / h"),
                        NovelReaderStatisticsSheetPolicy.Row("Reading Time", "02:00"),
                    ),
                ),
                NovelReaderStatisticsSheetPolicy.Section(
                    title = "All Time",
                    rows = listOf(
                        NovelReaderStatisticsSheetPolicy.Row("Characters Read", "300"),
                        NovelReaderStatisticsSheetPolicy.Row("Reading Speed", "7200 / h"),
                        NovelReaderStatisticsSheetPolicy.Row("Reading Time", "03:00"),
                    ),
                ),
            ),
            state.sections,
        )
    }

    @Test
    fun sheetStateUsesFrozenPositionWhenPaused() {
        val state = NovelReaderStatisticsSheetPolicy.sheetState(
            trackerState = NovelReaderStatisticsState(
                isTracking = false,
                session = NovelReadingStatistic(
                    title = "Book",
                    dateKey = "2026-07-04",
                    charactersRead = 120,
                    readingTime = 90.0,
                    lastReadingSpeed = 3600,
                ),
                today = NovelReadingStatistic(title = "Book", dateKey = "2026-07-04"),
                allTime = NovelReadingStatistic(title = "Book", dateKey = "2026-07-04"),
            ),
            currentCharacter = 1200,
            frozenPosition = 3600,
            totalCharacters = 7200,
            currentChapterEndCharacter = 5400,
        )

        assertEquals("Resume Timer", state.toggleContentDescription)
        assertFalse(state.showPauseIcon)
        assertEquals(
            listOf(
                NovelReaderStatisticsSheetPolicy.Row("Characters Read", "120"),
                NovelReaderStatisticsSheetPolicy.Row("Reading Speed", "3600 / h"),
                NovelReaderStatisticsSheetPolicy.Row("Reading Time", "01:30"),
                NovelReaderStatisticsSheetPolicy.Row("Time to finish Book", "1h 0m 0s"),
                NovelReaderStatisticsSheetPolicy.Row("Time to finish Chapter", "30m 0s"),
            ),
            state.sections.first().rows,
        )
    }
}
