package tachiyomi.domain.track.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import tachiyomi.domain.track.model.Track
import tachiyomi.domain.track.repository.TrackRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackInteractorTest {
    @Test
    fun getTracksReturnsRepositoryResultsAndFallbacks() = runTest {
        val repository = FakeTrackRepository(
            track(id = 1, mangaId = 10, trackerId = 60),
            track(id = 2, mangaId = 10, trackerId = 1),
            track(id = 3, mangaId = 20, trackerId = 1),
        )
        val getTracks = GetTracks(repository)

        assertEquals(repository.getTrackById(1), getTracks.awaitOne(1))
        assertEquals(listOf(1L, 2L, 3L), getTracks.await().map(Track::id))
        assertEquals(listOf(1L, 2L), getTracks.await(10).map(Track::id))
        assertEquals(
            mapOf(10L to listOf(1L, 2L), 20L to listOf(3L)),
            getTracks.await(listOf(10L, 20L)).idsByManga(),
        )

        val failingRepository = FakeTrackRepository(failReads = true)
        assertNull(GetTracks(failingRepository).awaitOne(1))
        assertEquals(emptyList(), GetTracks(failingRepository).await())
        assertEquals(emptyList(), GetTracks(failingRepository).await(10))
        assertEquals(emptyMap(), GetTracks(failingRepository).await(listOf(10L, 20L)))
    }

    @Test
    fun insertAndDeleteTracksForwardRepositoryOperations() = runTest {
        val repository = FakeTrackRepository(track(id = 1, mangaId = 10, trackerId = 60))

        InsertTrack(repository).await(track(id = 2, mangaId = 20, trackerId = 1))
        InsertTrack(repository).awaitAll(listOf(track(id = 3, mangaId = 20, trackerId = 2)))
        DeleteTrack(repository).await(mangaId = 10, trackerId = 60)

        assertEquals(listOf(2L, 3L), repository.insertedTracks.map(Track::id))
        assertEquals(listOf(10L to 60L), repository.deletedTracks)
        assertEquals(listOf(2L, 3L), repository.getTracks().map(Track::id))
    }

    @Test
    fun getTracksPerMangaFiltersUnfollowedMdListTracks() = runTest {
        val repository = FakeTrackRepository(
            track(id = 1, mangaId = 10, trackerId = 60, status = 0),
            track(id = 2, mangaId = 10, trackerId = 60, status = 1),
            track(id = 3, mangaId = 20, trackerId = 1, status = 0),
        )

        val result = GetTracksPerManga(repository, IsTrackUnfollowed()).subscribe().first()

        assertEquals(mapOf(10L to listOf(2L), 20L to listOf(3L)), result.idsByManga())
    }

    @Test
    fun isTrackUnfollowedOnlyMatchesMdListUnfollowedTracks() {
        val isTrackUnfollowed = IsTrackUnfollowed()

        assertTrue(isTrackUnfollowed.await(track(id = 1, mangaId = 10, trackerId = 60, status = 0)))
        assertFalse(isTrackUnfollowed.await(track(id = 1, mangaId = 10, trackerId = 60, status = 1)))
        assertFalse(isTrackUnfollowed.await(track(id = 1, mangaId = 10, trackerId = 1, status = 0)))
    }
}

private fun Map<Long, List<Track>>.idsByManga(): Map<Long, List<Long>> {
    return mapValues { (_, tracks) -> tracks.map(Track::id) }
}

private fun track(
    id: Long,
    mangaId: Long,
    trackerId: Long,
    status: Long = 1,
): Track {
    return Track(
        id = id,
        mangaId = mangaId,
        trackerId = trackerId,
        remoteId = id,
        libraryId = null,
        title = "Track $id",
        lastChapterRead = 0.0,
        totalChapters = 0,
        status = status,
        score = 0.0,
        remoteUrl = "",
        startDate = 0,
        finishDate = 0,
        private = false,
    )
}

private class FakeTrackRepository(
    vararg tracks: Track,
    private val failReads: Boolean = false,
) : TrackRepository {
    private val tracks = MutableStateFlow(tracks.associateBy(Track::id))
    val insertedTracks = mutableListOf<Track>()
    val deletedTracks = mutableListOf<Pair<Long, Long>>()

    override suspend fun getTrackById(id: Long): Track? {
        failReadIfNeeded()
        return tracks.value[id]
    }

    override suspend fun getTracks(): List<Track> {
        failReadIfNeeded()
        return tracks.value.values.sortedBy(Track::id)
    }

    override suspend fun getTracksByMangaIds(mangaIds: List<Long>): List<Track> {
        failReadIfNeeded()
        return tracks.value.values
            .filter { it.mangaId in mangaIds }
            .sortedBy(Track::id)
    }

    override suspend fun getTracksByMangaId(mangaId: Long): List<Track> {
        failReadIfNeeded()
        return tracks.value.values
            .filter { it.mangaId == mangaId }
            .sortedBy(Track::id)
    }

    override fun getTracksAsFlow(): Flow<List<Track>> {
        return tracks.map { entries -> entries.values.sortedBy(Track::id) }
    }

    override fun getTracksByMangaIdAsFlow(mangaId: Long): Flow<List<Track>> {
        return tracks.map { entries ->
            entries.values
                .filter { it.mangaId == mangaId }
                .sortedBy(Track::id)
        }
    }

    override suspend fun delete(mangaId: Long, trackerId: Long) {
        deletedTracks += mangaId to trackerId
        tracks.value = tracks.value.filterValues { it.mangaId != mangaId || it.trackerId != trackerId }
    }

    override suspend fun insert(track: Track) {
        insertedTracks += track
        tracks.value = tracks.value + (track.id to track)
    }

    override suspend fun insertAll(tracks: List<Track>) {
        insertedTracks += tracks
        this.tracks.value = this.tracks.value + tracks.associateBy(Track::id)
    }

    private fun failReadIfNeeded() {
        if (failReads) {
            throw IllegalStateException("Read failed")
        }
    }
}
