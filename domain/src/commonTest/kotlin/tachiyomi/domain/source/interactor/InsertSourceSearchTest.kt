package tachiyomi.domain.source.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.repository.SavedSearchRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InsertSourceSearchTest {
    @Test
    fun insertSavedSearchForwardsSingleAndBulkInserts() = runTest {
        val repository = FakeSavedSearchRepository()
        val insertSavedSearch = InsertSavedSearch(repository)

        assertEquals(1, insertSavedSearch.await(savedSearch(id = 1, source = 10)))
        insertSavedSearch.awaitAll(
            listOf(
                savedSearch(id = 2, source = 10),
                savedSearch(id = 3, source = 20),
            ),
        )

        assertEquals(listOf(1L, 2L, 3L), repository.insertedSavedSearches.map(SavedSearch::id))
        assertEquals(listOf(1L, 2L), repository.getBySourceId(10).map(SavedSearch::id))
    }

    @Test
    fun insertSavedSearchFallsBackWhenRepositoryFails() = runTest {
        val repository = FakeSavedSearchRepository(failWrites = true)
        val insertSavedSearch = InsertSavedSearch(repository)

        assertNull(insertSavedSearch.await(savedSearch(id = 1, source = 10)))
        insertSavedSearch.awaitAll(listOf(savedSearch(id = 2, source = 10)))

        assertEquals(emptyList(), repository.insertedSavedSearches)
    }

    @Test
    fun insertFeedSavedSearchForwardsSingleAndBulkInserts() = runTest {
        val repository = FakeFeedSavedSearchRepository()
        val insertFeedSavedSearch = InsertFeedSavedSearch(repository)

        assertEquals(1, insertFeedSavedSearch.await(feed(id = 1, source = 10, global = true)))
        insertFeedSavedSearch.awaitAll(
            listOf(
                feed(id = 2, source = 10, global = false),
                feed(id = 3, source = 20, global = true),
            ),
        )

        assertEquals(listOf(1L, 2L, 3L), repository.insertedFeeds.map(FeedSavedSearch::id))
        assertEquals(listOf(1L, 3L), repository.getGlobal().map(FeedSavedSearch::id))
    }

    @Test
    fun insertFeedSavedSearchFallsBackWhenRepositoryFails() = runTest {
        val repository = FakeFeedSavedSearchRepository(failWrites = true)
        val insertFeedSavedSearch = InsertFeedSavedSearch(repository)

        assertNull(insertFeedSavedSearch.await(feed(id = 1, source = 10, global = true)))
        insertFeedSavedSearch.awaitAll(listOf(feed(id = 2, source = 10, global = false)))

        assertEquals(emptyList(), repository.insertedFeeds)
    }
}

private fun savedSearch(
    id: Long,
    source: Long,
): SavedSearch {
    return SavedSearch(
        id = id,
        source = source,
        name = "Saved $id",
        query = "query-$id",
        filtersJson = null,
    )
}

private class FakeSavedSearchRepository(
    private val failWrites: Boolean = false,
) : SavedSearchRepository {
    private val searches = MutableStateFlow(emptyMap<Long, SavedSearch>())
    val insertedSavedSearches = mutableListOf<SavedSearch>()

    override suspend fun getById(savedSearchId: Long): SavedSearch? {
        return searches.value[savedSearchId]
    }

    override suspend fun getBySourceId(sourceId: Long): List<SavedSearch> {
        return searches.value.values
            .filter { it.source == sourceId }
            .sortedBy(SavedSearch::id)
    }

    override fun getBySourceIdAsFlow(sourceId: Long): Flow<List<SavedSearch>> {
        return searches.map { entries ->
            entries.values
                .filter { it.source == sourceId }
                .sortedBy(SavedSearch::id)
        }
    }

    override suspend fun delete(savedSearchId: Long) {
        searches.value = searches.value - savedSearchId
    }

    override suspend fun insert(savedSearch: SavedSearch): Long? {
        failWriteIfNeeded()
        insertedSavedSearches += savedSearch
        searches.value = searches.value + (savedSearch.id to savedSearch)
        return savedSearch.id
    }

    override suspend fun insertAll(savedSearch: List<SavedSearch>) {
        failWriteIfNeeded()
        insertedSavedSearches += savedSearch
        searches.value = searches.value + savedSearch.associateBy(SavedSearch::id)
    }

    private fun failWriteIfNeeded() {
        if (failWrites) {
            throw IllegalStateException("Write failed")
        }
    }
}
