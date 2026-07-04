package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.NovelReaderStatisticsState
import tachiyomi.domain.reader.model.NovelReadingStatistic

object NovelReaderStatisticsSheetPolicy {
    const val TITLE = "Statistics"
    const val SESSION_TITLE = "Session"
    const val TODAY_TITLE = "Today"
    const val ALL_TIME_TITLE = "All Time"

    data class SheetState(
        val title: String,
        val toggleContentDescription: String,
        val showPauseIcon: Boolean,
        val sections: List<Section>,
    )

    data class Section(
        val title: String,
        val rows: List<Row>,
    )

    data class Row(
        val label: String,
        val value: String,
    )

    fun sheetState(
        trackerState: NovelReaderStatisticsState,
        currentCharacter: Int,
        frozenPosition: Int,
        totalCharacters: Int,
        currentChapterEndCharacter: Int,
    ): SheetState {
        val projectionCharacter = if (trackerState.isTracking) {
            currentCharacter
        } else {
            frozenPosition
        }

        return SheetState(
            title = TITLE,
            toggleContentDescription = if (trackerState.isTracking) "Pause Timer" else "Resume Timer",
            showPauseIcon = trackerState.isTracking,
            sections = listOf(
                Section(
                    title = SESSION_TITLE,
                    rows = sessionRows(
                        statistic = trackerState.session,
                        projectionCharacter = projectionCharacter,
                        totalCharacters = totalCharacters,
                        currentChapterEndCharacter = currentChapterEndCharacter,
                    ),
                ),
                Section(
                    title = TODAY_TITLE,
                    rows = statisticRows(trackerState.today),
                ),
                Section(
                    title = ALL_TIME_TITLE,
                    rows = statisticRows(trackerState.allTime),
                ),
            ),
        )
    }

    private fun sessionRows(
        statistic: NovelReadingStatistic,
        projectionCharacter: Int,
        totalCharacters: Int,
        currentChapterEndCharacter: Int,
    ): List<Row> {
        return buildList {
            addAll(statisticRows(statistic))

            val bookTimeRemaining = NovelReaderStatisticsPolicy.secondsRemaining(
                remainingCharacters = totalCharacters - projectionCharacter,
                readingSpeedPerHour = statistic.lastReadingSpeed,
            )
            if (bookTimeRemaining > 0.0) {
                add(
                    Row(
                        label = "Time to finish Book",
                        value = NovelReaderStatisticsPolicy.remainingDurationLabel(bookTimeRemaining),
                    ),
                )
            }

            val chapterTimeRemaining = NovelReaderStatisticsPolicy.secondsRemaining(
                remainingCharacters = currentChapterEndCharacter - projectionCharacter,
                readingSpeedPerHour = statistic.lastReadingSpeed,
            )
            if (chapterTimeRemaining > 0.0) {
                add(
                    Row(
                        label = "Time to finish Chapter",
                        value = NovelReaderStatisticsPolicy.remainingDurationLabel(chapterTimeRemaining),
                    ),
                )
            }
        }
    }

    private fun statisticRows(statistic: NovelReadingStatistic): List<Row> {
        return listOf(
            Row("Characters Read", statistic.charactersRead.toString()),
            Row("Reading Speed", "${statistic.lastReadingSpeed} / h"),
            Row(
                "Reading Time",
                NovelReaderStatisticsPolicy.elapsedDurationLabel(statistic.readingTime.toLong()),
            ),
        )
    }
}
