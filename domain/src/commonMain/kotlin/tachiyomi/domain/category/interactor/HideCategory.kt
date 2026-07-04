package tachiyomi.domain.category.interactor

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.repository.CategoryRepository

class HideCategory(
    private val categoryRepository: CategoryRepository,
) {

    suspend fun await(category: Category) = withContext(NonCancellable) {
        val update = CategoryUpdate(
            id = category.id,
            hidden = !category.hidden,
        )

        try {
            categoryRepository.updatePartial(update)
            Result.Success
        } catch (e: Exception) {
            Result.InternalError(e)
        }
    }

    sealed class Result {
        data object Success : Result()
        data class InternalError(val error: Throwable) : Result()
    }
}
