package tachiyomi.domain.reader.model

data class NovelReaderStatisticsState(
    val isTracking: Boolean,
    val session: NovelReadingStatistic,
    val today: NovelReadingStatistic,
    val allTime: NovelReadingStatistic,
)
