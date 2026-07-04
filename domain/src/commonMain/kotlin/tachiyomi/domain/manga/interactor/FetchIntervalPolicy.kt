package tachiyomi.domain.manga.interactor

import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.MangaUpdate
import kotlin.math.absoluteValue

object FetchIntervalPolicy {
    const val MAX_INTERVAL_DAYS = 28
    const val MANUAL_DISABLE = 99999
    const val DEFAULT_INCREASE_WHEN_OVER = 10
    const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L

    fun calculateInterval(
        chapters: List<Chapter>,
        localEpochDay: (timestampMillis: Long) -> Long,
    ): Int {
        val chapterWindow = if (chapters.size <= 8) 3 else 10

        val uploadDates = chapters.asSequence()
            .filter { it.dateUpload > 0L }
            .sortedByDescending { it.dateUpload }
            .map { localEpochDay(it.dateUpload) }
            .distinct()
            .take(chapterWindow)
            .toList()

        val fetchDates = chapters.asSequence()
            .sortedByDescending { it.dateFetch }
            .map { localEpochDay(it.dateFetch) }
            .distinct()
            .take(chapterWindow)
            .toList()

        val interval = intervalFromDays(uploadDates)
            ?: intervalFromDays(fetchDates)
            ?: 7

        return coerceInterval(interval)
    }

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

    fun mangaUpdate(
        mangaId: Long,
        currentNextUpdate: Long,
        latestUpdateEpochDay: Long,
        todayEpochDay: Long,
        currentOffsetMillis: Long,
        intervalDays: Int,
        window: Pair<Long, Long>,
    ): MangaUpdate {
        return MangaUpdate(
            id = mangaId,
            nextUpdate = calculateNextUpdate(
                currentNextUpdate = currentNextUpdate,
                latestUpdateEpochDay = latestUpdateEpochDay,
                todayEpochDay = todayEpochDay,
                currentOffsetMillis = currentOffsetMillis,
                intervalDays = intervalDays,
                window = window,
            ),
            fetchInterval = intervalDays,
        )
    }

    fun calculateNextUpdate(
        currentNextUpdate: Long,
        latestUpdateEpochDay: Long,
        todayEpochDay: Long,
        currentOffsetMillis: Long,
        intervalDays: Int,
        window: Pair<Long, Long>,
    ): Long {
        if (currentNextUpdate in window.first.rangeTo(window.second + 1)) {
            return currentNextUpdate
        }

        val timeSinceLatestDays = (todayEpochDay - latestUpdateEpochDay).toInt()
        val cycle = timeSinceLatestDays.floorDiv(
            intervalDays.absoluteValue.takeIf { intervalDays < 0 }
                ?: increaseIntervalWhenOverdue(intervalDays, timeSinceLatestDays),
        )
        val nextUpdateEpochDay = latestUpdateEpochDay + (cycle + 1) * intervalDays.absoluteValue.toLong()
        return nextUpdateEpochDay * MILLIS_PER_DAY - currentOffsetMillis
    }

    private fun intervalFromDays(daysDescending: List<Long>): Int? {
        if (daysDescending.size < 3) return null

        val ranges = daysDescending
            .windowed(2)
            .map { (newer, older) -> newer - older }
            .sorted()

        return ranges[(ranges.size - 1) / 2].toInt()
    }
}
