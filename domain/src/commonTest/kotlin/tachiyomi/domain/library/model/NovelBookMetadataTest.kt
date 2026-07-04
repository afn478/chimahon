package tachiyomi.domain.library.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

class NovelBookMetadataTest {
    @Test
    fun defaultMetadataUsesPortableGeneratedValues() {
        val before = Clock.System.now().toEpochMilliseconds()

        val metadata = NovelBookMetadata()

        val after = Clock.System.now().toEpochMilliseconds()
        assertTrue(metadata.id.matches(UUID_PATTERN))
        assertNull(metadata.title)
        assertNull(metadata.author)
        assertNull(metadata.cover)
        assertNull(metadata.folder)
        assertTrue(metadata.lastAccess in before..after)
        assertTrue(metadata.dateAdded in before..after)
        assertNull(metadata.hash)
        assertFalse(metadata.isGhost)
        assertEquals(emptyList(), metadata.categoryIds)
        assertNull(metadata.lang)
    }

    @Test
    fun explicitMetadataValuesArePreserved() {
        val metadata = NovelBookMetadata(
            id = "book-id",
            title = "Book title",
            author = "Author",
            cover = "cover.jpg",
            folder = "book-folder",
            lastAccess = 10,
            dateAdded = 5,
            hash = "hash",
            isGhost = true,
            categoryIds = listOf("reading", "favorites"),
            lang = "en",
        )

        assertEquals("book-id", metadata.id)
        assertEquals("Book title", metadata.title)
        assertEquals("Author", metadata.author)
        assertEquals("cover.jpg", metadata.cover)
        assertEquals("book-folder", metadata.folder)
        assertEquals(10, metadata.lastAccess)
        assertEquals(5, metadata.dateAdded)
        assertEquals("hash", metadata.hash)
        assertTrue(metadata.isGhost)
        assertEquals(listOf("reading", "favorites"), metadata.categoryIds)
        assertEquals("en", metadata.lang)
    }

    private companion object {
        val UUID_PATTERN = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    }
}
