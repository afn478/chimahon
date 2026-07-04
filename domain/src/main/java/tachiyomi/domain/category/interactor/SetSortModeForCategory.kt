package tachiyomi.domain.category.interactor

import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.category.service.CategorySortModePolicy
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.LibraryPreferences
import kotlin.random.Random

class SetSortModeForCategory(
    private val preferences: LibraryPreferences,
    private val categoryRepository: CategoryRepository,
) {

    suspend fun await(categoryId: Long?, type: LibrarySort.Type, direction: LibrarySort.Direction) {
        val groupLibraryMode = preferences.groupLibraryBy().get()
        val defaultGroupMode = groupLibraryMode == LibraryGroup.BY_DEFAULT
        val category = if (defaultGroupMode) categoryId?.let { categoryRepository.get(it) } else null
        val plan = CategorySortModePolicy.plan(
            groupLibraryMode = groupLibraryMode,
            category = category,
            categorizedDisplaySettings = defaultGroupMode && preferences.categorizedDisplaySettings().get(),
            type = type,
            direction = direction,
        )

        if (plan.refreshRandomSortSeed) {
            preferences.randomSortSeed().set(Random.nextInt())
        }
        plan.globalSort?.let { preferences.sortingMode().set(it) }
        plan.categoryUpdate?.let { categoryRepository.updatePartial(it) }
        plan.allCategoryFlags?.let { categoryRepository.updateAllFlags(it) }
    }

    suspend fun await(
        category: Category?,
        type: LibrarySort.Type,
        direction: LibrarySort.Direction,
    ) {
        await(category?.id, type, direction)
    }
}
