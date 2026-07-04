package tachiyomi.domain.selection.service

object SelectedItemPolicy {
    fun <T, ID> toggleItem(
        selectedItems: List<T>,
        item: T,
        selected: Boolean? = null,
        itemId: (T) -> ID,
    ): List<T> {
        val id = itemId(item)
        val isSelected = selectedItems.any { itemId(it) == id }
        val shouldSelect = selected ?: !isSelected

        return when {
            shouldSelect && !isSelected -> selectedItems + item
            !shouldSelect && isSelected -> selectedItems.filterNot { itemId(it) == id }
            else -> selectedItems
        }
    }

    fun <T, ID> invertVisibleSelection(
        visibleItems: List<T>,
        selectedItems: List<T>,
        itemId: (T) -> ID,
    ): List<T> {
        val selectedIds = selectedItems.mapTo(mutableSetOf(), itemId)

        return visibleItems
            .filterNot { itemId(it) in selectedIds }
            .distinctBy(itemId)
    }

    fun <T, ID> selectVisibleIds(
        visibleItems: List<T>,
        itemId: (T) -> ID,
    ): List<ID> {
        return visibleItems
            .map(itemId)
            .distinct()
    }

    fun <T, ID> invertVisibleIds(
        visibleItems: List<T>,
        selectedIds: Collection<ID>,
        itemId: (T) -> ID,
    ): List<ID> {
        val selectedIdSet = selectedIds.toSet()

        return visibleItems
            .map(itemId)
            .filterNot { it in selectedIdSet }
            .distinct()
    }
}
