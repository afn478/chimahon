package tachiyomi.domain.storage.service

object NovelBookStoragePolicy {
    const val BOOKS_DIRECTORY = "novels"
    const val OBSOLETE_SPINE_CACHE_FILE = "spine_cache.json"

    val importedContentExtensions = setOf(
        "opf",
        "xhtml",
        "html",
        "htm",
        "ncx",
    )

    fun isImportedContentFile(
        isFile: Boolean,
        extension: String,
    ): Boolean {
        return isFile && extension.lowercase() in importedContentExtensions
    }

    fun containsImportedContent(entries: Iterable<NovelBookStorageEntry>): Boolean {
        return entries.any { entry ->
            isImportedContentFile(
                isFile = entry.isFile,
                extension = entry.extension,
            )
        }
    }
}

data class NovelBookStorageEntry(
    val isFile: Boolean,
    val extension: String,
)
