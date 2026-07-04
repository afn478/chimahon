package mihon.domain.upcoming.interactor

import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.flow.Flow
import mihon.domain.upcoming.model.UpcomingMangaCandidate
import mihon.domain.upcoming.service.UpcomingMangaSelectionPolicy
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate
import java.time.ZoneId

class GetUpcomingManga(
    private val mangaRepository: MangaRepository,
) {
    // KMK -->
    private val libraryPreferences: LibraryPreferences = Injekt.get()
    private val getLibraryManga: GetLibraryManga = Injekt.get()
    // KMK <--

    private val includedStatuses = setOf(
        SManga.ONGOING.toLong(),
        SManga.PUBLISHING_FINISHED.toLong(),
    )

    suspend fun subscribe(): Flow<List<Manga>> {
        return mangaRepository.getUpcomingManga(includedStatuses)
    }

    // KMK -->
    suspend fun updatingMangas(): List<Manga> {
        val libraryManga = getLibraryManga.await()

        val includedCategories = libraryPreferences.updateCategories().get().map { it.toLong() }.toSet()
        val excludedCategories = libraryPreferences.updateCategoriesExclude().get().map { it.toLong() }.toSet()
        val restrictions = libraryPreferences.autoUpdateMangaRestrictions().get()
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        val mangaById = libraryManga
            .map { it.manga }
            .distinctBy { it.id }
            .associateBy { it.id }

        return UpcomingMangaSelectionPolicy.selectMangaIdsForUpdate(
            candidates = libraryManga.map {
                UpcomingMangaCandidate(
                    mangaId = it.manga.id,
                    categoryIds = it.categories,
                    updateStrategy = it.manga.updateStrategy,
                    status = it.manga.status,
                    nextUpdate = it.manga.nextUpdate,
                    totalChapters = it.totalChapters,
                    unreadCount = it.unreadCount,
                    hasStarted = it.hasStarted,
                )
            },
            includedCategoryIds = includedCategories,
            excludedCategoryIds = excludedCategories,
            restrictions = restrictions,
            today = today,
        )
            .mapNotNull { mangaById[it] }
    }
    // KMK <--
}
