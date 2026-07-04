package mihon.domain.upcoming.interactor

import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.flow.Flow
import mihon.domain.upcoming.service.UpcomingMangaUpdatePolicy
import tachiyomi.domain.library.service.LibraryUpdateCategoryPolicy
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

        val listToUpdate = libraryManga.filter {
            LibraryUpdateCategoryPolicy.shouldInclude(
                categoryIds = it.categories,
                includedCategoryIds = includedCategories,
                excludedCategoryIds = excludedCategories,
            )
        }

        val restrictions = libraryPreferences.autoUpdateMangaRestrictions().get()
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000

        return listToUpdate
            .distinctBy { it.manga.id }
            .filter {
                UpcomingMangaUpdatePolicy.shouldUpdate(
                    updateStrategy = it.manga.updateStrategy,
                    status = it.manga.status,
                    nextUpdate = it.manga.nextUpdate,
                    totalChapters = it.totalChapters,
                    unreadCount = it.unreadCount,
                    hasStarted = it.hasStarted,
                    restrictions = restrictions,
                    today = today,
                )
            }
            .map { it.manga }
            .sortedBy { it.nextUpdate }
    }
    // KMK <--
}
