package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import exh.source.BlacklistedSources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.service.SourceCatalogPolicy
import tachiyomi.source.local.isLocal

class GetEnabledSources(
    private val repository: SourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<List<Source>> {
        return combine(
            preferences.pinnedSources().changes(),
            combine(
                preferences.enabledLanguages().changes(),
                preferences.disabledSources().changes(),
                preferences.lastUsedSource().changes(),
            ) { a, b, c -> Triple(a, b, c) },
            // SY -->
            combine(
                preferences.dataSaverExcludedSources().changes(),
                preferences.sourcesTabSourcesInCategories().changes(),
                preferences.sourcesTabCategoriesFilter().changes(),
            ) { a, b, c -> Triple(a, b, c) },
            // SY <--
            repository.getSources(),
        ) {
                pinnedSourceIds,
                (enabledLanguages, disabledSources, lastUsedSource),
                (excludedFromDataSaver, sourcesInCategories, sourceCategoriesFilter),
                sources,
            ->

            SourceCatalogPolicy.selectEnabledSources(
                sources = sources,
                enabledLanguages = enabledLanguages,
                disabledSourceIds = disabledSources,
                hiddenSourceIds = BlacklistedSources.HIDDEN_SOURCES,
                pinnedSourceIds = pinnedSourceIds,
                lastUsedSourceId = lastUsedSource,
                dataSaverExcludedSourceIds = excludedFromDataSaver,
                sourceCategoryPreferences = sourcesInCategories,
                sourceCategoriesFilterEnabled = sourceCategoriesFilter,
                isLocalSource = { it.isLocal() },
            )
        }
            .distinctUntilChanged()
    }
}
