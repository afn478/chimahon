package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.source.service.SourceCategoryPolicy

class CreateSourceCategory(private val preferences: SourcePreferences) {

    fun await(category: String): Result {
        if (!SourceCategoryPolicy.canCreateCategory(category)) {
            return Result.InvalidName
        }

        preferences.sourcesTabCategories().getAndSet {
            SourceCategoryPolicy.addCategory(it, category)
        }

        return Result.Success
    }

    sealed class Result {
        data object InvalidName : Result()
        data object Success : Result()
    }
}
