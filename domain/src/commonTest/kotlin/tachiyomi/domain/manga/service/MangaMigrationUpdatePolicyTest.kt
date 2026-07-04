package tachiyomi.domain.manga.service

import tachiyomi.domain.manga.model.MangaUpdate
import kotlin.test.Test
import kotlin.test.assertEquals

class MangaMigrationUpdatePolicyTest {
    @Test
    fun replaceMigrationUnfavoritesCurrentMangaAndCopiesSelectedTargetFields() {
        val updates = MangaMigrationUpdatePolicy.migrationUpdates(
            currentMangaId = CURRENT_MANGA_ID,
            targetMangaId = TARGET_MANGA_ID,
            replace = true,
            targetDateAdded = CURRENT_DATE_ADDED,
            includeExtra = true,
            includeNotes = true,
            currentChapterFlags = CHAPTER_FLAGS,
            currentViewerFlags = VIEWER_FLAGS,
            currentNotes = NOTES,
        )

        assertEquals(
            listOf(
                MangaUpdate(id = CURRENT_MANGA_ID, favorite = false, dateAdded = 0L),
                MangaUpdate(
                    id = TARGET_MANGA_ID,
                    favorite = true,
                    chapterFlags = CHAPTER_FLAGS,
                    viewerFlags = VIEWER_FLAGS,
                    dateAdded = CURRENT_DATE_ADDED,
                    notes = NOTES,
                ),
            ),
            updates,
        )
    }

    @Test
    fun nonReplaceMigrationOnlyFavoritesTargetManga() {
        val updates = MangaMigrationUpdatePolicy.migrationUpdates(
            currentMangaId = CURRENT_MANGA_ID,
            targetMangaId = TARGET_MANGA_ID,
            replace = false,
            targetDateAdded = NEW_DATE_ADDED,
            includeExtra = false,
            includeNotes = false,
            currentChapterFlags = CHAPTER_FLAGS,
            currentViewerFlags = VIEWER_FLAGS,
            currentNotes = NOTES,
        )

        assertEquals(
            listOf(
                MangaUpdate(
                    id = TARGET_MANGA_ID,
                    favorite = true,
                    dateAdded = NEW_DATE_ADDED,
                ),
            ),
            updates,
        )
    }

    @Test
    fun notesCanBeCopiedWithoutExtraFlags() {
        val updates = MangaMigrationUpdatePolicy.migrationUpdates(
            currentMangaId = CURRENT_MANGA_ID,
            targetMangaId = TARGET_MANGA_ID,
            replace = false,
            targetDateAdded = NEW_DATE_ADDED,
            includeExtra = false,
            includeNotes = true,
            currentChapterFlags = CHAPTER_FLAGS,
            currentViewerFlags = VIEWER_FLAGS,
            currentNotes = NOTES,
        )

        assertEquals(
            listOf(
                MangaUpdate(
                    id = TARGET_MANGA_ID,
                    favorite = true,
                    dateAdded = NEW_DATE_ADDED,
                    notes = NOTES,
                ),
            ),
            updates,
        )
    }

    private companion object {
        const val CURRENT_MANGA_ID = 42L
        const val TARGET_MANGA_ID = 43L
        const val CURRENT_DATE_ADDED = 1000L
        const val NEW_DATE_ADDED = 2000L
        const val CHAPTER_FLAGS = 123L
        const val VIEWER_FLAGS = 456L
        const val NOTES = "Keep these notes"
    }
}
