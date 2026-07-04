package tachiyomi.domain.source.service

import tachiyomi.domain.source.model.MigrationSourceSortDirection
import tachiyomi.domain.source.model.MigrationSourceSortMode
import tachiyomi.domain.source.model.Source
import kotlin.test.Test
import kotlin.test.assertEquals

class MigrationSourceSortPolicyTest {
    @Test
    fun sortSourcesWithFavoriteCountFiltersLocalSourcesAndSortsAlphabeticallyAscending() {
        val result = MigrationSourceSortPolicy.sortSourcesWithFavoriteCount(
            sourcesWithCount = listOf(
                source(id = LOCAL_SOURCE_ID, name = "Local") to 99,
                source(id = 1, name = "beta") to 2,
                source(id = 2, name = "Alpha") to 4,
                source(id = 3, name = "Zulu", isStub = true) to 1,
            ),
            mode = MigrationSourceSortMode.ALPHABETICAL,
            direction = MigrationSourceSortDirection.ASCENDING,
            compareNames = ::compareNames,
            isLocalSource = { it.id == LOCAL_SOURCE_ID },
        )

        assertEquals(listOf(3L, 2L, 1L), result.map { it.first.id })
    }

    @Test
    fun sortSourcesWithFavoriteCountReversesStubPriorityWhenDescending() {
        val result = MigrationSourceSortPolicy.sortSourcesWithFavoriteCount(
            sourcesWithCount = listOf(
                source(id = 1, name = "Alpha") to 4,
                source(id = 2, name = "beta") to 2,
                source(id = 3, name = "Zulu", isStub = true) to 1,
            ),
            mode = MigrationSourceSortMode.ALPHABETICAL,
            direction = MigrationSourceSortDirection.DESCENDING,
            compareNames = ::compareNames,
            isLocalSource = { false },
        )

        assertEquals(listOf(2L, 1L, 3L), result.map { it.first.id })
    }

    @Test
    fun sortSourcesWithFavoriteCountSortsByTotal() {
        val ascending = MigrationSourceSortPolicy.sortSourcesWithFavoriteCount(
            sourcesWithCount = listOf(
                source(id = 1, name = "One") to 9,
                source(id = 2, name = "Two") to 3,
                source(id = 3, name = "Stub", isStub = true) to 100,
            ),
            mode = MigrationSourceSortMode.TOTAL,
            direction = MigrationSourceSortDirection.ASCENDING,
            compareNames = ::compareNames,
            isLocalSource = { false },
        )
        val descending = MigrationSourceSortPolicy.sortSourcesWithFavoriteCount(
            sourcesWithCount = ascending,
            mode = MigrationSourceSortMode.TOTAL,
            direction = MigrationSourceSortDirection.DESCENDING,
            compareNames = ::compareNames,
            isLocalSource = { false },
        )

        assertEquals(listOf(3L, 2L, 1L), ascending.map { it.first.id })
        assertEquals(listOf(1L, 2L, 3L), descending.map { it.first.id })
    }

    private fun source(
        id: Long,
        name: String,
        isStub: Boolean = false,
    ): Source {
        return Source(
            id = id,
            lang = "en",
            name = name,
            supportsLatest = true,
            isStub = isStub,
        )
    }

    private companion object {
        const val LOCAL_SOURCE_ID = 99L

        fun compareNames(left: String, right: String): Int {
            return left.lowercase().compareTo(right.lowercase())
        }
    }
}
