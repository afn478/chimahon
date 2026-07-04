package tachiyomi.domain.reader.service

import kotlin.math.abs
import tachiyomi.domain.reader.model.NovelReaderChapterProgress

object NovelReaderProgressPolicy {
    const val READER_AUTO_PERSIST_INTERVAL_MS = 60_000L
    const val WEB_SCROLL_PROGRESS_REPORT_INTERVAL_MS = 1000L

    sealed interface ScrollProgressReportAction {
        data object Ignore : ScrollProgressReportAction
        data class ReportNow(val reportTimeMillis: Long) : ScrollProgressReportAction
        data class ScheduleDelayed(val delayMillis: Long) : ScrollProgressReportAction
    }

    sealed interface WebProgressRequestAction {
        data object Ignore : WebProgressRequestAction
        data class EvaluateScript(val script: String) : WebProgressRequestAction
    }

    sealed interface WebProgressResultAction {
        data object Ignore : WebProgressResultAction
        data class ReportProgress(val progress: Double) : WebProgressResultAction
    }

    fun accumulatedCharacterCounts(chapterCharacterCounts: List<Int>): List<Int> {
        val accumulated = mutableListOf<Int>()
        var runningTotal = 0

        chapterCharacterCounts.forEach { chapterCharacters ->
            accumulated += runningTotal
            runningTotal += chapterCharacters
        }

        accumulated += runningTotal
        return accumulated
    }

    fun exploredCharacterCount(
        chapterIndex: Int,
        progress: Double,
        chapterCharacterCount: (Int) -> Int,
    ): Int {
        var count = 0
        for (index in 0 until chapterIndex) {
            count += chapterCharacterCount(index)
        }

        val currentChapterCharacters = chapterCharacterCount(chapterIndex)
        return count + (currentChapterCharacters * progress).toInt()
    }

    fun chapterProgressForCharacterCount(
        totalExploredCharacters: Int,
        chapterCharacterCounts: List<Int>,
    ): NovelReaderChapterProgress {
        var remainingCharacters = totalExploredCharacters

        chapterCharacterCounts.forEachIndexed { index, chapterCharacters ->
            if (remainingCharacters <= chapterCharacters) {
                val progress = if (chapterCharacters == 0) {
                    0.0
                } else {
                    remainingCharacters.toDouble() / chapterCharacters.toDouble()
                }
                return NovelReaderChapterProgress(
                    chapterIndex = index,
                    progress = progress.coerceIn(0.0, 1.0),
                )
            }
            remainingCharacters -= chapterCharacters
        }

        return NovelReaderChapterProgress(
            chapterIndex = maxOf(0, chapterCharacterCounts.size - 1),
            progress = 1.0,
        )
    }

    fun shouldPersistBookmark(
        force: Boolean,
        chapterIndex: Int,
        progress: Double,
        characterCount: Int,
        lastChapterIndex: Int,
        lastProgress: Double,
        lastCharacterCount: Int,
        progressEpsilon: Double,
    ): Boolean {
        return force ||
            chapterIndex != lastChapterIndex ||
            characterCount != lastCharacterCount ||
            abs(progress - lastProgress) > progressEpsilon
    }

    fun shouldPersistPeriodically(
        nowMillis: Long,
        lastPersistTimeMillis: Long,
        intervalMillis: Long = READER_AUTO_PERSIST_INTERVAL_MS,
    ): Boolean {
        return nowMillis - lastPersistTimeMillis >= maxOf(0L, intervalMillis)
    }

    fun scrollProgressReportAction(
        continuousMode: Boolean,
        imageOnly: Boolean,
        nowMillis: Long,
        lastReportMillis: Long,
        reportIntervalMillis: Long = WEB_SCROLL_PROGRESS_REPORT_INTERVAL_MS,
    ): ScrollProgressReportAction {
        if (!continuousMode || imageOnly) return ScrollProgressReportAction.Ignore

        val intervalMillis = maxOf(0L, reportIntervalMillis)
        return if (nowMillis - lastReportMillis > intervalMillis) {
            ScrollProgressReportAction.ReportNow(nowMillis)
        } else {
            ScrollProgressReportAction.ScheduleDelayed(intervalMillis)
        }
    }

    fun webProgressRequestAction(
        continuousMode: Boolean,
        imageOnly: Boolean,
    ): WebProgressRequestAction {
        return if (continuousMode && !imageOnly) {
            WebProgressRequestAction.EvaluateScript(
                NovelReaderWebScriptPolicy.calculateProgressScript(),
            )
        } else {
            WebProgressRequestAction.Ignore
        }
    }

    fun webProgressResultAction(result: String?): WebProgressResultAction {
        return NovelReaderWebResultPolicy.progressResult(result)?.let { progress ->
            WebProgressResultAction.ReportProgress(progress)
        } ?: WebProgressResultAction.Ignore
    }
}
