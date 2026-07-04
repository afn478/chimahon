package tachiyomi.domain.source.service

import tachiyomi.domain.source.model.MigrationSourceSortDirection
import tachiyomi.domain.source.model.MigrationSourceSortMode
import tachiyomi.domain.source.model.Source

object MigrationSourceSortPolicy {
    fun sortSourcesWithFavoriteCount(
        sourcesWithCount: List<Pair<Source, Long>>,
        mode: MigrationSourceSortMode,
        direction: MigrationSourceSortDirection,
        compareNames: (String, String) -> Int,
        isLocalSource: (Source) -> Boolean,
    ): List<Pair<Source, Long>> {
        val comparator = sourceWithFavoriteCountComparator(
            mode = mode,
            direction = direction,
            compareNames = compareNames,
        )

        return sourcesWithCount
            .filterNot { isLocalSource(it.first) }
            .sortedWith(comparator)
    }

    fun sourceWithFavoriteCountComparator(
        mode: MigrationSourceSortMode,
        direction: MigrationSourceSortDirection,
        compareNames: (String, String) -> Int,
    ): Comparator<Pair<Source, Long>> {
        return Comparator { left, right ->
            when (direction) {
                MigrationSourceSortDirection.ASCENDING -> compare(left, right, mode, compareNames)
                MigrationSourceSortDirection.DESCENDING -> compare(right, left, mode, compareNames)
            }
        }
    }

    private fun compare(
        left: Pair<Source, Long>,
        right: Pair<Source, Long>,
        mode: MigrationSourceSortMode,
        compareNames: (String, String) -> Int,
    ): Int {
        return compareStubPriority(left.first, right.first) ?: when (mode) {
            MigrationSourceSortMode.ALPHABETICAL -> compareNames(left.first.name, right.first.name)
            MigrationSourceSortMode.TOTAL -> left.second.compareTo(right.second)
        }
    }

    private fun compareStubPriority(left: Source, right: Source): Int? {
        return when {
            left.isStub && !right.isStub -> -1
            right.isStub && !left.isStub -> 1
            else -> null
        }
    }
}
