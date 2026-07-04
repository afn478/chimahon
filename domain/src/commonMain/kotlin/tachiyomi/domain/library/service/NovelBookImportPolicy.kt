package tachiyomi.domain.library.service

import tachiyomi.domain.library.model.NovelBookMetadata

object NovelBookImportPolicy {
    const val UNKNOWN_TITLE = "Unknown"

    fun importedTitle(title: String?): String {
        return title ?: UNKNOWN_TITLE
    }

    fun importedAuthor(author: String?): String {
        return author ?: ""
    }

    fun stableIdForImport(
        title: String?,
        author: String?,
    ): String {
        return NovelBookIdentityPolicy.stableTitleAuthorId(
            title = importedTitle(title),
            author = importedAuthor(author),
        )
    }

    fun metadataForImportedBook(
        title: String?,
        author: String?,
        coverPath: String?,
        language: String?,
        existingMetadata: NovelBookMetadata?,
        requestedCategoryIds: Collection<String>?,
        importTimeMillis: Long,
        uncategorizedCategoryId: String,
    ): NovelBookMetadata {
        val importedTitle = importedTitle(title)
        val importedAuthor = importedAuthor(author)
        val stableId = stableIdForImport(
            title = title,
            author = author,
        )

        return NovelBookMetadata(
            id = stableId,
            title = importedTitle,
            author = importedAuthor,
            cover = coverPath,
            folder = stableId,
            lastAccess = existingMetadata?.lastAccess ?: importTimeMillis,
            dateAdded = existingMetadata?.dateAdded ?: importTimeMillis,
            hash = stableId,
            isGhost = false,
            lang = language,
            categoryIds = NovelCategoryPolicy.resolveImportedCategoryIds(
                existingCategoryIds = existingMetadata?.categoryIds.orEmpty(),
                requestedCategoryIds = requestedCategoryIds,
                uncategorizedCategoryId = uncategorizedCategoryId,
            ),
        )
    }
}
