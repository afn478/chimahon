package tachiyomi.domain.library.service

import tachiyomi.domain.library.model.NovelBookMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class NovelBookImportPolicyTest {
    @Test
    fun importedTitleAndAuthorKeepLegacyFallbacks() {
        assertEquals("Unknown", NovelBookImportPolicy.importedTitle(null))
        assertEquals("", NovelBookImportPolicy.importedAuthor(null))
        assertEquals("", NovelBookImportPolicy.importedTitle(""))
        assertEquals("", NovelBookImportPolicy.importedAuthor(""))
    }

    @Test
    fun stableIdForImportUsesImportedTitleAndAuthorFallbacks() {
        assertEquals(
            "8a91ea2d2e067402ca83c2d29bb7f69f",
            NovelBookImportPolicy.stableIdForImport(
                title = "Book",
                author = "Author",
            ),
        )
        assertEquals(
            "98b2de4cbcc6b642901948f1fffa2299",
            NovelBookImportPolicy.stableIdForImport(
                title = null,
                author = null,
            ),
        )
    }

    @Test
    fun metadataForImportedBookBuildsStableMetadataForNewBook() {
        val metadata = NovelBookImportPolicy.metadataForImportedBook(
            title = "Book",
            author = "Author",
            coverPath = "/books/book/cover.jpg",
            language = "en",
            existingMetadata = null,
            requestedCategoryIds = listOf("default", "classics"),
            importTimeMillis = 1234,
            uncategorizedCategoryId = "default",
        )

        assertEquals("8a91ea2d2e067402ca83c2d29bb7f69f", metadata.id)
        assertEquals("Book", metadata.title)
        assertEquals("Author", metadata.author)
        assertEquals("/books/book/cover.jpg", metadata.cover)
        assertEquals("8a91ea2d2e067402ca83c2d29bb7f69f", metadata.folder)
        assertEquals(1234, metadata.lastAccess)
        assertEquals(1234, metadata.dateAdded)
        assertEquals("8a91ea2d2e067402ca83c2d29bb7f69f", metadata.hash)
        assertFalse(metadata.isGhost)
        assertEquals("en", metadata.lang)
        assertEquals(listOf("classics"), metadata.categoryIds)
    }

    @Test
    fun metadataForImportedBookPreservesExistingAccessDatesAndMergesCategories() {
        val metadata = NovelBookImportPolicy.metadataForImportedBook(
            title = "Book",
            author = "Author",
            coverPath = "/new/cover.jpg",
            language = "ja",
            existingMetadata = NovelBookMetadata(
                id = "old-id",
                title = "Old",
                author = "Old author",
                cover = "/old/cover.jpg",
                folder = "old-id",
                lastAccess = 10,
                dateAdded = 5,
                hash = "old-id",
                isGhost = true,
                categoryIds = listOf("default", "reading"),
                lang = "en",
            ),
            requestedCategoryIds = listOf("reading", "classics"),
            importTimeMillis = 1234,
            uncategorizedCategoryId = "default",
        )

        assertEquals(10, metadata.lastAccess)
        assertEquals(5, metadata.dateAdded)
        assertFalse(metadata.isGhost)
        assertEquals("ja", metadata.lang)
        assertEquals("/new/cover.jpg", metadata.cover)
        assertEquals(listOf("reading", "classics"), metadata.categoryIds)
    }

    @Test
    fun metadataForImportedBookKeepsExistingCategoriesWhenNoCategoryRequested() {
        val metadata = NovelBookImportPolicy.metadataForImportedBook(
            title = "Book",
            author = "Author",
            coverPath = null,
            language = null,
            existingMetadata = NovelBookMetadata(categoryIds = listOf("reading")),
            requestedCategoryIds = null,
            importTimeMillis = 1234,
            uncategorizedCategoryId = "default",
        )

        assertEquals(listOf("reading"), metadata.categoryIds)
    }
}
