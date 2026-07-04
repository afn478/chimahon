package tachiyomi.domain.manga.service

import eu.kanade.tachiyomi.source.model.SManga
import tachiyomi.domain.manga.model.MangaUpdate

object MangaSourceUpdatePolicy {
    fun sourceMetadataUpdate(
        mangaId: Long,
        remoteManga: SManga,
        remoteTitle: String,
        localFavorite: Boolean,
        updateTitle: Boolean,
        coverLastModified: Long?,
    ): MangaUpdate {
        val title = sourceTitleUpdate(
            remoteTitle = remoteTitle,
            localFavorite = localFavorite,
            updateTitle = updateTitle,
        )
        val thumbnailUrl = remoteManga.thumbnail_url?.takeIf { it.isNotEmpty() }

        return MangaUpdate(
            id = mangaId,
            title = title,
            coverLastModified = coverLastModified,
            author = remoteManga.author,
            artist = remoteManga.artist,
            description = remoteManga.description,
            genre = remoteManga.getGenres(),
            thumbnailUrl = thumbnailUrl,
            status = remoteManga.status.toLong(),
            updateStrategy = remoteManga.update_strategy,
            initialized = true,
        )
    }

    private fun sourceTitleUpdate(
        remoteTitle: String,
        localFavorite: Boolean,
        updateTitle: Boolean,
    ): String? {
        return remoteTitle
            .takeIf { it.isNotEmpty() && (!localFavorite || updateTitle) }
    }
}
