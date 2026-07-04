package tachiyomi.domain.history.service

import tachiyomi.domain.history.model.AnkiCardStatistic

object AnkiCardStatisticsPolicy {
    data class MergeResult(
        val statistics: List<AnkiCardStatistic>,
        val changed: Boolean,
    )

    fun addCard(
        statistics: List<AnkiCardStatistic>,
        dateKey: String,
        type: String?,
    ): List<AnkiCardStatistic> {
        val next = statistics.map { it.copy() }.toMutableList()
        val existing = next.find { it.dateKey == dateKey }

        if (existing != null) {
            existing.increment(type)
        } else {
            next.add(
                AnkiCardStatistic(dateKey = dateKey).apply {
                    increment(type)
                },
            )
        }

        return next
    }

    fun merge(
        local: List<AnkiCardStatistic>,
        incoming: List<AnkiCardStatistic>,
    ): MergeResult {
        if (incoming.isEmpty()) {
            return MergeResult(statistics = local, changed = false)
        }

        val merged = local.map { it.copy() }.toMutableList()
        var changed = false

        incoming.forEach { remote ->
            val existing = merged.find { it.dateKey == remote.dateKey }
            if (existing != null) {
                if (remote.mangaCards > existing.mangaCards) {
                    existing.mangaCards = remote.mangaCards
                    changed = true
                }
                if (remote.novelCards > existing.novelCards) {
                    existing.novelCards = remote.novelCards
                    changed = true
                }
            } else {
                merged.add(remote)
                changed = true
            }
        }

        return MergeResult(statistics = merged, changed = changed)
    }

    private fun AnkiCardStatistic.increment(type: String?) {
        if (type == "manga") {
            mangaCards++
        } else {
            novelCards++
        }
    }
}
