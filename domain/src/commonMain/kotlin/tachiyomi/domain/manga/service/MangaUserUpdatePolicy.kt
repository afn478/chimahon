package tachiyomi.domain.manga.service

import tachiyomi.domain.manga.model.MangaUpdate

object MangaUserUpdatePolicy {
    fun lastUpdateUpdate(
        mangaId: Long,
        updatedAtMillis: Long,
    ): MangaUpdate {
        return MangaUpdate(
            id = mangaId,
            lastUpdate = updatedAtMillis,
        )
    }

    fun coverLastModifiedUpdate(
        mangaId: Long,
        modifiedAtMillis: Long,
    ): MangaUpdate {
        return MangaUpdate(
            id = mangaId,
            coverLastModified = modifiedAtMillis,
        )
    }

    fun favoriteUpdate(
        mangaId: Long,
        favorite: Boolean,
        addedAtMillis: Long,
    ): MangaUpdate {
        return MangaUpdate(
            id = mangaId,
            favorite = favorite,
            dateAdded = if (favorite) addedAtMillis else 0L,
        )
    }

    fun notesUpdate(
        mangaId: Long,
        notes: String,
    ): MangaUpdate {
        return MangaUpdate(
            id = mangaId,
            notes = notes,
        )
    }
}
