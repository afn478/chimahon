package tachiyomi.domain.selection.service

data class SelectableListItem<T>(
    val id: T,
    val selected: Boolean,
)

data class ListSelectionState<T>(
    val selectedIds: Set<T> = emptySet(),
    val firstSelectedIndex: Int = NO_SELECTION,
    val lastSelectedIndex: Int = NO_SELECTION,
) {
    companion object {
        const val NO_SELECTION = -1
    }
}

data class ListSelectionResult<T>(
    val items: List<SelectableListItem<T>>,
    val state: ListSelectionState<T>,
)

object ListSelectionPolicy {
    fun <T> selectItems(
        itemIds: List<T>,
        selectedIds: Set<T>,
    ): List<SelectableListItem<T>> {
        return itemIds.map { id ->
            SelectableListItem(
                id = id,
                selected = id in selectedIds,
            )
        }
    }

    fun <T> toggleSelection(
        items: List<SelectableListItem<T>>,
        state: ListSelectionState<T>,
        targetId: T,
        selected: Boolean,
        userSelected: Boolean = false,
        fromLongPress: Boolean = false,
        additionalTargetIds: Set<T> = emptySet(),
    ): ListSelectionResult<T> {
        val selectedIndex = items.indexOfFirst { it.id == targetId }
        if (selectedIndex < 0) {
            return ListSelectionResult(items, state)
        }

        val selectedItem = items[selectedIndex]
        if (selectedItem.selected == selected) {
            return ListSelectionResult(items, state)
        }

        val updatedItems = items.toMutableList()
        val updatedSelectedIds = state.selectedIds.toMutableSet()
        var firstSelectedIndex = state.firstSelectedIndex
        var lastSelectedIndex = state.lastSelectedIndex

        val firstSelection = updatedItems.none { it.selected }
        updatedItems[selectedIndex] = selectedItem.copy(selected = selected)
        updatedSelectedIds.addOrRemove(targetId, selected)

        if (additionalTargetIds.isNotEmpty()) {
            updatedItems.forEachIndexed { index, item ->
                if (item.id in additionalTargetIds && item.selected != selected) {
                    updatedItems[index] = item.copy(selected = selected)
                }
            }
            additionalTargetIds.forEach { id ->
                updatedSelectedIds.addOrRemove(id, selected)
            }
        }

        if (selected && userSelected && fromLongPress) {
            if (firstSelection) {
                firstSelectedIndex = selectedIndex
                lastSelectedIndex = selectedIndex
            } else {
                val range: IntRange
                if (selectedIndex < firstSelectedIndex) {
                    range = selectedIndex + 1 until firstSelectedIndex
                    firstSelectedIndex = selectedIndex
                } else if (selectedIndex > lastSelectedIndex) {
                    range = (lastSelectedIndex + 1) until selectedIndex
                    lastSelectedIndex = selectedIndex
                } else {
                    range = IntRange.EMPTY
                }

                range.forEach { index ->
                    val inBetweenItem = updatedItems[index]
                    if (!inBetweenItem.selected) {
                        updatedSelectedIds.add(inBetweenItem.id)
                        updatedItems[index] = inBetweenItem.copy(selected = true)
                    }
                }
            }
        } else if (userSelected && !fromLongPress) {
            if (!selected) {
                if (selectedIndex == firstSelectedIndex) {
                    firstSelectedIndex = updatedItems.indexOfFirst { it.selected }
                } else if (selectedIndex == lastSelectedIndex) {
                    lastSelectedIndex = updatedItems.indexOfLast { it.selected }
                }
            } else {
                if (selectedIndex < firstSelectedIndex) {
                    firstSelectedIndex = selectedIndex
                } else if (selectedIndex > lastSelectedIndex) {
                    lastSelectedIndex = selectedIndex
                }
            }
        }

        return ListSelectionResult(
            items = updatedItems,
            state = state.copy(
                selectedIds = updatedSelectedIds,
                firstSelectedIndex = firstSelectedIndex,
                lastSelectedIndex = lastSelectedIndex,
            ),
        )
    }

    fun <T> toggleAllSelection(
        items: List<SelectableListItem<T>>,
        state: ListSelectionState<T>,
        selected: Boolean,
    ): ListSelectionResult<T> {
        val updatedSelectedIds = state.selectedIds.toMutableSet()
        val updatedItems = items.map { item ->
            updatedSelectedIds.addOrRemove(item.id, selected)
            item.copy(selected = selected)
        }

        return ListSelectionResult(
            items = updatedItems,
            state = state.copy(
                selectedIds = updatedSelectedIds,
                firstSelectedIndex = ListSelectionState.NO_SELECTION,
                lastSelectedIndex = ListSelectionState.NO_SELECTION,
            ),
        )
    }

    fun <T> invertSelection(
        items: List<SelectableListItem<T>>,
        state: ListSelectionState<T>,
    ): ListSelectionResult<T> {
        val updatedSelectedIds = state.selectedIds.toMutableSet()
        val updatedItems = items.map { item ->
            val selected = !item.selected
            updatedSelectedIds.addOrRemove(item.id, selected)
            item.copy(selected = selected)
        }

        return ListSelectionResult(
            items = updatedItems,
            state = state.copy(
                selectedIds = updatedSelectedIds,
                firstSelectedIndex = ListSelectionState.NO_SELECTION,
                lastSelectedIndex = ListSelectionState.NO_SELECTION,
            ),
        )
    }

    private fun <T> MutableSet<T>.addOrRemove(
        value: T,
        shouldAdd: Boolean,
    ) {
        if (shouldAdd) {
            add(value)
        } else {
            remove(value)
        }
    }
}
