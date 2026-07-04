package tachiyomi.domain.source.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.FeedSavedSearchUpdate
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.repository.FeedSavedSearchRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class ReorderFeedTest {
    @Test
    fun changeOrderReordersGlobalFeed() = runTest {
        val repository = FakeFeedSavedSearchRepository(
            feed(1, source = 10, global = true, order = 0),
            feed(2, source = 10, global = true, order = 1),
            feed(3, source = 10, global = true, order = 2),
        )
        val reorderFeed = ReorderFeed(repository)

        val result = reorderFeed.changeOrder(
            feed(3, source = 10, global = true, order = 2),
            newIndex = 0,
        )

        assertEquals(ReorderFeed.Result.Success, result)
        assertEquals(listOf(3L, 1L, 2L), repository.getGlobal().map(FeedSavedSearch::id))
        assertEquals(listOf(0L, 1L, 2L), repository.getGlobal().map(FeedSavedSearch::feedOrder))
    }

    @Test
    fun changeOrderReordersSourceFeedWithoutChangingOtherSources() = runTest {
        val repository = FakeFeedSavedSearchRepository(
            feed(1, source = 10, global = false, order = 0),
            feed(2, source = 10, global = false, order = 1),
            feed(3, source = 20, global = false, order = 0),
        )
        val reorderFeed = ReorderFeed(repository)

        val result = reorderFeed.changeOrder(
            feed(2, source = 10, global = false, order = 1),
            newIndex = 0,
            global = false,
        )

        assertEquals(ReorderFeed.Result.Success, result)
        assertEquals(listOf(2L, 1L), repository.getBySourceId(10).map(FeedSavedSearch::id))
        assertEquals(listOf(3L), repository.getBySourceId(20).map(FeedSavedSearch::id))
    }

    @Test
    fun changeOrderReturnsUnchangedWhenFeedIsMissing() = runTest {
        val repository = FakeFeedSavedSearchRepository(
            feed(1, source = 10, global = true, order = 0),
        )
        val reorderFeed = ReorderFeed(repository)

        val result = reorderFeed.changeOrder(
            feed(99, source = 10, global = true, order = 0),
            newIndex = 0,
        )

        assertEquals(ReorderFeed.Result.Unchanged, result)
        assertEquals(emptyList(), repository.appliedUpdates)
    }
}

fun feed(
    id: Long,
    source: Long,
    global: Boolean,
    order: Long = id,
    savedSearch: Long? = null,
): FeedSavedSearch {
    return FeedSavedSearch(
        id = id,
        source = source,
        savedSearch = savedSearch,
        global = global,
        feedOrder = order,
    )
}

class FakeFeedSavedSearchRepository(
    vararg feeds: FeedSavedSearch,
    private val failWrites: Boolean = false,
) : FeedSavedSearchRepository {
    private val feeds = MutableStateFlow(feeds.associateBy(FeedSavedSearch::id))
    val appliedUpdates = mutableListOf<FeedSavedSearchUpdate>()
    val insertedFeeds = mutableListOf<FeedSavedSearch>()

    override suspend fun getGlobal(): List<FeedSavedSearch> {
        return feeds.value.values
            .filter(FeedSavedSearch::global)
            .sortedBy(FeedSavedSearch::feedOrder)
    }

    override fun getGlobalAsFlow(): Flow<List<FeedSavedSearch>> {
        return feeds.map { entries ->
            entries.values
                .filter(FeedSavedSearch::global)
                .sortedBy(FeedSavedSearch::feedOrder)
        }
    }

    override suspend fun getGlobalFeedSavedSearch(): List<SavedSearch> = emptyList()

    override suspend fun countGlobal(): Long = getGlobal().size.toLong()

    override suspend fun getBySourceId(sourceId: Long): List<FeedSavedSearch> {
        return feeds.value.values
            .filter { it.source == sourceId && !it.global }
            .sortedBy(FeedSavedSearch::feedOrder)
    }

    override fun getBySourceIdAsFlow(sourceId: Long): Flow<List<FeedSavedSearch>> {
        return feeds.map { entries ->
            entries.values
                .filter { it.source == sourceId && !it.global }
                .sortedBy(FeedSavedSearch::feedOrder)
        }
    }

    override suspend fun getBySourceIdFeedSavedSearch(sourceId: Long): List<SavedSearch> = emptyList()

    override suspend fun countBySourceId(sourceId: Long): Long = getBySourceId(sourceId).size.toLong()

    override suspend fun delete(feedSavedSearchId: Long) {
        feeds.value = feeds.value - feedSavedSearchId
    }

    override suspend fun insert(feedSavedSearch: FeedSavedSearch): Long? {
        failWriteIfNeeded()
        insertedFeeds += feedSavedSearch
        feeds.value = feeds.value + (feedSavedSearch.id to feedSavedSearch)
        return feedSavedSearch.id
    }

    override suspend fun insertAll(feedSavedSearch: List<FeedSavedSearch>) {
        failWriteIfNeeded()
        insertedFeeds += feedSavedSearch
        feeds.value = feeds.value + feedSavedSearch.associateBy(FeedSavedSearch::id)
    }

    override suspend fun updatePartial(update: FeedSavedSearchUpdate) {
        updatePartial(listOf(update))
    }

    override suspend fun updatePartial(updates: List<FeedSavedSearchUpdate>) {
        appliedUpdates += updates
        feeds.value = feeds.value.mapValues { (_, feed) ->
            updates.firstOrNull { it.id == feed.id }?.let { update ->
                feed.copy(
                    source = update.source ?: feed.source,
                    savedSearch = update.savedSearch ?: feed.savedSearch,
                    global = update.global ?: feed.global,
                    feedOrder = update.feedOrder ?: feed.feedOrder,
                )
            } ?: feed
        }
    }

    private fun failWriteIfNeeded() {
        if (failWrites) {
            throw IllegalStateException("Write failed")
        }
    }
}
