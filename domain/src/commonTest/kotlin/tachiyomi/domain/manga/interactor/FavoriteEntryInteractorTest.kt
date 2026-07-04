package tachiyomi.domain.manga.interactor

import kotlinx.coroutines.test.runTest
import tachiyomi.domain.manga.model.FavoriteEntry
import tachiyomi.domain.manga.model.FavoriteEntryAlternative
import tachiyomi.domain.manga.repository.FavoritesEntryRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoriteEntryInteractorTest {
    @Test
    fun favoriteEntryInteractorsForwardRepositoryOperations() = runTest {
        val repository = FakeFavoritesEntryRepository()
        val entries = listOf(
            FavoriteEntry(title = "First", gid = "100", token = "aaa", category = 1),
            FavoriteEntry(title = "Second", gid = "200", token = "bbb", category = 2),
        )
        val alternative = FavoriteEntryAlternative(
            otherGid = "201",
            otherToken = "ccc",
            gid = "200",
            token = "bbb",
        )

        InsertFavoriteEntries(repository).await(entries)
        InsertFavoriteEntryAlternative(repository).await(alternative)

        assertEquals(entries, GetFavoriteEntries(repository).await())
        assertEquals(listOf(alternative), repository.alternatives)

        DeleteFavoriteEntries(repository).await()

        assertEquals(emptyList(), GetFavoriteEntries(repository).await())
    }
}

private class FakeFavoritesEntryRepository : FavoritesEntryRepository {
    private val entries = mutableListOf<FavoriteEntry>()
    val alternatives = mutableListOf<FavoriteEntryAlternative>()

    override suspend fun deleteAll() {
        entries.clear()
    }

    override suspend fun insertAll(favoriteEntries: List<FavoriteEntry>) {
        entries += favoriteEntries
    }

    override suspend fun selectAll(): List<FavoriteEntry> {
        return entries.toList()
    }

    override suspend fun addAlternative(favoriteEntryAlternative: FavoriteEntryAlternative) {
        alternatives += favoriteEntryAlternative
    }
}
