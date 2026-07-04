package tachiyomi.domain.library.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NovelBookIdentityPolicyTest {
    @Test
    fun titleAuthorIdentityInputNormalizesTitleAndAuthor() {
        val result = NovelBookIdentityPolicy.titleAuthorIdentityInput(
            title = "  Mars Colony  ",
            author = " Alice ",
        )

        assertEquals("mars colony|alice", result)
    }

    @Test
    fun titleAuthorIdentityInputKeepsAuthorOnlyIdentity() {
        val result = NovelBookIdentityPolicy.titleAuthorIdentityInput(
            title = " ",
            author = " Alice ",
        )

        assertEquals("|alice", result)
    }

    @Test
    fun titleAuthorIdentityInputReturnsNullWhenBothPartsAreBlank() {
        val result = NovelBookIdentityPolicy.titleAuthorIdentityInput(
            title = " ",
            author = null,
        )

        assertNull(result)
    }

    @Test
    fun identityKeyHashesTitleAuthorBeforeStoredHash() {
        val result = NovelBookIdentityPolicy.identityKey(
            title = " Mars ",
            author = " Alice ",
            storedHash = "stored",
            fallbackId = "fallback",
            hashIdentity = { "hash:$it" },
        )

        assertEquals("hash:mars|alice", result)
    }

    @Test
    fun identityKeyFallsBackToStoredHashBeforeId() {
        val storedHashResult = NovelBookIdentityPolicy.identityKey(
            title = null,
            author = null,
            storedHash = "stored",
            fallbackId = "fallback",
            hashIdentity = { "hash:$it" },
        )
        val fallbackResult = NovelBookIdentityPolicy.identityKey(
            title = null,
            author = null,
            storedHash = " ",
            fallbackId = "fallback",
            hashIdentity = { "hash:$it" },
        )

        assertEquals("stored", storedHashResult)
        assertEquals("fallback", fallbackResult)
    }

    @Test
    fun selectPreferredDuplicatePrefersImportedNonGhostStableDirectoryThenRecentAccess() {
        val result = NovelBookIdentityPolicy.selectPreferredDuplicate(
            books = listOf(
                book(id = "ghost-content", ghost = true, hasContent = true, folder = "stable", lastAccess = 500),
                book(id = "empty-stable", hasContent = false, folder = "stable", lastAccess = 600),
                book(id = "content-legacy", hasContent = true, folder = "legacy", lastAccess = 700),
                book(id = "content-stable-old", hasContent = true, folder = "stable", lastAccess = 400),
                book(id = "content-stable-new", hasContent = true, folder = "stable", lastAccess = 800),
            ),
            identityKey = "stable",
            isGhost = TestBook::ghost,
            hasImportedContent = TestBook::hasContent,
            folderName = TestBook::folder,
            lastAccess = TestBook::lastAccess,
        )

        assertEquals("content-stable-new", result?.id)
    }

    @Test
    fun deduplicateByIdentityKeepsPreferredBookPerIdentity() {
        val result = NovelBookIdentityPolicy.deduplicateByIdentity(
            books = listOf(
                book(id = "a-empty", identity = "a", hasContent = false, folder = "a", lastAccess = 100),
                book(id = "a-content", identity = "a", hasContent = true, folder = "a", lastAccess = 50),
                book(id = "b-only", identity = "b", hasContent = false, folder = "b", lastAccess = 10),
            ),
            identityKey = TestBook::identity,
            isGhost = TestBook::ghost,
            hasImportedContent = TestBook::hasContent,
            folderName = TestBook::folder,
            lastAccess = TestBook::lastAccess,
        )

        assertEquals(listOf("a-content", "b-only"), result.map(TestBook::id))
    }

    @Test
    fun selectPreferredDuplicateReturnsNullForEmptyInput() {
        val result = NovelBookIdentityPolicy.selectPreferredDuplicate(
            books = emptyList<TestBook>(),
            identityKey = "stable",
            isGhost = TestBook::ghost,
            hasImportedContent = TestBook::hasContent,
            folderName = TestBook::folder,
            lastAccess = TestBook::lastAccess,
        )

        assertNull(result)
    }

    private fun book(
        id: String,
        identity: String = "stable",
        ghost: Boolean = false,
        hasContent: Boolean = true,
        folder: String = identity,
        lastAccess: Long = 0,
    ): TestBook {
        return TestBook(
            id = id,
            identity = identity,
            ghost = ghost,
            hasContent = hasContent,
            folder = folder,
            lastAccess = lastAccess,
        )
    }

    private data class TestBook(
        val id: String,
        val identity: String,
        val ghost: Boolean,
        val hasContent: Boolean,
        val folder: String,
        val lastAccess: Long,
    )
}
