package tachiyomi.domain.manga.service

import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.model.MangaViewerFlags

object MangaViewerFlagUpdatePolicy {
    fun readingModeUpdate(
        mangaId: Long,
        currentFlags: Long,
        flag: Long,
    ): MangaUpdate {
        return viewerFlagsUpdate(
            mangaId = mangaId,
            viewerFlags = MangaViewerFlags.withReadingMode(currentFlags, flag),
        )
    }

    fun orientationUpdate(
        mangaId: Long,
        currentFlags: Long,
        flag: Long,
    ): MangaUpdate {
        return viewerFlagsUpdate(
            mangaId = mangaId,
            viewerFlags = MangaViewerFlags.withOrientation(currentFlags, flag),
        )
    }

    private fun viewerFlagsUpdate(
        mangaId: Long,
        viewerFlags: Long,
    ): MangaUpdate {
        return MangaUpdate(
            id = mangaId,
            viewerFlags = viewerFlags,
        )
    }
}
