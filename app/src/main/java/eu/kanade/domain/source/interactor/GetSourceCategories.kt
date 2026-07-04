package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.source.service.SourceCategoryPolicy

class GetSourceCategories(
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<List<String>> {
        return preferences.sourcesTabCategories().changes().map(SourceCategoryPolicy::sortCategories)
    }
}
