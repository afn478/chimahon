package tachiyomi.domain.reader.service

import kotlin.math.abs
import tachiyomi.domain.reader.model.NovelReaderChapterProgress

object NovelReaderProgressPolicy {
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
}
