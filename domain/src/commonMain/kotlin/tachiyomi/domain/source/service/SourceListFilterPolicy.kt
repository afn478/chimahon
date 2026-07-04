package tachiyomi.domain.source.service

import tachiyomi.domain.source.model.Source

data class SourceListFilterMetadata(
    val extensionName: String? = null,
    val extensionIsNsfw: Boolean? = null,
)

object SourceListFilterPolicy {
    fun filterSources(
        sources: List<Source>,
        searchQuery: String?,
        nsfwOnly: Boolean,
        metadata: (Source) -> SourceListFilterMetadata = { SourceListFilterMetadata() },
    ): List<Source> {
        return filterItems(
            items = sources,
            searchQuery = searchQuery,
            nsfwOnly = nsfwOnly,
            source = { it },
            metadata = metadata,
        )
    }

    fun <T> filterItems(
        items: List<T>,
        searchQuery: String?,
        source: (T) -> Source,
        nsfwOnly: Boolean = false,
        metadata: (T) -> SourceListFilterMetadata = { SourceListFilterMetadata() },
    ): List<T> {
        val subqueries = SourceSearchPolicy.parseSearchQuery(searchQuery)
        val needsMetadata = nsfwOnly || subqueries.isNotEmpty()
        if (!needsMetadata) {
            return items
        }

        return items.filter { item ->
            val itemMetadata = metadata(item)
            val matchesSearch = subqueries.isEmpty() || SourceSearchPolicy.matchesSource(
                source = source(item),
                extensionName = itemMetadata.extensionName,
                subqueries = subqueries,
            )

            matchesNsfwFilter(
                nsfwOnly = nsfwOnly,
                extensionIsNsfw = itemMetadata.extensionIsNsfw,
            ) && matchesSearch
        }
    }

    fun matchesNsfwFilter(
        nsfwOnly: Boolean,
        extensionIsNsfw: Boolean?,
    ): Boolean {
        return !nsfwOnly || extensionIsNsfw != false
    }
}
