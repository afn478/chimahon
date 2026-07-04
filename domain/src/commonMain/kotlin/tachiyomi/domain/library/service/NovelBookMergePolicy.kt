package tachiyomi.domain.library.service

object NovelBookMergePolicy {
    fun <T> selectLatest(
        current: T,
        incoming: T,
        lastModified: (T) -> Long,
    ): T {
        return if (lastModified(incoming) > lastModified(current)) {
            incoming
        } else {
            current
        }
    }

    fun <T> selectLatestOrNull(
        current: T?,
        incoming: T?,
        lastModified: (T) -> Long,
    ): T? {
        return when {
            current == null -> incoming
            incoming == null -> current
            else -> selectLatest(
                current = current,
                incoming = incoming,
                lastModified = lastModified,
            )
        }
    }

    fun <T> mergeLatestByKey(
        items: List<T>,
        itemKey: (T) -> String,
        lastModified: (T) -> Long,
    ): List<T> {
        return items
            .groupBy(itemKey)
            .map { (_, entries) ->
                entries.reduce { latest, candidate ->
                    selectLatest(
                        current = latest,
                        incoming = candidate,
                        lastModified = lastModified,
                    )
                }
            }
    }

    fun mergeCategoryIds(
        currentCategoryIds: Collection<String>,
        incomingCategoryIds: Collection<String>,
        uncategorizedCategoryId: String,
    ): List<String> {
        return NovelCategoryPolicy.normalizeCategoryIds(
            categoryIds = currentCategoryIds + incomingCategoryIds,
            uncategorizedCategoryId = uncategorizedCategoryId,
        )
    }
}
