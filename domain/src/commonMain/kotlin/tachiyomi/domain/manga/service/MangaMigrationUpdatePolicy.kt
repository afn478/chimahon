package tachiyomi.domain.manga.service

import tachiyomi.domain.manga.model.MangaUpdate

object MangaMigrationUpdatePolicy {
    fun migrationUpdates(
        currentMangaId: Long,
        targetMangaId: Long,
        replace: Boolean,
        targetDateAdded: Long,
        includeExtra: Boolean,
        includeNotes: Boolean,
        currentChapterFlags: Long,
        currentViewerFlags: Long,
        currentNotes: String,
    ): List<MangaUpdate> {
        val currentMangaUpdate = MangaUserUpdatePolicy.favoriteUpdate(
            mangaId = currentMangaId,
            favorite = false,
            addedAtMillis = 0L,
        )
            .takeIf { replace }
        val targetMangaUpdate = MangaUpdate(
            id = targetMangaId,
            favorite = true,
            chapterFlags = currentChapterFlags.takeIf { includeExtra },
            viewerFlags = currentViewerFlags.takeIf { includeExtra },
            dateAdded = targetDateAdded,
            notes = currentNotes.takeIf { includeNotes },
        )

        return listOfNotNull(currentMangaUpdate, targetMangaUpdate)
    }
}
