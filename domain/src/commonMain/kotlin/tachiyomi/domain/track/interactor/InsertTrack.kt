package tachiyomi.domain.track.interactor

import tachiyomi.domain.track.model.Track
import tachiyomi.domain.track.repository.TrackRepository

class InsertTrack(
    private val trackRepository: TrackRepository,
) {

    suspend fun await(track: Track) {
        try {
            trackRepository.insert(track)
        } catch (_: Exception) {
            return
        }
    }

    suspend fun awaitAll(tracks: List<Track>) {
        try {
            trackRepository.insertAll(tracks)
        } catch (_: Exception) {
            return
        }
    }
}
