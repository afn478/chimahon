package tachiyomi.domain.manga.service

import tachiyomi.domain.manga.model.MangaUpdate
import kotlin.test.Test
import kotlin.test.assertEquals

class MangaUserUpdatePolicyTest {
    @Test
    fun lastUpdateUpdateOnlySetsLastUpdate() {
        val update = MangaUserUpdatePolicy.lastUpdateUpdate(
            mangaId = MANGA_ID,
            updatedAtMillis = TIMESTAMP,
        )

        assertEquals(MangaUpdate(id = MANGA_ID, lastUpdate = TIMESTAMP), update)
    }

    @Test
    fun coverLastModifiedUpdateOnlySetsCoverLastModified() {
        val update = MangaUserUpdatePolicy.coverLastModifiedUpdate(
            mangaId = MANGA_ID,
            modifiedAtMillis = TIMESTAMP,
        )

        assertEquals(MangaUpdate(id = MANGA_ID, coverLastModified = TIMESTAMP), update)
    }

    @Test
    fun favoriteUpdateSetsDateAddedWhenFavoriting() {
        val update = MangaUserUpdatePolicy.favoriteUpdate(
            mangaId = MANGA_ID,
            favorite = true,
            addedAtMillis = TIMESTAMP,
        )

        assertEquals(MangaUpdate(id = MANGA_ID, favorite = true, dateAdded = TIMESTAMP), update)
    }

    @Test
    fun favoriteUpdateClearsDateAddedWhenUnfavoriting() {
        val update = MangaUserUpdatePolicy.favoriteUpdate(
            mangaId = MANGA_ID,
            favorite = false,
            addedAtMillis = TIMESTAMP,
        )

        assertEquals(MangaUpdate(id = MANGA_ID, favorite = false, dateAdded = 0L), update)
    }

    @Test
    fun notesUpdateOnlySetsNotes() {
        val update = MangaUserUpdatePolicy.notesUpdate(
            mangaId = MANGA_ID,
            notes = "Reader notes",
        )

        assertEquals(MangaUpdate(id = MANGA_ID, notes = "Reader notes"), update)
    }

    private companion object {
        const val MANGA_ID = 42L
        const val TIMESTAMP = 1234L
    }
}
