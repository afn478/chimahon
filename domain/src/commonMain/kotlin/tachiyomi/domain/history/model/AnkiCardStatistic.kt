package tachiyomi.domain.history.model

import kotlinx.serialization.Serializable

@Serializable
data class AnkiCardStatistic(
    val dateKey: String,
    var mangaCards: Int = 0,
    var novelCards: Int = 0,
)
