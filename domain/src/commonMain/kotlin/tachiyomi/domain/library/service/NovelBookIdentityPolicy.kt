package tachiyomi.domain.library.service

object NovelBookIdentityPolicy {
    fun titleAuthorIdentityInput(
        title: String?,
        author: String?,
    ): String? {
        val titleKey = title?.trim()?.lowercase().orEmpty()
        val authorKey = author?.trim()?.lowercase().orEmpty()

        return if (titleKey.isNotEmpty() || authorKey.isNotEmpty()) {
            "$titleKey|$authorKey"
        } else {
            null
        }
    }

    fun identityKey(
        title: String?,
        author: String?,
        storedHash: String?,
        fallbackId: String,
        hashIdentity: (String) -> String,
    ): String {
        return titleAuthorIdentityInput(
            title = title,
            author = author,
        )?.let(hashIdentity)
            ?: storedHash?.takeIf { it.isNotBlank() }
            ?: fallbackId
    }

    fun <T> deduplicateByIdentity(
        books: List<T>,
        identityKey: (T) -> String,
        isGhost: (T) -> Boolean,
        hasImportedContent: (T) -> Boolean,
        folderName: (T) -> String,
        lastAccess: (T) -> Long,
    ): List<T> {
        return books
            .groupBy(identityKey)
            .mapNotNull { (identity, duplicates) ->
                selectPreferredDuplicate(
                    books = duplicates,
                    identityKey = identity,
                    isGhost = isGhost,
                    hasImportedContent = hasImportedContent,
                    folderName = folderName,
                    lastAccess = lastAccess,
                )
            }
    }

    fun <T> selectPreferredDuplicate(
        books: List<T>,
        identityKey: String,
        isGhost: (T) -> Boolean,
        hasImportedContent: (T) -> Boolean,
        folderName: (T) -> String,
        lastAccess: (T) -> Long,
    ): T? {
        return books.minWithOrNull(
            compareBy<T> { isGhost(it) }
                .thenBy { !hasImportedContent(it) }
                .thenBy { folderName(it) != identityKey }
                .thenByDescending(lastAccess),
        )
    }
}
