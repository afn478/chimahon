package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourceCategoryPolicy

class SetSourceCategories(
    private val preferences: SourcePreferences,
) {

    fun await(source: Source, sourceCategories: List<String>) {
        preferences.sourcesTabSourcesInCategories().getAndSet { sourcesInCategories ->
            SourceCategoryPolicy.setCategoriesForSource(
                sourceCategoryPreferences = sourcesInCategories,
                sourceId = source.id,
                categories = sourceCategories,
            )
        }
    }
}
