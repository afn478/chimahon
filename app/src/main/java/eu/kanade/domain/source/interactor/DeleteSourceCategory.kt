package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.source.service.SourceCategoryPolicy

class DeleteSourceCategory(private val preferences: SourcePreferences) {

    fun await(category: String) {
        preferences.sourcesTabSourcesInCategories().getAndSet { sourcesInCategories ->
            SourceCategoryPolicy.deleteSourceCategoryPreferences(sourcesInCategories, category)
        }
        preferences.sourcesTabCategories().getAndSet {
            SourceCategoryPolicy.deleteCategory(it, category)
        }
    }
}
