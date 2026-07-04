package com.canopus.chimareader.ui.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import tachiyomi.domain.reader.model.NovelReadingStatistic
import tachiyomi.domain.reader.service.NovelReaderStatisticsPolicy

data class ReaderStatisticsState(
    val isTracking: Boolean,
    val session: NovelReadingStatistic,
    val today: NovelReadingStatistic,
    val allTime: NovelReadingStatistic,
)

class ReaderStatisticsTracker(
    private val title: String,
    initialStatistics: List<NovelReadingStatistic>,
    private val enabled: Boolean,
) {
    private var statistics = initialStatistics.toMutableList()
    private var lastTimestampMillis: Long = System.currentTimeMillis()
    private var lastCharacterCount: Int = 0
    val frozenPosition: Int get() = lastCharacterCount
    private var hasUpdated = false

    var state: ReaderStatisticsState by mutableStateOf(
        ReaderStatisticsState(
            isTracking = false,
            session = defaultStatistic(),
            today = statisticForDate(currentDateKey()),
            allTime = allTimeStatistic(statistics),
        )
    )
        private set

    fun start(currentCharacter: Int) {
        if (!enabled) return
        state = state.copy(isTracking = true)
        resetBaseline(currentCharacter)
    }

    fun startForPageTurnIfNeeded(currentCharacter: Int) {
        if (!state.isTracking) {
            start(currentCharacter)
        }
    }

    fun stop(currentCharacter: Int) {
        pause(currentCharacter)
    }

    fun pause(currentCharacter: Int): Boolean {
        if (!state.isTracking) return false
        update(currentCharacter)
        state = state.copy(isTracking = false)
        return true
    }

    fun togglePause(currentCharacter: Int) {
        if (state.isTracking) {
            pause(currentCharacter)
        } else {
            start(currentCharacter)
        }
    }

    fun update(currentCharacter: Int) {
        if (!enabled || !state.isTracking) return
        rollTodayIfNeeded()
        val now = System.currentTimeMillis()
        val timeDiff = (now - lastTimestampMillis).toDouble() / 1000.0
        if (timeDiff <= 0.0) return

        val charDiff = currentCharacter - lastCharacterCount
        val finalCharDiff = NovelReaderStatisticsPolicy.clampBackwardCharacterDiff(
            characterDiff = charDiff,
            sessionCharactersRead = state.session.charactersRead,
        )
        state = state.copy(
            session = updateStatistic(state.session, timeDiff, finalCharDiff, now),
            today = updateStatistic(state.today, timeDiff, finalCharDiff, now),
            allTime = updateStatistic(state.allTime, timeDiff, finalCharDiff, now),
        )
        hasUpdated = true
        lastTimestampMillis = now
        lastCharacterCount = currentCharacter
    }

    fun resetBaseline(currentCharacter: Int) {
        lastCharacterCount = currentCharacter
        lastTimestampMillis = System.currentTimeMillis()
    }

    fun statisticsForPersistenceOrNull(): List<NovelReadingStatistic>? =
        if (enabled && (hasUpdated || statistics.isNotEmpty())) statisticsForPersistence() else null

    fun statisticsForPersistence(): List<NovelReadingStatistic> {
        val today = state.today
        val next = NovelReaderStatisticsPolicy.statisticsForPersistence(
            existing = statistics,
            today = today,
        )
        statistics = next.toMutableList()
        return next
    }

    private fun rollTodayIfNeeded() {
        val key = currentDateKey()
        if (state.today.dateKey == key) return
        statisticsForPersistence()
        state = state.copy(today = statisticForDate(key))
    }

    private fun statisticForDate(dateKey: String): NovelReadingStatistic {
        return NovelReaderStatisticsPolicy.statisticForDate(
            statistics = statistics,
            title = title,
            dateKey = dateKey,
        )
    }

    private fun defaultStatistic(dateKey: String = currentDateKey()): NovelReadingStatistic =
        NovelReaderStatisticsPolicy.defaultStatistic(title = title, dateKey = dateKey)

    private fun allTimeStatistic(statistics: List<NovelReadingStatistic>): NovelReadingStatistic {
        return NovelReaderStatisticsPolicy.allTimeStatistic(
            title = title,
            dateKey = currentDateKey(),
            statistics = statistics,
        )
    }

    private fun updateStatistic(
        statistic: NovelReadingStatistic,
        timeDiff: Double,
        characterDiff: Int,
        modifiedAt: Long,
    ): NovelReadingStatistic {
        return NovelReaderStatisticsPolicy.updateStatistic(
            statistic = statistic,
            timeDiffSeconds = timeDiff,
            characterDiff = characterDiff,
            lastStatisticModified = modifiedAt,
        )
    }

    private fun currentDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
