package com.canopus.chimareader.data

import android.content.Context
import tachiyomi.domain.history.service.MangaReadingStatisticsPolicy
import java.io.File
import java.time.LocalDate

object MangaStatsStorage {

    private fun getMangaStatsFile(context: Context): File {
        return File(context.filesDir, FileNames.mangaStats)
    }

    fun loadAll(context: Context): List<MangaStats> {
        val file = getMangaStatsFile(context)
        if (!file.exists()) return emptyList()
        return BookStorage.load<List<MangaStats>>(context.filesDir, FileNames.mangaStats) ?: emptyList()
    }

    fun saveAll(context: Context, stats: List<MangaStats>) {
        BookStorage.save(stats, context.filesDir, FileNames.mangaStats)
    }

    fun addStats(context: Context, characters: Int, timeMs: Long, mangaId: Long = 0, date: LocalDate = LocalDate.now()) {
        val dateKey = date.toString()
        val result = MangaReadingStatisticsPolicy.addStats(
            statistics = loadAll(context),
            dateKey = dateKey,
            characters = characters,
            timeMs = timeMs,
            mangaId = mangaId,
        )
        if (result.changed) saveAll(context, result.statistics)
    }

    fun merge(context: Context, incoming: List<MangaStats>) {
        val result = MangaReadingStatisticsPolicy.merge(
            local = loadAll(context),
            incoming = incoming,
        )
        if (result.changed) saveAll(context, result.statistics)
    }
}
