package tachiyomi.domain.manga.interactor

object FetchIntervalPolicy {
    const val MAX_INTERVAL_DAYS = 28
    const val MANUAL_DISABLE = 99999
    const val DEFAULT_INCREASE_WHEN_OVER = 10

    fun coerceInterval(intervalDays: Int): Int {
        return intervalDays.coerceIn(1, MAX_INTERVAL_DAYS)
    }

    fun increaseIntervalWhenOverdue(
        intervalDays: Int,
        timeSinceLatestDays: Int,
        increaseWhenOver: Int = DEFAULT_INCREASE_WHEN_OVER,
    ): Int {
        if (intervalDays >= MAX_INTERVAL_DAYS) return MAX_INTERVAL_DAYS

        val cycle = timeSinceLatestDays.floorDiv(intervalDays) + 1
        return if (cycle > increaseWhenOver) {
            increaseIntervalWhenOverdue(intervalDays * 2, timeSinceLatestDays, increaseWhenOver)
        } else {
            intervalDays
        }
    }
}
