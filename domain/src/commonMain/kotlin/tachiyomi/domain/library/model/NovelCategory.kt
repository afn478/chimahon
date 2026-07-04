package tachiyomi.domain.library.model

import kotlinx.serialization.Serializable

@Serializable
data class NovelCategory(
    val id: String = NovelLibraryDefaults.newId(),
    val name: String,
    val order: Int = 0,
    val flags: Long = 0,
) {
    val isSystemCategory: Boolean
        get() = id == UNCATEGORIZED_ID

    companion object {
        const val UNCATEGORIZED_ID = "default"
    }
}
