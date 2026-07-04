package tachiyomi.domain.source.service

import tachiyomi.domain.source.model.Source

object SourceSearchPolicy {
    fun parseSearchQuery(query: String?): List<String> {
        return query.orEmpty()
            .split(",")
            .map(String::trim)
            .filter(String::isNotBlank)
    }

    fun matchesSource(
        source: Source,
        extensionName: String?,
        subqueries: List<String>,
    ): Boolean {
        return matchesSource(
            sourceName = source.name,
            sourceId = source.id,
            extensionName = extensionName,
            subqueries = subqueries,
        )
    }

    fun matchesSource(
        sourceName: String,
        sourceId: Long,
        extensionName: String?,
        subqueries: List<String>,
    ): Boolean {
        if (subqueries.isEmpty()) {
            return true
        }

        return subqueries.any { subquery ->
            extensionName?.contains(subquery, ignoreCase = true) == true ||
                sourceName.contains(subquery, ignoreCase = true) ||
                sourceId == subquery.toLongOrNull()
        }
    }
}
