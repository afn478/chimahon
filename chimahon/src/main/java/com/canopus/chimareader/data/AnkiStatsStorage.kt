package com.canopus.chimareader.data

import android.content.Context
import tachiyomi.domain.history.service.AnkiCardStatisticsPolicy
import java.io.File
import java.time.LocalDate

object AnkiStatsStorage {

    private fun getAnkiStatsFile(context: Context): File {
        return File(context.filesDir, FileNames.ankiStats)
    }

    fun loadAll(context: Context): List<AnkiStats> {
        val file = getAnkiStatsFile(context)
        if (!file.exists()) return emptyList()
        return BookStorage.load<List<AnkiStats>>(context.filesDir, FileNames.ankiStats) ?: emptyList()
    }

    fun saveAll(context: Context, stats: List<AnkiStats>) {
        BookStorage.save(stats, context.filesDir, FileNames.ankiStats)
    }

    fun addCard(context: Context, type: String? = null, date: LocalDate = LocalDate.now()) {
        val dateKey = date.toString()
        val nextStats = AnkiCardStatisticsPolicy.addCard(
            statistics = loadAll(context),
            dateKey = dateKey,
            type = type,
        )
        saveAll(context, nextStats)
    }

    fun merge(context: Context, incoming: List<AnkiStats>) {
        val result = AnkiCardStatisticsPolicy.merge(
            local = loadAll(context),
            incoming = incoming,
        )
        if (result.changed) saveAll(context, result.statistics)
    }
}
