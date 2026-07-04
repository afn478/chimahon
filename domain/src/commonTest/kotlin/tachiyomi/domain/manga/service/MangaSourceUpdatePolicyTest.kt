package tachiyomi.domain.manga.service

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import tachiyomi.domain.manga.model.MangaUpdate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MangaSourceUpdatePolicyTest {
    @Test
    fun sourceMetadataUpdateCopiesSourceFields() {
        val remoteManga = SManga(
            url = "/manga",
            title = "Remote title",
            artist = "Artist",
            author = "Author",
            description = "Description",
            genre = "Action, Drama, Action",
            status = SManga.ONGOING,
            thumbnail_url = "https://example.test/cover.jpg",
            initialized = false,
        ).also {
            it.update_strategy = UpdateStrategy.ONLY_FETCH_ONCE
        }

        val update = MangaSourceUpdatePolicy.sourceMetadataUpdate(
            mangaId = MANGA_ID,
            remoteManga = remoteManga,
            remoteTitle = remoteManga.title,
            localFavorite = false,
            updateTitle = false,
            coverLastModified = COVER_LAST_MODIFIED,
        )

        assertEquals(
            MangaUpdate(
                id = MANGA_ID,
                title = "Remote title",
                coverLastModified = COVER_LAST_MODIFIED,
                author = "Author",
                artist = "Artist",
                description = "Description",
                genre = listOf("Action", "Drama"),
                thumbnailUrl = "https://example.test/cover.jpg",
                status = SManga.ONGOING.toLong(),
                updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE,
                initialized = true,
            ),
            update,
        )
    }

    @Test
    fun favoriteMangaKeepsExistingTitleUnlessTitleUpdatesAreEnabled() {
        val remoteManga = SManga(url = "/manga", title = "Remote title")

        val skippedUpdate = MangaSourceUpdatePolicy.sourceMetadataUpdate(
            mangaId = MANGA_ID,
            remoteManga = remoteManga,
            remoteTitle = remoteManga.title,
            localFavorite = true,
            updateTitle = false,
            coverLastModified = null,
        )
        val titleUpdate = MangaSourceUpdatePolicy.sourceMetadataUpdate(
            mangaId = MANGA_ID,
            remoteManga = remoteManga,
            remoteTitle = remoteManga.title,
            localFavorite = true,
            updateTitle = true,
            coverLastModified = null,
        )

        assertNull(skippedUpdate.title)
        assertEquals("Remote title", titleUpdate.title)
    }

    @Test
    fun blankRemoteMetadataStaysUnsetForPartialUpdate() {
        val remoteManga = SManga(
            url = "/manga",
            title = "",
            genre = "   ",
            thumbnail_url = "",
        )

        val update = MangaSourceUpdatePolicy.sourceMetadataUpdate(
            mangaId = MANGA_ID,
            remoteManga = remoteManga,
            remoteTitle = remoteManga.title,
            localFavorite = false,
            updateTitle = true,
            coverLastModified = null,
        )

        assertNull(update.title)
        assertNull(update.genre)
        assertNull(update.thumbnailUrl)
    }

    @Test
    fun missingRemoteTitleIsIgnored() {
        val update = MangaSourceUpdatePolicy.sourceMetadataUpdate(
            mangaId = MANGA_ID,
            remoteManga = SManga(url = "/manga", title = "Ignored delegate title"),
            remoteTitle = "",
            localFavorite = false,
            updateTitle = true,
            coverLastModified = null,
        )

        assertNull(update.title)
    }

    private companion object {
        const val MANGA_ID = 42L
        const val COVER_LAST_MODIFIED = 1234L
    }
}
