package tachiyomi.domain.history.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import tachiyomi.domain.history.model.AnkiCardStatistic

class AnkiCardStatisticsPolicyTest {
    @Test
    fun addCardCreatesOrUpdatesDailyMangaAndNovelCounts() {
        val created = AnkiCardStatisticsPolicy.addCard(
            statistics = emptyList(),
            dateKey = "2026-07-04",
            type = "manga",
        )
        val updated = AnkiCardStatisticsPolicy.addCard(
            statistics = created,
            dateKey = "2026-07-04",
            type = null,
        )

        assertEquals(listOf(AnkiCardStatistic("2026-07-04", mangaCards = 1, novelCards = 1)), updated)
    }

    @Test
    fun mergeKeepsHigherCardCountsPerDate() {
        val result = AnkiCardStatisticsPolicy.merge(
            local = listOf(
                AnkiCardStatistic("2026-07-04", mangaCards = 2, novelCards = 5),
                AnkiCardStatistic("2026-07-05", mangaCards = 1, novelCards = 0),
            ),
            incoming = listOf(
                AnkiCardStatistic("2026-07-04", mangaCards = 4, novelCards = 3),
                AnkiCardStatistic("2026-07-06", mangaCards = 0, novelCards = 2),
            ),
        )

        assertTrue(result.changed)
        assertEquals(
            listOf(
                AnkiCardStatistic("2026-07-04", mangaCards = 4, novelCards = 5),
                AnkiCardStatistic("2026-07-05", mangaCards = 1, novelCards = 0),
                AnkiCardStatistic("2026-07-06", mangaCards = 0, novelCards = 2),
            ),
            result.statistics,
        )
    }

    @Test
    fun mergeReportsUnchangedWhenIncomingAddsNoHigherCounts() {
        val local = listOf(AnkiCardStatistic("2026-07-04", mangaCards = 2, novelCards = 5))
        val result = AnkiCardStatisticsPolicy.merge(
            local = local,
            incoming = listOf(AnkiCardStatistic("2026-07-04", mangaCards = 1, novelCards = 5)),
        )

        assertFalse(result.changed)
        assertEquals(local, result.statistics)
    }
}
