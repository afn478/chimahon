package com.canopus.chimareader.ui.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import tachiyomi.domain.reader.model.NovelReaderStatisticsState
import tachiyomi.domain.reader.model.NovelReadingStatistic
import tachiyomi.domain.reader.service.NovelReaderStatisticsPolicy

class ReaderStatisticsTracker(
    private val title: String,
    initialStatistics: List<NovelReadingStatistic>,
    private val enabled: Boolean,
    private val nowMillis: () -> Long,
    private val dateKeyProvider: () -> String,
) {
    private var statistics = initialStatistics.toMutableList()
    private var lastTimestampMillis: Long = nowMillis()
    private var lastCharacterCount: Int = 0
    val frozenPosition: Int get() = lastCharacterCount
    private var hasUpdated = false
    private val initialDateKey = dateKeyProvider()

    var state: NovelReaderStatisticsState by mutableStateOf(
        NovelReaderStatisticsPolicy.initialState(
            title = title,
            dateKey = initialDateKey,
            statistics = statistics,
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
        val now = nowMillis()
        val timeDiff = (now - lastTimestampMillis).toDouble() / 1000.0
        if (timeDiff <= 0.0) return

        val charDiff = currentCharacter - lastCharacterCount
        state = NovelReaderStatisticsPolicy.stateAfterTick(
            state = state,
            timeDiffSeconds = timeDiff,
            characterDiff = charDiff,
            lastStatisticModified = now,
        )
        hasUpdated = true
        lastTimestampMillis = now
        lastCharacterCount = currentCharacter
    }

    fun resetBaseline(currentCharacter: Int) {
        lastCharacterCount = currentCharacter
        lastTimestampMillis = nowMillis()
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
        val key = dateKeyProvider()
        if (!NovelReaderStatisticsPolicy.shouldRollDate(state, key)) return
        statisticsForPersistence()
        state = NovelReaderStatisticsPolicy.stateAfterDateRoll(
            state = state,
            statistics = statistics,
            title = title,
            dateKey = key,
        )
    }
}
