package tachiyomi.domain.history.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import tachiyomi.domain.history.model.MangaReadingStatistic

class MangaReadingStatisticsPolicyTest {
    @Test
    fun addStatsCreatesOrUpdatesDailyMangaEntry() {
        val created = MangaReadingStatisticsPolicy.addStats(
            statistics = emptyList(),
            dateKey = "2026-07-04",
            characters = 120,
            timeMs = 5_000,
            mangaId = 7,
        )
        val updated = MangaReadingStatisticsPolicy.addStats(
            statistics = created.statistics,
            dateKey = "2026-07-04",
            characters = 80,
            timeMs = 2_000,
            mangaId = 7,
        )

        assertTrue(created.changed)
        assertTrue(updated.changed)
        assertEquals(
            listOf(MangaReadingStatistic("2026-07-04", charactersRead = 200, readingTime = 7_000, mangaId = 7)),
            updated.statistics,
        )
    }

    @Test
    fun addStatsReturnsUnchangedWhenNothingWasRead() {
        val local = listOf(MangaReadingStatistic("2026-07-04", charactersRead = 10, readingTime = 500, mangaId = 7))
        val result = MangaReadingStatisticsPolicy.addStats(
            statistics = local,
            dateKey = "2026-07-04",
            characters = 0,
            timeMs = 0,
            mangaId = 7,
        )

        assertFalse(result.changed)
        assertEquals(local, result.statistics)
    }

    @Test
    fun mergeKeepsHigherValuesPerDateAndManga() {
        val result = MangaReadingStatisticsPolicy.merge(
            local = listOf(
                MangaReadingStatistic("2026-07-04", charactersRead = 100, readingTime = 5_000, mangaId = 7),
                MangaReadingStatistic("2026-07-04", charactersRead = 20, readingTime = 1_000, mangaId = 8),
            ),
            incoming = listOf(
                MangaReadingStatistic("2026-07-04", charactersRead = 80, readingTime = 6_000, mangaId = 7),
                MangaReadingStatistic("2026-07-05", charactersRead = 30, readingTime = 2_000, mangaId = 7),
            ),
        )

        assertTrue(result.changed)
        assertEquals(
            listOf(
                MangaReadingStatistic("2026-07-04", charactersRead = 100, readingTime = 6_000, mangaId = 7),
                MangaReadingStatistic("2026-07-04", charactersRead = 20, readingTime = 1_000, mangaId = 8),
                MangaReadingStatistic("2026-07-05", charactersRead = 30, readingTime = 2_000, mangaId = 7),
            ),
            result.statistics,
        )
    }
}
