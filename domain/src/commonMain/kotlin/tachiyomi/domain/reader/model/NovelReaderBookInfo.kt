package tachiyomi.domain.reader.model

import kotlinx.serialization.Serializable

@Serializable
data class NovelReaderBookInfo(
    val characterCount: Int,
    val chapterInfo: Map<String, NovelReaderChapterInfo>,
) {
    fun resolveCharacterPosition(charCount: Int): Pair<Int, Double>? {
        val clamped = maxOf(0, minOf(charCount, characterCount - 1))

        for (chapter in chapterInfo.values) {
            val spineIndex = chapter.spineIndex ?: continue
            if (chapter.chapterCount <= 0) continue

            val start = chapter.currentTotal
            val end = start + chapter.chapterCount

            if (clamped >= start && clamped < end) {
                val progress = (clamped - start).toDouble() / chapter.chapterCount
                return Pair(spineIndex, progress)
            }
        }
        return null
    }
}
