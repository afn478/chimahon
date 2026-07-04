package tachiyomi.domain.manga.interactor

import kotlin.test.Test
import kotlin.test.assertEquals

class FetchIntervalPolicyTest {
    @Test
    fun fetchIntervalConstantsRemainStable() {
        assertEquals(28, FetchIntervalPolicy.MAX_INTERVAL_DAYS)
        assertEquals(99999, FetchIntervalPolicy.MANUAL_DISABLE)
        assertEquals(10, FetchIntervalPolicy.DEFAULT_INCREASE_WHEN_OVER)
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
}
