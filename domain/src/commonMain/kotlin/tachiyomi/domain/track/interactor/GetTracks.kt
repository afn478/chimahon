package tachiyomi.domain.track.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.track.model.Track
import tachiyomi.domain.track.repository.TrackRepository

class GetTracks(
    private val trackRepository: TrackRepository,
) {

    suspend fun awaitOne(id: Long): Track? {
        return try {
            trackRepository.getTrackById(id)
        } catch (_: Exception) {
            null
        }
    }

    // SY -->
    suspend fun await(): List<Track> {
        return try {
            trackRepository.getTracks()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun await(mangaIds: List<Long>): Map<Long, List<Track>> {
        return try {
            trackRepository.getTracksByMangaIds(mangaIds)
                .groupBy { it.mangaId }
        } catch (_: Exception) {
            emptyMap()
        }
    }
    // SY <--

    suspend fun await(mangaId: Long): List<Track> {
        return try {
            trackRepository.getTracksByMangaId(mangaId)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun subscribe(mangaId: Long): Flow<List<Track>> {
        return trackRepository.getTracksByMangaIdAsFlow(mangaId)
    }
}
