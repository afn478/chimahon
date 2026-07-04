package tachiyomi.domain.reader.service

import kotlin.math.abs
import tachiyomi.domain.reader.model.NovelReaderSasayakiMatch
import tachiyomi.domain.reader.model.NovelReaderSasayakiMatchData

class NovelReaderSasayakiCueTimeline(
    matchData: NovelReaderSasayakiMatchData?,
) {
    private val cues: List<NovelReaderSasayakiMatch> = matchData?.matches ?: emptyList()

    fun nextCue(afterTime: Double): Double? {
        var index = findCue(afterTime)
        if (index < cues.size && cues[index].startTime == afterTime) {
            index++
        }
        return if (index < cues.size) cues[index].startTime else null
    }

    fun prevCue(beforeTime: Double): Double? {
        val index = findCue(beforeTime)
        return if (index > 0) cues[index - 1].startTime else null
    }

    fun cueAt(time: Double): NovelReaderSasayakiMatch? {
        val index = findCue(time)
        if (index < cues.size && abs(cues[index].startTime - time) <= START_TIME_TOLERANCE_SECONDS) {
            return cues[index]
        }
        if (index == 0) return null
        val cue = cues[index - 1]
        return if (time <= cue.endTime) cue else null
    }

    private fun findCue(time: Double): Int {
        var low = 0
        var high = cues.size
        while (low < high) {
            val mid = (low + high) / 2
            if (cues[mid].startTime < time) {
                low = mid + 1
            } else {
                high = mid
            }
        }
        return low
    }

    private companion object {
        const val START_TIME_TOLERANCE_SECONDS = 0.01
    }
}
