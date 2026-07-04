package tachiyomi.domain.library.service

import tachiyomi.domain.library.model.NovelBookMetadata

object NovelBookMigrationPolicy {
    data class MetadataMigration(
        val stableId: String,
        val title: String,
        val author: String,
        val metadata: NovelBookMetadata,
    )

    fun migrateExistingMetadataWithoutHash(
        existingMetadata: NovelBookMetadata,
        parsedTitle: String?,
        parsedAuthor: String?,
        currentFolderName: String,
    ): MetadataMigration {
        val title = parsedTitle ?: existingMetadata.title ?: NovelBookImportPolicy.UNKNOWN_TITLE
        val author = NovelBookImportPolicy.importedAuthor(parsedAuthor)
        val stableId = NovelBookIdentityPolicy.stableTitleAuthorId(
            title = title,
            author = author,
        )

        return MetadataMigration(
            stableId = stableId,
            title = title,
            author = author,
            metadata = existingMetadata.copy(
                id = stableId,
                title = existingMetadata.title ?: title,
                author = existingMetadata.author ?: author.takeIf { it.isNotEmpty() },
                cover = rewriteFolderInPath(
                    path = existingMetadata.cover,
                    oldFolderName = currentFolderName,
                    newFolderName = stableId,
                ),
                folder = stableId,
                hash = stableId,
            ),
        )
    }

    fun migrateParsedBookWithoutExistingMetadata(
        parsedTitle: String?,
        parsedAuthor: String?,
        coverPath: String?,
        migrationTimeMillis: Long,
    ): MetadataMigration {
        val title = NovelBookImportPolicy.importedTitle(parsedTitle)
        val author = NovelBookImportPolicy.importedAuthor(parsedAuthor)
        val stableId = NovelBookIdentityPolicy.stableTitleAuthorId(
            title = title,
            author = author,
        )

        return MetadataMigration(
            stableId = stableId,
            title = title,
            author = author,
            metadata = NovelBookMetadata(
                id = stableId,
                title = title,
                author = author,
                cover = coverPath,
                folder = stableId,
                lastAccess = migrationTimeMillis,
                dateAdded = migrationTimeMillis,
                hash = stableId,
                isGhost = false,
            ),
        )
    }

    fun correctStableDirectoryMetadata(
        metadata: NovelBookMetadata,
        currentFolderName: String,
    ): MetadataMigration? {
        val title = metadata.title ?: return null
        val author = NovelBookImportPolicy.importedAuthor(metadata.author)
        val stableId = NovelBookIdentityPolicy.stableTitleAuthorId(
            title = title,
            author = author,
        )

        return MetadataMigration(
            stableId = stableId,
            title = title,
            author = author,
            metadata = metadata.copy(
                id = stableId,
                folder = stableId,
                hash = stableId,
                cover = rewriteFolderInPath(
                    path = metadata.cover,
                    oldFolderName = currentFolderName,
                    newFolderName = stableId,
                ),
            ),
        )
    }

    private fun rewriteFolderInPath(
        path: String?,
        oldFolderName: String,
        newFolderName: String,
    ): String? {
        return path?.replace(oldFolderName, newFolderName)
    }
}
