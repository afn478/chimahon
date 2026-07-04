package tachiyomi.domain.library.model

import tachiyomi.domain.category.model.Category
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class LibraryFlagsTest {

    @Test
    fun `Check the amount of flags`() {
        assertEquals(5, LibraryDisplayMode.values.size)
        assertEquals(11, LibrarySort.types.size)
        assertEquals(2, LibrarySort.directions.size)
    }

    @Test
    fun `Test Flag plus operator with LibrarySort`() {
        val current = LibrarySort(LibrarySort.Type.LastRead, LibrarySort.Direction.Ascending)
        val new = LibrarySort(LibrarySort.Type.DateAdded, LibrarySort.Direction.Ascending)
        val flag = current + new

        assertEquals(0b01011100L, flag)
    }

    @Test
    fun `Test Flag plus operator`() {
        val sort = LibrarySort(LibrarySort.Type.DateAdded, LibrarySort.Direction.Ascending)

        assertEquals(0b01011100L, sort.flag)
    }

    @Test
    fun `Test Flag plus operator with old flag as base`() {
        val currentSort = LibrarySort(
            LibrarySort.Type.UnreadCount,
            LibrarySort.Direction.Descending,
        )
        assertEquals(0b00001100L, currentSort.flag)

        val sort = LibrarySort(LibrarySort.Type.DateAdded, LibrarySort.Direction.Ascending)
        val flag = currentSort.flag + sort

        assertEquals(0b01011100L, flag)
        assertNotEquals(currentSort.flag, flag)
    }

    @Test
    fun `Test default flags`() {
        val sort = LibrarySort.default
        val flag = sort.type + sort.direction

        assertEquals(0b01000000L, flag)
    }

    @Test
    fun `LibraryDisplayMode round trips serialized values`() {
        LibraryDisplayMode.values.forEach { mode ->
            assertEquals(mode, LibraryDisplayMode.deserialize(mode.serialize()))
        }

        assertEquals(LibraryDisplayMode.default, LibraryDisplayMode.deserialize("missing"))
    }

    @Test
    fun `LibrarySort round trips serialized values and flags`() {
        val sort = LibrarySort(LibrarySort.Type.TagList, LibrarySort.Direction.Descending)

        assertEquals(sort, LibrarySort.deserialize(sort.serialize()))
        assertEquals(sort, LibrarySort.valueOf(sort.flag))
        assertEquals(
            sort,
            Category(id = 1, name = "Tags", order = 0, flags = sort.flag, hidden = false).sort,
        )
        assertEquals(LibrarySort.default, LibrarySort.deserialize(""))
        assertEquals(LibrarySort.default, LibrarySort.valueOf(null))
    }
}
