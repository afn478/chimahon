package tachiyomi.domain.source.service

import tachiyomi.domain.source.model.EXHSavedSearch

object SourceSavedSearchPolicy {
    fun sortSavedSearches(savedSearches: List<EXHSavedSearch>): List<EXHSavedSearch> {
        return savedSearches.sortedWith { left, right ->
            left.name.compareTo(right.name, ignoreCase = true)
        }
    }
}
