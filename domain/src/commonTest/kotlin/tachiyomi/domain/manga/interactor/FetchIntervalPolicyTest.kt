package tachiyomi.domain.manga.interactor

import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.MangaUpdate
import kotlin.test.Test
import kotlin.test.assertEquals

class FetchIntervalPolicyTest {
    @Test
    fun fetchIntervalConstantsRemainStable() {
        assertEquals(28, FetchIntervalPolicy.MAX_INTERVAL_DAYS)
        assertEquals(99999, FetchIntervalPolicy.MANUAL_DISABLE)
        assertEquals(10, FetchIntervalPolicy.DEFAULT_INCREASE_WHEN_OVER)
        assertEquals(MILLIS_PER_DAY, FetchIntervalPolicy.MILLIS_PER_DAY)
    }

    @Test
    fun intervalsAreCoercedIntoSupportedRange() {
        assertEquals(1, FetchIntervalPolicy.coerceInterval(0))
        assertEquals(7, FetchIntervalPolicy.coerceInterval(7))
        assertEquals(28, FetchIntervalPolicy.coerceInterval(60))
    }

    @Test
    fun overdueIntervalsIncreaseUntilWithinCycleLimit() {
        assertEquals(
            7,
            FetchIntervalPolicy.increaseIntervalWhenOverdue(
                intervalDays = 7,
                timeSinceLatestDays = 69,
            ),
        )
        assertEquals(
            14,
            FetchIntervalPolicy.increaseIntervalWhenOverdue(
                intervalDays = 7,
                timeSinceLatestDays = 70,
            ),
        )
        assertEquals(
            28,
            FetchIntervalPolicy.increaseIntervalWhenOverdue(
                intervalDays = 7,
                timeSinceLatestDays = 500,
            ),
        )
    }

    @Test
    fun calculateNextUpdateKeepsExistingUpdateInsideWindow() {
        val nextUpdate = FetchIntervalPolicy.calculateNextUpdate(
            currentNextUpdate = 150L,
            latestUpdateEpochDay = 1L,
            todayEpochDay = 10L,
            currentOffsetMillis = 0L,
            intervalDays = 7,
            window = 100L to 200L,
        )

        assertEquals(150L, nextUpdate)
    }

    @Test
    fun calculateNextUpdateUsesLatestUpdateDayAndCurrentOffset() {
        val nextUpdate = FetchIntervalPolicy.calculateNextUpdate(
            currentNextUpdate = 0L,
            latestUpdateEpochDay = 10L,
            todayEpochDay = 24L,
            currentOffsetMillis = 2 * MILLIS_PER_HOUR,
            intervalDays = 7,
            window = Long.MAX_VALUE - 2 to Long.MAX_VALUE - 1,
        )

        assertEquals(dayMillis(31) - 2 * MILLIS_PER_HOUR, nextUpdate)
    }

    @Test
    fun calculateNextUpdateUsesIncreasedIntervalOnlyForCycleCalculation() {
        val nextUpdate = FetchIntervalPolicy.calculateNextUpdate(
            currentNextUpdate = 0L,
            latestUpdateEpochDay = 0L,
            todayEpochDay = 70L,
            currentOffsetMillis = 0L,
            intervalDays = 7,
            window = Long.MAX_VALUE - 2 to Long.MAX_VALUE - 1,
        )

        assertEquals(dayMillis(42), nextUpdate)
    }

    @Test
    fun calculateNextUpdateUsesAbsoluteManualIntervalWithoutOverdueIncrease() {
        val nextUpdate = FetchIntervalPolicy.calculateNextUpdate(
            currentNextUpdate = 0L,
            latestUpdateEpochDay = 0L,
            todayEpochDay = 70L,
            currentOffsetMillis = 0L,
            intervalDays = -7,
            window = Long.MAX_VALUE - 2 to Long.MAX_VALUE - 1,
        )

        assertEquals(dayMillis(77), nextUpdate)
    }

    @Test
    fun mangaUpdateCarriesNextUpdateAndFetchInterval() {
        val update = FetchIntervalPolicy.mangaUpdate(
            mangaId = MANGA_ID,
            currentNextUpdate = 0L,
            latestUpdateEpochDay = 1L,
            todayEpochDay = 8L,
            currentOffsetMillis = 0L,
            intervalDays = 7,
            window = Long.MAX_VALUE - 2 to Long.MAX_VALUE - 1,
        )

        assertEquals(
            MangaUpdate(
                id = MANGA_ID,
                nextUpdate = dayMillis(15),
                fetchInterval = 7,
            ),
            update,
        )
    }

    @Test
    fun calculateIntervalDefaultsToSevenDaysWithoutEnoughDistinctDays() {
        val chaptersWithUploadDate = (1..50).map {
            chapterWithTime(days = 1)
        }
        assertEquals(7, calculateInterval(chaptersWithUploadDate))

        val chaptersWithoutUploadDate = chaptersWithUploadDate.map {
            it.copy(dateUpload = 0L)
        }
        assertEquals(7, calculateInterval(chaptersWithoutUploadDate))
    }

    @Test
    fun calculateIntervalUsesRecentChapterSubset() {
        val oldChapters = (1..5).map {
            chapterWithTime(days = it * 7)
        }
        val newChapters = (1..10).map {
            chapterWithTime(timestampMillis = oldChapters.last().dateUpload + dayMillis(it))
        }

        assertEquals(1, calculateInterval(oldChapters + newChapters))
    }

    @Test
    fun calculateIntervalUsesSmallerRecentSubsetForFewChapters() {
        val oldChapters = (1..3).map {
            chapterWithTime(days = it * 7)
        }
        val newChapters = (1..3).map {
            chapterWithTime(timestampMillis = oldChapters.last().dateUpload + dayMillis(365 + it * 7))
        }

        assertEquals(7, calculateInterval(oldChapters + newChapters))
    }

    @Test
    fun calculateIntervalDefaultsWhenChaptersCoverOnlyOneOrTwoDays() {
        val sameDayChapters = (1..10).map {
            chapterWithTime(hours = 10)
        }
        assertEquals(7, calculateInterval(sameDayChapters))

        val twoDayChapters = (1..2).map {
            chapterWithTime(days = 1)
        } + (1..5).map {
            chapterWithTime(days = 2)
        }
        assertEquals(7, calculateInterval(twoDayChapters))
    }

    @Test
    fun calculateIntervalUsesMedianDistinctDayGap() {
        assertEquals(
            1,
            calculateInterval(
                (1..20).map {
                    chapterWithTime(days = it)
                },
            ),
        )
        assertEquals(
            1,
            calculateInterval(
                (1..20).map {
                    chapterWithTime(hours = 15 * it)
                },
            ),
        )
        assertEquals(
            2,
            calculateInterval(
                (1..20).map {
                    chapterWithTime(days = 2 * it)
                },
            ),
        )
        assertEquals(
            2,
            calculateInterval(
                (1..20).map {
                    chapterWithTime(hours = 43 * it)
                },
            ),
        )
    }

    @Test
    fun calculateIntervalFallsBackToFetchDateWhenUploadDateIsMissing() {
        val chaptersWithoutUploadDate = (1..5).map {
            chapterWithTime(hours = 25 * it).copy(dateUpload = 0L)
        }

        assertEquals(1, calculateInterval(chaptersWithoutUploadDate))
    }

    private fun calculateInterval(chapters: List<Chapter>): Int {
        return FetchIntervalPolicy.calculateInterval(chapters) { timestampMillis ->
            timestampMillis / MILLIS_PER_DAY
        }
    }

    private fun chapterWithTime(
        days: Int? = null,
        hours: Int? = null,
        timestampMillis: Long = when {
            days != null -> dayMillis(days)
            hours != null -> hourMillis(hours)
            else -> error("Either days, hours, or timestampMillis must be provided")
        },
    ): Chapter {
        return Chapter.create().copy(
            dateFetch = timestampMillis,
            dateUpload = timestampMillis,
        )
    }

    private fun dayMillis(days: Int) = days * MILLIS_PER_DAY

    private fun hourMillis(hours: Int) = hours * MILLIS_PER_HOUR

    private companion object {
        const val MANGA_ID = 42L
        const val MILLIS_PER_HOUR = 60 * 60 * 1000L
        const val MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR
    }
}
