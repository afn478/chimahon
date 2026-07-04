package tachiyomi.domain.manga.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MangaModelTest {
    @Test
    fun favoriteEntryFormatsGalleryUrl() {
        val entry = FavoriteEntry(title = "Favorite", gid = "12345", token = "abcdef")

        assertEquals("/g/12345/abcdef/?nw=always", entry.getUrl())
    }

    @Test
    fun mergedMangaReferenceExposesSortModeConstants() {
        assertEquals(0, MergedMangaReference.CHAPTER_SORT_NONE)
        assertEquals(1, MergedMangaReference.CHAPTER_SORT_PRIORITY)
        assertEquals(2, MergedMangaReference.CHAPTER_SORT_MOST_CHAPTERS)
        assertEquals(3, MergedMangaReference.CHAPTER_SORT_HIGHEST_CHAPTER_NUMBER)
    }
}
