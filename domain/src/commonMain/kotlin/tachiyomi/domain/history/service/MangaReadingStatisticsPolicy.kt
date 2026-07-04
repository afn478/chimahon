package tachiyomi.domain.history.service

import tachiyomi.domain.history.model.MangaReadingStatistic

object MangaReadingStatisticsPolicy {
    data class MergeResult(
        val statistics: List<MangaReadingStatistic>,
        val changed: Boolean,
    )

    fun addStats(
        statistics: List<MangaReadingStatistic>,
        dateKey: String,
        characters: Int,
        timeMs: Long,
        mangaId: Long,
    ): MergeResult {
        if (characters <= 0 && timeMs <= 0) {
            return MergeResult(statistics = statistics, changed = false)
        }

        val next = statistics.map { it.copy() }.toMutableList()
        val existing = next.find { it.dateKey == dateKey && it.mangaId == mangaId }
        if (existing != null) {
            existing.charactersRead += characters
            existing.readingTime += timeMs
        } else {
            next.add(
                MangaReadingStatistic(
                    dateKey = dateKey,
                    charactersRead = characters,
                    readingTime = timeMs,
                    mangaId = mangaId,
                ),
            )
        }

        return MergeResult(statistics = next, changed = true)
    }

    fun merge(
        local: List<MangaReadingStatistic>,
        incoming: List<MangaReadingStatistic>,
    ): MergeResult {
        if (incoming.isEmpty()) {
            return MergeResult(statistics = local, changed = false)
        }

        val merged = local.map { it.copy() }.toMutableList()
        var changed = false

        incoming.forEach { remote ->
            val existing = merged.find { it.dateKey == remote.dateKey && it.mangaId == remote.mangaId }
            if (existing != null) {
                if (remote.charactersRead > existing.charactersRead) {
                    existing.charactersRead = remote.charactersRead
                    changed = true
                }
                if (remote.readingTime > existing.readingTime) {
                    existing.readingTime = remote.readingTime
                    changed = true
                }
            } else {
                merged.add(remote)
                changed = true
            }
        }

        return MergeResult(statistics = merged, changed = changed)
    }
}
