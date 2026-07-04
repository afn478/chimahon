package mihon.domain.upcoming.model

import eu.kanade.tachiyomi.source.model.UpdateStrategy

data class UpcomingMangaCandidate(
    val mangaId: Long,
    val categoryIds: List<Long>,
    val updateStrategy: UpdateStrategy,
    val status: Long,
    val nextUpdate: Long,
    val totalChapters: Long,
    val unreadCount: Long,
    val hasStarted: Boolean,
)
