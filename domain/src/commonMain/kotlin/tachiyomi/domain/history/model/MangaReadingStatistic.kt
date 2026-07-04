package tachiyomi.domain.history.model

import kotlinx.serialization.Serializable

@Serializable
data class MangaReadingStatistic(
    val dateKey: String,
    var charactersRead: Int = 0,
    var readingTime: Long = 0,
    var mangaId: Long = 0,
)
