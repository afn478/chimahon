package tachiyomi.domain.manga.interactor

import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class FetchInterval(
    private val getChaptersByMangaId: GetChaptersByMangaId,
) {

    suspend fun toMangaUpdate(
        manga: Manga,
        dateTime: ZonedDateTime,
        window: Pair<Long, Long>,
    ): MangaUpdate {
        val interval = manga.fetchInterval.takeIf { it < 0 } ?: calculateInterval(
            chapters = getChaptersByMangaId.await(manga.id, applyFilter = true),
            zone = dateTime.zone,
        )
        val currentWindow = if (window.first == 0L && window.second == 0L) {
            getWindow(ZonedDateTime.now())
        } else {
            window
        }

        return FetchIntervalPolicy.mangaUpdate(
            mangaId = manga.id,
            currentNextUpdate = manga.nextUpdate,
            latestUpdateEpochDay = localEpochDay(
                if (manga.lastUpdate > 0) manga.lastUpdate else Instant.now().toEpochMilli(),
                dateTime.zone,
            ),
            todayEpochDay = dateTime.toLocalDate().toEpochDay(),
            currentOffsetMillis = dateTime.offset.totalSeconds * 1000L,
            intervalDays = interval,
            window = currentWindow,
        )
    }

    fun getWindow(dateTime: ZonedDateTime): Pair<Long, Long> {
        val today = dateTime.toLocalDate().atStartOfDay(dateTime.zone)
        val lowerBound = today.minusDays(GRACE_PERIOD)
        val upperBound = today.plusDays(GRACE_PERIOD)
        return Pair(lowerBound.toEpochSecond() * 1000, upperBound.toEpochSecond() * 1000 - 1)
    }

    internal fun calculateInterval(chapters: List<Chapter>, zone: ZoneId): Int {
        return FetchIntervalPolicy.calculateInterval(chapters) { timestampMillis ->
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), zone)
                .toLocalDate()
                .toEpochDay()
        }
    }

    private fun localEpochDay(
        timestampMillis: Long,
        zone: ZoneId,
    ): Long {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), zone)
            .toLocalDate()
            .toEpochDay()
    }

    companion object {
        const val MAX_INTERVAL = FetchIntervalPolicy.MAX_INTERVAL_DAYS

        private const val GRACE_PERIOD = 1L

        // KMK -->
        const val MANUAL_DISABLE = FetchIntervalPolicy.MANUAL_DISABLE // 274 years in future
        // KMK <--
    }
}
