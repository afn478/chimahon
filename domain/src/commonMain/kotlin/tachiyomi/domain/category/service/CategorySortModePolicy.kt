package tachiyomi.domain.category.service

import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.plus

object CategorySortModePolicy {
    fun flagsFor(sort: LibrarySort): Long {
        return sort.type + sort.direction
    }

    fun flagsFor(
        baseFlags: Long,
        type: LibrarySort.Type,
        direction: LibrarySort.Direction,
    ): Long {
        return baseFlags + type + direction
    }

    fun plan(
        groupLibraryMode: Int,
        category: Category?,
        categorizedDisplaySettings: Boolean,
        type: LibrarySort.Type,
        direction: LibrarySort.Direction,
    ): SortModePlan {
        val sort = LibrarySort(type, direction)
        if (groupLibraryMode != LibraryGroup.BY_DEFAULT) {
            return SortModePlan(globalSort = sort)
        }

        val flags = flagsFor(
            baseFlags = category?.flags ?: 0,
            type = type,
            direction = direction,
        )
        val refreshRandomSortSeed = type == LibrarySort.Type.Random

        return if (category != null && categorizedDisplaySettings) {
            SortModePlan(
                categoryUpdate = CategoryUpdate(
                    id = category.id,
                    flags = flags,
                ),
                refreshRandomSortSeed = refreshRandomSortSeed,
            )
        } else {
            SortModePlan(
                globalSort = sort,
                allCategoryFlags = flags,
                refreshRandomSortSeed = refreshRandomSortSeed,
            )
        }
    }

    data class SortModePlan(
        val globalSort: LibrarySort? = null,
        val categoryUpdate: CategoryUpdate? = null,
        val allCategoryFlags: Long? = null,
        val refreshRandomSortSeed: Boolean = false,
    )
}
