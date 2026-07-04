package tachiyomi.domain.selection.service

import kotlin.test.Test
import kotlin.test.assertEquals

class SelectedItemPolicyTest {
    @Test
    fun toggleItemAddsUnselectedItem() {
        val result = SelectedItemPolicy.toggleItem(
            selectedItems = listOf(item(1)),
            item = item(2),
            itemId = TestItem::id,
        )

        assertEquals(listOf(item(1), item(2)), result)
    }

    @Test
    fun toggleItemDoesNotDuplicateSelectedItem() {
        val selectedItems = listOf(item(1), item(2))

        val result = SelectedItemPolicy.toggleItem(
            selectedItems = selectedItems,
            item = item(2, "Updated"),
            selected = true,
            itemId = TestItem::id,
        )

        assertEquals(selectedItems, result)
    }

    @Test
    fun toggleItemRemovesSelectedItemById() {
        val result = SelectedItemPolicy.toggleItem(
            selectedItems = listOf(item(1), item(2, "Stored")),
            item = item(2, "Visible"),
            selected = false,
            itemId = TestItem::id,
        )

        assertEquals(listOf(item(1)), result)
    }

    @Test
    fun invertVisibleSelectionKeepsVisibleItemsThatAreNotSelectedById() {
        val result = SelectedItemPolicy.invertVisibleSelection(
            visibleItems = listOf(
                item(1, "Visible"),
                item(2),
                item(2, "Duplicate"),
                item(3),
            ),
            selectedItems = listOf(item(1, "Stored"), item(4)),
            itemId = TestItem::id,
        )

        assertEquals(listOf(item(2), item(3)), result)
    }

    @Test
    fun selectVisibleIdsReturnsDistinctVisibleIds() {
        val result = SelectedItemPolicy.selectVisibleIds(
            visibleItems = listOf(item(1), item(2), item(2, "Duplicate")),
            itemId = TestItem::id,
        )

        assertEquals(listOf(1L, 2L), result)
    }

    @Test
    fun invertVisibleIdsReturnsDistinctIdsThatAreNotSelected() {
        val result = SelectedItemPolicy.invertVisibleIds(
            visibleItems = listOf(item(1), item(2), item(2, "Duplicate"), item(3)),
            selectedIds = listOf(1L, 4L),
            itemId = TestItem::id,
        )

        assertEquals(listOf(2L, 3L), result)
    }

    private fun item(
        id: Long,
        title: String = "Item $id",
    ): TestItem {
        return TestItem(
            id = id,
            title = title,
        )
    }

    private data class TestItem(
        val id: Long,
        val title: String,
    )
}
