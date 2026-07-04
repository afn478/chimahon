package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import eu.kanade.tachiyomi.source.CatalogueSource
import tachiyomi.domain.source.service.GlobalSearchSourceSelectionPolicy

class GlobalSearchScreenModel(
    initialQuery: String = "",
    initialExtensionFilter: String? = null,
) : SearchScreenModel(State(searchQuery = initialQuery)) {

    init {
        extensionFilter = initialExtensionFilter
        if (initialQuery.isNotBlank() || !initialExtensionFilter.isNullOrBlank()) {
            if (extensionFilter != null) {
                // we're going to use custom extension filter instead
                setSourceFilter(SourceFilter.All)
            }
            search()
        }

        // KMK -->
        shouldPinnedSourcesHidden()
        // KMK <--
    }

    override fun getEnabledSources(): List<CatalogueSource> {
        val sources = super.getEnabledSources()
        if (state.value.sourceFilter != SourceFilter.PinnedOnly) {
            return sources
        }

        return GlobalSearchSourceSelectionPolicy.selectPinnedSources(
            sources = sources,
            pinnedSourceIds = pinnedSources,
            sourceId = CatalogueSource::id,
        )
    }
}
