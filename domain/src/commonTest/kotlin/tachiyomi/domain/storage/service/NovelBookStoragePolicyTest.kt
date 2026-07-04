package tachiyomi.domain.storage.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NovelBookStoragePolicyTest {
    @Test
    fun storageNamesRemainStable() {
        assertEquals("novels", NovelBookStoragePolicy.BOOKS_DIRECTORY)
        assertEquals("spine_cache.json", NovelBookStoragePolicy.OBSOLETE_SPINE_CACHE_FILE)
    }

    @Test
    fun importedContentExtensionsRemainStable() {
        assertEquals(
            setOf("opf", "xhtml", "html", "htm", "ncx"),
            NovelBookStoragePolicy.importedContentExtensions,
        )
    }

    @Test
    fun isImportedContentFileMatchesKnownBookContentFilesIgnoringCase() {
        assertTrue(NovelBookStoragePolicy.isImportedContentFile(isFile = true, extension = "opf"))
        assertTrue(NovelBookStoragePolicy.isImportedContentFile(isFile = true, extension = "XHTML"))
        assertFalse(NovelBookStoragePolicy.isImportedContentFile(isFile = false, extension = "opf"))
        assertFalse(NovelBookStoragePolicy.isImportedContentFile(isFile = true, extension = "jpg"))
    }

    @Test
    fun containsImportedContentMatchesAnyKnownContentFile() {
        val entries = listOf(
            NovelBookStorageEntry(isFile = false, extension = "opf"),
            NovelBookStorageEntry(isFile = true, extension = "jpg"),
            NovelBookStorageEntry(isFile = true, extension = "html"),
        )

        assertTrue(NovelBookStoragePolicy.containsImportedContent(entries))
    }

    @Test
    fun containsImportedContentReturnsFalseWithoutKnownContentFiles() {
        val entries = listOf(
            NovelBookStorageEntry(isFile = false, extension = "xhtml"),
            NovelBookStorageEntry(isFile = true, extension = "png"),
        )

        assertFalse(NovelBookStoragePolicy.containsImportedContent(entries))
    }
}
