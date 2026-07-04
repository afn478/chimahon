package tachiyomi.domain.extension.service

data class ExtensionSearchSource(
    val name: String,
    val baseUrl: String?,
    val id: Long,
)

object ExtensionSearchPolicy {
    fun parseSearchQuery(query: String): List<String> {
        return query
            .split(",")
            .map(String::trim)
            .filter(String::isNotBlank)
    }

    fun matchesExtension(
        extensionName: String,
        sources: List<ExtensionSearchSource>,
        subqueries: List<String>,
    ): Boolean {
        if (subqueries.isEmpty()) {
            return true
        }

        return subqueries.any { subquery ->
            extensionName.contains(subquery, ignoreCase = true) ||
                sources.any { source -> source.matches(subquery) }
        }
    }

    private fun ExtensionSearchSource.matches(subquery: String): Boolean {
        return name.contains(subquery, ignoreCase = true) ||
            baseUrl?.contains(subquery, ignoreCase = true) == true ||
            id == subquery.toLongOrNull()
    }
}
