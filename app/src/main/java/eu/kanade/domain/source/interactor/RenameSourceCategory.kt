package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.source.service.SourceCategoryPolicy

class RenameSourceCategory(
    private val preferences: SourcePreferences,
    private val createSourceCategory: CreateSourceCategory,
) {

    fun await(categoryOld: String, categoryNew: String): CreateSourceCategory.Result {
        when (val result = createSourceCategory.await(categoryNew)) {
            CreateSourceCategory.Result.InvalidName -> return result
            CreateSourceCategory.Result.Success -> {}
        }

        preferences.sourcesTabSourcesInCategories().getAndSet { sourcesInCategories ->
            SourceCategoryPolicy.renameSourceCategoryPreferences(
                sourceCategoryPreferences = sourcesInCategories,
                oldCategory = categoryOld,
                newCategory = categoryNew,
            )
        }
        preferences.sourcesTabCategories().getAndSet {
            SourceCategoryPolicy.renameCategory(
                categories = it,
                oldCategory = categoryOld,
                newCategory = categoryNew,
            )
        }

        return CreateSourceCategory.Result.Success
    }
}
