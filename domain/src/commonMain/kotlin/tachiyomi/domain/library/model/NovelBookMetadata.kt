package tachiyomi.domain.library.model

import kotlinx.serialization.Serializable

@Serializable
data class NovelBookMetadata(
    val id: String = NovelLibraryDefaults.newId(),
    val title: String? = null,
    val author: String? = null,
    val cover: String? = null,
    val folder: String? = null,
    val lastAccess: Long = NovelLibraryDefaults.nowMillis(),
    val dateAdded: Long = NovelLibraryDefaults.nowMillis(),
    val hash: String? = null,
    val isGhost: Boolean = false,
    val categoryIds: List<String> = emptyList(),
    val lang: String? = null,
)
