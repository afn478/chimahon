package tachiyomi.domain.library.service

import tachiyomi.domain.library.model.NovelBookMetadata

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

    fun mergeMetadata(
        sourceMetadata: NovelBookMetadata?,
        targetMetadata: NovelBookMetadata?,
        targetFolderName: String,
        targetHasImportedContent: Boolean,
        uncategorizedCategoryId: String,
        identityKey: (NovelBookMetadata) -> String,
    ): NovelBookMetadata? {
        if (sourceMetadata == null && targetMetadata == null) return null

        val base = targetMetadata ?: sourceMetadata ?: return null
        val incoming = sourceMetadata

        return base.copy(
            id = targetFolderName,
            folder = targetFolderName,
            hash = identityKey(base),
            author = base.author ?: incoming?.author,
            cover = base.cover ?: incoming?.cover,
            lang = base.lang ?: incoming?.lang,
            isGhost = base.isGhost && !targetHasImportedContent,
            categoryIds = mergeCategoryIds(
                currentCategoryIds = base.categoryIds,
                incomingCategoryIds = incoming?.categoryIds.orEmpty(),
                uncategorizedCategoryId = uncategorizedCategoryId,
            ),
        )
    }
}
