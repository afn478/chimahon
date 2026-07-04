package mihon.domain.upcoming.service

import mihon.domain.upcoming.model.UpcomingMangaCandidate
import tachiyomi.domain.library.service.LibraryUpdateCategoryPolicy

object UpcomingMangaSelectionPolicy {
    fun selectMangaIdsForUpdate(
        candidates: List<UpcomingMangaCandidate>,
        includedCategoryIds: Set<Long>,
        excludedCategoryIds: Set<Long>,
        restrictions: Set<String>,
        today: Long,
    ): List<Long> {
        return candidates.asSequence()
            .filter {
                LibraryUpdateCategoryPolicy.shouldInclude(
                    categoryIds = it.categoryIds,
                    includedCategoryIds = includedCategoryIds,
                    excludedCategoryIds = excludedCategoryIds,
                )
            }
            .distinctBy { it.mangaId }
            .filter {
                UpcomingMangaUpdatePolicy.shouldUpdate(
                    updateStrategy = it.updateStrategy,
                    status = it.status,
                    nextUpdate = it.nextUpdate,
                    totalChapters = it.totalChapters,
                    unreadCount = it.unreadCount,
                    hasStarted = it.hasStarted,
                    restrictions = restrictions,
                    today = today,
                )
            }
            .sortedBy { it.nextUpdate }
            .map { it.mangaId }
            .toList()
    }
}
