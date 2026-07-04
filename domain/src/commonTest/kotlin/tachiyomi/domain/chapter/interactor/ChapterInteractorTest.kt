package tachiyomi.domain.chapter.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChapterInteractorTest {
    @Test
    fun getChapterReturnsStoredChapterAndFallsBackToNull() = runTest {
        val repository = FakeChapterRepository(chapter(id = 1, mangaId = 10, url = "chapter-1"))

        assertEquals(repository.getChapterById(1), GetChapter(repository).await(1))
        assertEquals(repository.getChapterById(1), GetChapter(repository).await("chapter-1", mangaId = 10))

        assertNull(GetChapter(FakeChapterRepository(failReads = true)).await(1))
        assertNull(GetChapter(FakeChapterRepository(failReads = true)).await("chapter-1", mangaId = 10))
    }

    @Test
    fun listChapterInteractorsReturnRepositoryResultsAndFallBackToEmptyLists() = runTest {
        val repository = FakeChapterRepository(
            chapter(id = 1, mangaId = 10, url = "shared-url"),
            chapter(id = 2, mangaId = 20, url = "shared-url"),
        )

        assertEquals(2, GetChapterByUrl(repository).await("shared-url").size)
        assertEquals(listOf(1L), GetChaptersByMangaId(repository).await(10).map(Chapter::id))

        val failingRepository = FakeChapterRepository(failReads = true)
        assertEquals(emptyList(), GetChapterByUrl(failingRepository).await("shared-url"))
        assertEquals(emptyList(), GetChaptersByMangaId(failingRepository).await(10))
    }

    @Test
    fun updateAndDeleteInteractorsForwardRepositoryOperations() = runTest {
        val repository = FakeChapterRepository(
            chapter(id = 1, mangaId = 10, name = "Old"),
            chapter(id = 2, mangaId = 10),
        )
        val updateChapter = UpdateChapter(repository)

        updateChapter.await(ChapterUpdate(id = 1, name = "New"))
        updateChapter.awaitAll(listOf(ChapterUpdate(id = 2, read = true)))
        assertEquals("New", repository.getChapterById(1)?.name)
        assertEquals(true, repository.getChapterById(2)?.read)

        DeleteChapters(repository).await(listOf(1))

        assertEquals(listOf(1L, 2L), repository.appliedUpdates.map(ChapterUpdate::id))
        assertEquals(listOf(listOf(1L)), repository.removedIds)
        assertNull(repository.getChapterById(1))
    }

    @Test
    fun shouldUpdateDbChapterOnlyTracksSourceMetadata() {
        val shouldUpdateDbChapter = ShouldUpdateDbChapter()
        val dbChapter = chapter(id = 1, mangaId = 10)

        assertFalse(shouldUpdateDbChapter.await(dbChapter, dbChapter.copy(read = true, bookmark = true)))
        assertTrue(shouldUpdateDbChapter.await(dbChapter, dbChapter.copy(scanlator = "team")))
        assertTrue(shouldUpdateDbChapter.await(dbChapter, dbChapter.copy(name = "Renamed")))
        assertTrue(shouldUpdateDbChapter.await(dbChapter, dbChapter.copy(dateUpload = 42)))
        assertTrue(shouldUpdateDbChapter.await(dbChapter, dbChapter.copy(chapterNumber = 2.0)))
        assertTrue(shouldUpdateDbChapter.await(dbChapter, dbChapter.copy(sourceOrder = 3)))
    }
}

private fun chapter(
    id: Long,
    mangaId: Long,
    url: String = "chapter-$id",
    name: String = "Chapter $id",
): Chapter {
    return Chapter.create().copy(
        id = id,
        mangaId = mangaId,
        url = url,
        name = name,
        chapterNumber = id.toDouble(),
        sourceOrder = id,
    )
}

private class FakeChapterRepository(
    vararg chapters: Chapter,
    private val failReads: Boolean = false,
) : ChapterRepository {
    private val chapters = MutableStateFlow(chapters.associateBy(Chapter::id))
    val appliedUpdates = mutableListOf<ChapterUpdate>()
    val removedIds = mutableListOf<List<Long>>()

    override suspend fun addAll(chapters: List<Chapter>): List<Chapter> {
        this.chapters.value = this.chapters.value + chapters.associateBy(Chapter::id)
        return chapters
    }

    override suspend fun update(chapterUpdate: ChapterUpdate) {
        updateAll(listOf(chapterUpdate))
    }

    override suspend fun updateAll(chapterUpdates: List<ChapterUpdate>) {
        appliedUpdates += chapterUpdates
        chapters.value = chapters.value.mapValues { (_, chapter) ->
            chapterUpdates.firstOrNull { it.id == chapter.id }?.let { update ->
                chapter.copy(
                    mangaId = update.mangaId ?: chapter.mangaId,
                    read = update.read ?: chapter.read,
                    bookmark = update.bookmark ?: chapter.bookmark,
                    lastPageRead = update.lastPageRead ?: chapter.lastPageRead,
                    dateFetch = update.dateFetch ?: chapter.dateFetch,
                    sourceOrder = update.sourceOrder ?: chapter.sourceOrder,
                    url = update.url ?: chapter.url,
                    name = update.name ?: chapter.name,
                    dateUpload = update.dateUpload ?: chapter.dateUpload,
                    chapterNumber = update.chapterNumber ?: chapter.chapterNumber,
                    scanlator = update.scanlator ?: chapter.scanlator,
                    version = update.version ?: chapter.version,
                    isOcrReady = update.isOcrReady ?: chapter.isOcrReady,
                )
            } ?: chapter
        }
    }

    override suspend fun removeChaptersWithIds(chapterIds: List<Long>) {
        removedIds += chapterIds
        chapters.value = chapters.value - chapterIds.toSet()
    }

    override suspend fun getChapterByMangaId(mangaId: Long, applyFilter: Boolean): List<Chapter> {
        failReadIfNeeded()
        return chapters.value.values.filter { it.mangaId == mangaId }
    }

    override suspend fun getScanlatorsByMangaId(mangaId: Long): List<String> {
        return getChapterByMangaId(mangaId, applyFilter = false).mapNotNull(Chapter::scanlator).distinct()
    }

    override fun getScanlatorsByMangaIdAsFlow(mangaId: Long): Flow<List<String>> {
        return chapters.map { entries ->
            entries.values.filter { it.mangaId == mangaId }.mapNotNull(Chapter::scanlator).distinct()
        }
    }

    override suspend fun getBookmarkedChaptersByMangaId(mangaId: Long): List<Chapter> {
        return getChapterByMangaId(mangaId, applyFilter = false).filter(Chapter::bookmark)
    }

    override suspend fun getChapterById(id: Long): Chapter? {
        failReadIfNeeded()
        return chapters.value[id]
    }

    override suspend fun getChapterByMangaIdAsFlow(mangaId: Long, applyFilter: Boolean): Flow<List<Chapter>> {
        failReadIfNeeded()
        return chapters.map { entries -> entries.values.filter { it.mangaId == mangaId } }
    }

    override suspend fun getChapterByUrlAndMangaId(url: String, mangaId: Long): Chapter? {
        failReadIfNeeded()
        return chapters.value.values.firstOrNull { it.url == url && it.mangaId == mangaId }
    }

    override suspend fun getChapterByUrl(url: String): List<Chapter> {
        failReadIfNeeded()
        return chapters.value.values.filter { it.url == url }
    }

    override suspend fun getMergedChapterByMangaId(mangaId: Long, applyFilter: Boolean): List<Chapter> {
        return getChapterByMangaId(mangaId, applyFilter)
    }

    override suspend fun getMergedChapterByMangaIdAsFlow(
        mangaId: Long,
        applyFilter: Boolean,
    ): Flow<List<Chapter>> {
        return getChapterByMangaIdAsFlow(mangaId, applyFilter)
    }

    override suspend fun getScanlatorsByMergeId(mangaId: Long): List<String> {
        return getScanlatorsByMangaId(mangaId)
    }

    override fun getScanlatorsByMergeIdAsFlow(mangaId: Long): Flow<List<String>> {
        return getScanlatorsByMangaIdAsFlow(mangaId)
    }

    private fun failReadIfNeeded() {
        if (failReads) {
            throw IllegalStateException("Read failed")
        }
    }
}
