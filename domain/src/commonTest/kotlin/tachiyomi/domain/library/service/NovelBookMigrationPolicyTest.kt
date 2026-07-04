package tachiyomi.domain.library.service

import tachiyomi.domain.library.model.NovelBookMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NovelBookMigrationPolicyTest {
    @Test
    fun migrateExistingMetadataWithoutHashUsesParsedIdentityAndPreservesExistingMetadata() {
        val result = NovelBookMigrationPolicy.migrateExistingMetadataWithoutHash(
            existingMetadata = NovelBookMetadata(
                id = "legacy",
                title = "Stored Title",
                author = null,
                cover = "/books/legacy/cover.jpg",
                folder = "legacy",
                lastAccess = 10,
                dateAdded = 5,
                isGhost = true,
                categoryIds = listOf("reading"),
                lang = "en",
            ),
            parsedTitle = "Parsed Title",
            parsedAuthor = "Author",
            currentFolderName = "legacy",
        )

        assertEquals("7a3118af54f01e7d00686ab23573cdc8", result.stableId)
        assertEquals("Parsed Title", result.title)
        assertEquals("Author", result.author)
        assertEquals("7a3118af54f01e7d00686ab23573cdc8", result.metadata.id)
        assertEquals("Stored Title", result.metadata.title)
        assertEquals("Author", result.metadata.author)
        assertEquals("/books/7a3118af54f01e7d00686ab23573cdc8/cover.jpg", result.metadata.cover)
        assertEquals("7a3118af54f01e7d00686ab23573cdc8", result.metadata.folder)
        assertEquals(10, result.metadata.lastAccess)
        assertEquals(5, result.metadata.dateAdded)
        assertEquals("7a3118af54f01e7d00686ab23573cdc8", result.metadata.hash)
        assertEquals(true, result.metadata.isGhost)
        assertEquals(listOf("reading"), result.metadata.categoryIds)
        assertEquals("en", result.metadata.lang)
    }

    @Test
    fun migrateExistingMetadataWithoutHashFallsBackToExistingTitleAndUnknown() {
        val existingTitleResult = NovelBookMigrationPolicy.migrateExistingMetadataWithoutHash(
            existingMetadata = NovelBookMetadata(title = "Stored Title"),
            parsedTitle = null,
            parsedAuthor = null,
            currentFolderName = "legacy",
        )
        val unknownTitleResult = NovelBookMigrationPolicy.migrateExistingMetadataWithoutHash(
            existingMetadata = NovelBookMetadata(title = null),
            parsedTitle = null,
            parsedAuthor = null,
            currentFolderName = "legacy",
        )

        assertEquals("84707625897b5eb701bf70acc6293269", existingTitleResult.stableId)
        assertEquals("Stored Title", existingTitleResult.title)
        assertNull(existingTitleResult.metadata.author)
        assertEquals("98b2de4cbcc6b642901948f1fffa2299", unknownTitleResult.stableId)
        assertEquals("Unknown", unknownTitleResult.metadata.title)
    }

    @Test
    fun migrateParsedBookWithoutExistingMetadataBuildsStableMetadata() {
        val result = NovelBookMigrationPolicy.migrateParsedBookWithoutExistingMetadata(
            parsedTitle = "Book",
            parsedAuthor = "Author",
            coverPath = "/books/legacy/cover.jpg",
            migrationTimeMillis = 1234,
        )

        assertEquals("8a91ea2d2e067402ca83c2d29bb7f69f", result.stableId)
        assertEquals("Book", result.title)
        assertEquals("Author", result.author)
        assertEquals("8a91ea2d2e067402ca83c2d29bb7f69f", result.metadata.id)
        assertEquals("Book", result.metadata.title)
        assertEquals("Author", result.metadata.author)
        assertEquals("/books/legacy/cover.jpg", result.metadata.cover)
        assertEquals("8a91ea2d2e067402ca83c2d29bb7f69f", result.metadata.folder)
        assertEquals(1234, result.metadata.lastAccess)
        assertEquals(1234, result.metadata.dateAdded)
        assertEquals("8a91ea2d2e067402ca83c2d29bb7f69f", result.metadata.hash)
        assertEquals(false, result.metadata.isGhost)
    }

    @Test
    fun migrateParsedBookWithoutExistingMetadataUsesLegacyFallbacks() {
        val result = NovelBookMigrationPolicy.migrateParsedBookWithoutExistingMetadata(
            parsedTitle = null,
            parsedAuthor = null,
            coverPath = null,
            migrationTimeMillis = 1234,
        )

        assertEquals("98b2de4cbcc6b642901948f1fffa2299", result.stableId)
        assertEquals("Unknown", result.metadata.title)
        assertEquals("", result.metadata.author)
    }

    @Test
    fun correctStableDirectoryMetadataReturnsNullWhenTitleIsMissing() {
        val result = NovelBookMigrationPolicy.correctStableDirectoryMetadata(
            metadata = NovelBookMetadata(title = null),
            currentFolderName = "legacy",
        )

        assertNull(result)
    }

    @Test
    fun correctStableDirectoryMetadataRewritesStableFieldsAndCover() {
        val result = NovelBookMigrationPolicy.correctStableDirectoryMetadata(
            metadata = NovelBookMetadata(
                id = "legacy",
                title = "Book",
                author = "Author",
                cover = "/books/legacy/cover.jpg",
                folder = "legacy",
                hash = "legacy",
            ),
            currentFolderName = "legacy",
        )

        assertEquals("8a91ea2d2e067402ca83c2d29bb7f69f", result?.stableId)
        assertEquals("8a91ea2d2e067402ca83c2d29bb7f69f", result?.metadata?.id)
        assertEquals("8a91ea2d2e067402ca83c2d29bb7f69f", result?.metadata?.folder)
        assertEquals("8a91ea2d2e067402ca83c2d29bb7f69f", result?.metadata?.hash)
        assertEquals("/books/8a91ea2d2e067402ca83c2d29bb7f69f/cover.jpg", result?.metadata?.cover)
    }
}
