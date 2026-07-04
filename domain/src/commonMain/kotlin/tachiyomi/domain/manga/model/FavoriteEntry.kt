package tachiyomi.domain.manga.model

data class FavoriteEntry(
    val title: String,
    val gid: String,
    val token: String,
    val otherGid: String? = null,
    val otherToken: String? = null,
    val category: Int = -1,
) {
    fun getUrl() = "/g/$gid/$token/?nw=always"
}
