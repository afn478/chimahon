package tachiyomi.domain.backup.service

import tachiyomi.domain.backup.model.NovelBackupRecord
import tachiyomi.domain.backup.model.NovelBackupStatistic
import tachiyomi.domain.library.model.NovelBookMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NovelBackupMergePolicyTest {
    @Test
    fun mergeDuplicateRecordKeepsCanonicalIdAndLatestRecordValues() {
        val first = record(
            id = "canonical-id",
            title = "First",
            author = "First author",
            cover = "first-cover.jpg",
            lastModified = 10,
            stats = listOf(
                stat(dateKey = "2026-01-01", modified = 1, charactersRead = 10),
                stat(dateKey = "2026-01-02", modified = 5, charactersRead = 20),
            ),
            categoryIds = listOf("default", "reading"),
            lang = "en",
        )
        val second = record(
            id = "duplicate-id",
            title = "Second",
            author = null,
            cover = null,
            lastModified = 20,
            stats = listOf(
                stat(dateKey = "2026-01-01", modified = 3, charactersRead = 30),
                stat(dateKey = "2026-01-03", modified = 1, charactersRead = 40),
            ),
            categoryIds = listOf("classics"),
            lang = null,
        )

        val result = NovelBackupMergePolicy.mergeDuplicateRecord(
            first = first,
            second = second,
            uncategorizedCategoryId = "default",
            copyRecord = ::copyRecord,
        )

        assertEquals("canonical-id", result.id)
        assertEquals("Second", result.title)
        assertEquals("First author", result.author)
        assertEquals("first-cover.jpg", result.cover)
        assertEquals(listOf("2026-01-01", "2026-01-02", "2026-01-03"), result.stats.map { it.dateKey })
        assertEquals(listOf(30, 20, 40), result.stats.map { it.charactersRead })
        assertEquals(listOf("reading", "classics"), result.categoryIds)
        assertEquals("en", result.lang)
    }

    @Test
    fun mergeDuplicateRecordKeepsFirstRecordWhenTimestampsTie() {
        val result = NovelBackupMergePolicy.mergeDuplicateRecord(
            first = record(id = "canonical-id", title = "First", lastModified = 10),
            second = record(id = "duplicate-id", title = "Second", lastModified = 10),
            uncategorizedCategoryId = "default",
            copyRecord = ::copyRecord,
        )

        assertEquals("canonical-id", result.id)
        assertEquals("First", result.title)
    }

    @Test
    fun mergeStatsKeepsLatestStatPerDate() {
        val result = NovelBackupMergePolicy.mergeStats(
            listOf(
                stat(dateKey = "2026-01-01", modified = 1, charactersRead = 10),
                stat(dateKey = "2026-01-02", modified = 5, charactersRead = 20),
                stat(dateKey = "2026-01-01", modified = 3, charactersRead = 30),
            ),
        )

        assertEquals(listOf("2026-01-01", "2026-01-02"), result.map { it.dateKey })
        assertEquals(listOf(30, 20), result.map { it.charactersRead })
    }

    @Test
    fun mergeRestoredMetadataFillsMissingFieldsAndClearsGhostWhenContentExists() {
        val result = NovelBackupMergePolicy.mergeRestoredMetadata(
            localMetadata = metadata(
                author = null,
                cover = "local-cover.jpg",
                lang = null,
                isGhost = true,
                categoryIds = listOf("default", "reading"),
            ),
            backupAuthor = "Backup author",
            backupCover = null,
            backupLang = "ja",
            backupCategoryIds = listOf("classics"),
            hasImportedContent = true,
            uncategorizedCategoryId = "default",
        )

        assertEquals("Backup author", result.author)
        assertEquals("local-cover.jpg", result.cover)
        assertEquals("ja", result.lang)
        assertFalse(result.isGhost)
        assertEquals(listOf("reading", "classics"), result.categoryIds)
    }

    @Test
    fun mergeRestoredMetadataKeepsGhostWhenNoImportedContentExists() {
        val result = NovelBackupMergePolicy.mergeRestoredMetadata(
            localMetadata = metadata(isGhost = true),
            backupAuthor = null,
            backupCover = null,
            backupLang = null,
            backupCategoryIds = emptyList(),
            hasImportedContent = false,
            uncategorizedCategoryId = "default",
        )

        assertTrue(result.isGhost)
    }

    private fun copyRecord(
        base: TestRecord,
        id: String,
        author: String?,
        cover: String?,
        stats: List<TestStat>,
        categoryIds: List<String>,
        lang: String?,
    ): TestRecord {
        return base.copy(
            id = id,
            author = author,
            cover = cover,
            stats = stats,
            categoryIds = categoryIds,
            lang = lang,
        )
    }

    private fun record(
        id: String = "id",
        title: String = "Title",
        author: String? = null,
        cover: String? = null,
        lastModified: Long = 0,
        stats: List<TestStat> = emptyList(),
        categoryIds: List<String> = emptyList(),
        lang: String? = null,
    ): TestRecord {
        return TestRecord(
            id = id,
            title = title,
            author = author,
            cover = cover,
            lastModified = lastModified,
            stats = stats,
            categoryIds = categoryIds,
            lang = lang,
        )
    }

    private fun stat(
        dateKey: String,
        modified: Long,
        charactersRead: Int,
    ): TestStat {
        return TestStat(
            dateKey = dateKey,
            lastStatisticModified = modified,
            charactersRead = charactersRead,
        )
    }

    private fun metadata(
        author: String? = null,
        cover: String? = null,
        lang: String? = null,
        isGhost: Boolean = false,
        categoryIds: List<String> = emptyList(),
    ): NovelBookMetadata {
        return NovelBookMetadata(
            id = "book-id",
            title = "Book title",
            author = author,
            cover = cover,
            folder = "book-id",
            lastAccess = 10,
            dateAdded = 5,
            hash = "book-id",
            isGhost = isGhost,
            categoryIds = categoryIds,
            lang = lang,
        )
    }

    private data class TestRecord(
        override val id: String,
        val title: String,
        override val author: String?,
        override val cover: String?,
        override val lastModified: Long,
        override val stats: List<TestStat>,
        override val categoryIds: List<String>,
        override val lang: String?,
    ) : NovelBackupRecord<TestStat>

    private data class TestStat(
        override val dateKey: String,
        override val lastStatisticModified: Long,
        val charactersRead: Int,
    ) : NovelBackupStatistic
}
