package tachiyomi.domain.category.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.category.service.CategoryCreationPolicy
import tachiyomi.domain.category.service.CategorySortModePolicy
import tachiyomi.domain.library.service.LibraryPreferences

class CreateCategoryWithName(
    private val categoryRepository: CategoryRepository,
    private val preferences: LibraryPreferences,
) {

    private val initialFlags: Long
        get() {
            return CategorySortModePolicy.flagsFor(preferences.sortingMode().get())
        }

    suspend fun await(name: String): Result = withNonCancellableContext {
        val categories = categoryRepository.getAll()
        val newCategory = CategoryCreationPolicy.create(
            name = name,
            existingCategories = categories,
            flags = initialFlags,
        )

        try {
            categoryRepository.insert(newCategory)
            Result.Success(/* SY --> */newCategory/* SY <-- */)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            Result.InternalError(e)
        }
    }

    sealed interface Result {
        // SY -->
        data class Success(val category: Category) : Result

        // SY <--
        data class InternalError(val error: Throwable) : Result
    }
}
