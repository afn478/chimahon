package eu.kanade.domain.manga.interactor

import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.manga.service.MangaViewerFlagUpdatePolicy

class SetMangaViewerFlags(
    private val mangaRepository: MangaRepository,
) {

    suspend fun awaitSetReadingMode(id: Long, flag: Long) {
        val manga = mangaRepository.getMangaById(id)
        mangaRepository.update(
            MangaViewerFlagUpdatePolicy.readingModeUpdate(
                mangaId = id,
                currentFlags = manga.viewerFlags,
                flag = flag,
            ),
        )
    }

    suspend fun awaitSetOrientation(id: Long, flag: Long) {
        val manga = mangaRepository.getMangaById(id)
        mangaRepository.update(
            MangaViewerFlagUpdatePolicy.orientationUpdate(
                mangaId = id,
                currentFlags = manga.viewerFlags,
                flag = flag,
            ),
        )
    }
}
