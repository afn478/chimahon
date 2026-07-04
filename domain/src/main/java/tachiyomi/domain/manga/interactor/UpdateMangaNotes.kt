package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.manga.service.MangaUserUpdatePolicy

class UpdateMangaNotes(
    private val mangaRepository: MangaRepository,
) {

    suspend operator fun invoke(mangaId: Long, notes: String): Boolean {
        return mangaRepository.update(
            MangaUserUpdatePolicy.notesUpdate(
                mangaId = mangaId,
                notes = notes,
            ),
        )
    }
}
