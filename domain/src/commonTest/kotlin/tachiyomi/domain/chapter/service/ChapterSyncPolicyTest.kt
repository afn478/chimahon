package tachiyomi.domain.chapter.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChapterSyncPolicyTest {
    @Test
    fun prepareNewChaptersAssignsDescendingFetchDates() {
        val result = ChapterSyncPolicy.prepareNewChaptersForInsert(
            newChapters = listOf(
                chapter(url = "new-1", chapterNumber = 1.0),
                chapter(url = "new-2", chapterNumber = 2.0),
            ),
            dbChapters = emptyList(),
            removedChapters = emptyList(),
            nowMillis = NOW,
            markDuplicateAsRead = false,
            carryOverLastPageRead = false,
        )

        assertEquals(listOf(NOW + 2, NOW + 1), result.chaptersToAdd.map { it.dateFetch })
        assertEquals(emptySet(), result.changedOrDuplicateReadUrls)
    }

    @Test
    fun prepareNewChaptersMarksDuplicateReadChaptersAsReadWhenEnabled() {
        val result = ChapterSyncPolicy.prepareNewChaptersForInsert(
            newChapters = listOf(chapter(url = "new-2", chapterNumber = 2.0)),
            dbChapters = listOf(chapter(url = "old-2", chapterNumber = 2.0, read = true)),
            removedChapters = emptyList(),
            nowMillis = NOW,
            markDuplicateAsRead = true,
            carryOverLastPageRead = false,
        )

        assertTrue(result.chaptersToAdd.single().read)
        assertEquals(setOf("new-2"), result.changedOrDuplicateReadUrls)
    }

    @Test
    fun prepareNewChaptersRestoresStateFromRemovedChapterNumbers() {
        val result = ChapterSyncPolicy.prepareNewChaptersForInsert(
            newChapters = listOf(chapter(url = "readded-5", chapterNumber = 5.0)),
            dbChapters = emptyList(),
            removedChapters = listOf(
                chapter(
                    url = "old-5",
                    chapterNumber = 5.0,
                    read = true,
                    bookmark = true,
                    dateFetch = OLD_FETCH_DATE,
                ),
            ),
            nowMillis = NOW,
            markDuplicateAsRead = false,
            carryOverLastPageRead = false,
        )
        val chapter = result.chaptersToAdd.single()

        assertTrue(chapter.read)
        assertTrue(chapter.bookmark)
        assertEquals(OLD_FETCH_DATE, chapter.dateFetch)
        assertEquals(setOf("readded-5"), result.changedOrDuplicateReadUrls)
    }

    @Test
    fun prepareNewChaptersDoesNotRestoreUnrecognizedChapterNumbers() {
        val result = ChapterSyncPolicy.prepareNewChaptersForInsert(
            newChapters = listOf(chapter(url = "new-extra", chapterNumber = -1.0)),
            dbChapters = emptyList(),
            removedChapters = listOf(
                chapter(url = "old-extra", chapterNumber = -1.0, read = true, bookmark = true),
            ),
            nowMillis = NOW,
            markDuplicateAsRead = false,
            carryOverLastPageRead = false,
        )
        val chapter = result.chaptersToAdd.single()

        assertFalse(chapter.read)
        assertFalse(chapter.bookmark)
        assertEquals(NOW + 1, chapter.dateFetch)
        assertEquals(emptySet(), result.changedOrDuplicateReadUrls)
    }

    @Test
    fun prepareNewChaptersCarriesOverLastPageReadOnlyToNewEhChapters() {
        val result = ChapterSyncPolicy.prepareNewChaptersForInsert(
            newChapters = listOf(
                chapter(url = "readded-1", chapterNumber = 1.0),
                chapter(url = "new-2", chapterNumber = 2.0),
            ),
            dbChapters = listOf(chapter(url = "old-read", chapterNumber = 10.0, lastPageRead = 12)),
            removedChapters = listOf(chapter(url = "old-1", chapterNumber = 1.0, lastPageRead = 3)),
            nowMillis = NOW,
            markDuplicateAsRead = false,
            carryOverLastPageRead = true,
        )

        assertEquals(0, result.chaptersToAdd[0].lastPageRead)
        assertEquals(12, result.chaptersToAdd[1].lastPageRead)
        assertEquals(setOf("readded-1"), result.changedOrDuplicateReadUrls)
    }

    private fun chapter(
        url: String,
        chapterNumber: Double,
        read: Boolean = false,
        bookmark: Boolean = false,
        lastPageRead: Long = 0,
        dateFetch: Long = 0,
    ) = tachiyomi.domain.chapter.model.Chapter.create().copy(
        url = url,
        name = url,
        chapterNumber = chapterNumber,
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead,
        dateFetch = dateFetch,
    )

    private companion object {
        const val NOW = 1_000L
        const val OLD_FETCH_DATE = 500L
    }
}
