package tachiyomi.domain.category.service

import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibrarySort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CategorySortModePolicyTest {
    @Test
    fun flagsForSortMatchesPersistedLibrarySortFlags() {
        val sort = LibrarySort(LibrarySort.Type.DateAdded, LibrarySort.Direction.Ascending)

        assertEquals(sort.flag, CategorySortModePolicy.flagsFor(sort))
    }

    @Test
    fun flagsForTypeAndDirectionPreservesUnrelatedBits() {
        val existingFlags = 0b10000000L

        assertEquals(
            0b11011100L,
            CategorySortModePolicy.flagsFor(
                baseFlags = existingFlags,
                type = LibrarySort.Type.DateAdded,
                direction = LibrarySort.Direction.Ascending,
            ),
        )
    }

    @Test
    fun groupedLibraryModeOnlyUpdatesGlobalSort() {
        val plan = CategorySortModePolicy.plan(
            groupLibraryMode = LibraryGroup.BY_SOURCE,
            category = category(id = 1, flags = 0),
            categorizedDisplaySettings = true,
            type = LibrarySort.Type.Random,
            direction = LibrarySort.Direction.Descending,
        )

        assertEquals(
            LibrarySort(LibrarySort.Type.Random, LibrarySort.Direction.Descending),
            plan.globalSort,
        )
        assertEquals(null, plan.categoryUpdate)
        assertEquals(null, plan.allCategoryFlags)
        assertFalse(plan.refreshRandomSortSeed)
    }

    @Test
    fun categorizedDisplayUpdatesOnlySelectedCategory() {
        val plan = CategorySortModePolicy.plan(
            groupLibraryMode = LibraryGroup.BY_DEFAULT,
            category = category(id = 3, flags = 0b10000000),
            categorizedDisplaySettings = true,
            type = LibrarySort.Type.UnreadCount,
            direction = LibrarySort.Direction.Descending,
        )

        assertEquals(
            CategoryUpdate(
                id = 3,
                flags = 0b10001100,
            ),
            plan.categoryUpdate,
        )
        assertEquals(null, plan.globalSort)
        assertEquals(null, plan.allCategoryFlags)
        assertFalse(plan.refreshRandomSortSeed)
    }

    @Test
    fun defaultDisplayUpdatesGlobalSortAndAllCategories() {
        val plan = CategorySortModePolicy.plan(
            groupLibraryMode = LibraryGroup.BY_DEFAULT,
            category = category(id = 3, flags = 0b10000000),
            categorizedDisplaySettings = false,
            type = LibrarySort.Type.UnreadCount,
            direction = LibrarySort.Direction.Descending,
        )

        assertEquals(
            LibrarySort(LibrarySort.Type.UnreadCount, LibrarySort.Direction.Descending),
            plan.globalSort,
        )
        assertEquals(null, plan.categoryUpdate)
        assertEquals(0b10001100L, plan.allCategoryFlags)
        assertFalse(plan.refreshRandomSortSeed)
    }

    @Test
    fun randomSortRefreshesSeedInDefaultLibraryMode() {
        val plan = CategorySortModePolicy.plan(
            groupLibraryMode = LibraryGroup.BY_DEFAULT,
            category = null,
            categorizedDisplaySettings = true,
            type = LibrarySort.Type.Random,
            direction = LibrarySort.Direction.Ascending,
        )

        assertEquals(
            LibrarySort(LibrarySort.Type.Random, LibrarySort.Direction.Ascending),
            plan.globalSort,
        )
        assertEquals(0b01111100L, plan.allCategoryFlags)
        assertTrue(plan.refreshRandomSortSeed)
    }

    private fun category(
        id: Long,
        flags: Long,
    ): Category {
        return Category(
            id = id,
            name = "Category $id",
            order = id,
            flags = flags,
            hidden = false,
        )
    }
}
