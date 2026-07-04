package tachiyomi.domain.selection.service

import kotlin.test.Test
import kotlin.test.assertEquals

class ListSelectionPolicyTest {
    @Test
    fun selectItemsMarksSelectedIds() {
        val result = ListSelectionPolicy.selectItems(
            itemIds = listOf(1L, 2L, 3L),
            selectedIds = setOf(1L, 3L),
        )

        assertEquals(
            listOf(
                SelectableListItem(id = 1L, selected = true),
                SelectableListItem(id = 2L, selected = false),
                SelectableListItem(id = 3L, selected = true),
            ),
            result,
        )
    }

    @Test
    fun longPressFirstSelectionSetsRangeAnchors() {
        val result = ListSelectionPolicy.toggleSelection(
            items = ListSelectionPolicy.selectItems(listOf(1L, 2L, 3L), emptySet()),
            state = ListSelectionState(),
            targetId = 2L,
            selected = true,
            userSelected = true,
            fromLongPress = true,
        )

        assertEquals(setOf(2L), result.state.selectedIds)
        assertEquals(1, result.state.firstSelectedIndex)
        assertEquals(1, result.state.lastSelectedIndex)
        assertEquals(listOf(false, true, false), result.items.map { it.selected })
    }

    @Test
    fun longPressExtendsSelectionRangeAndSelectsInBetweenItems() {
        val firstResult = ListSelectionPolicy.toggleSelection(
            items = ListSelectionPolicy.selectItems(listOf(1L, 2L, 3L, 4L), emptySet()),
            state = ListSelectionState(),
            targetId = 1L,
            selected = true,
            userSelected = true,
            fromLongPress = true,
        )

        val result = ListSelectionPolicy.toggleSelection(
            items = firstResult.items,
            state = firstResult.state,
            targetId = 4L,
            selected = true,
            userSelected = true,
            fromLongPress = true,
        )

        assertEquals(setOf(1L, 2L, 3L, 4L), result.state.selectedIds)
        assertEquals(0, result.state.firstSelectedIndex)
        assertEquals(3, result.state.lastSelectedIndex)
        assertEquals(listOf(true, true, true, true), result.items.map { it.selected })
    }

    @Test
    fun toggleSelectionAppliesAdditionalTargetIds() {
        val result = ListSelectionPolicy.toggleSelection(
            items = ListSelectionPolicy.selectItems(listOf(1L, 2L, 3L, 4L), emptySet()),
            state = ListSelectionState(),
            targetId = 2L,
            selected = true,
            additionalTargetIds = setOf(3L, 4L),
        )

        assertEquals(setOf(2L, 3L, 4L), result.state.selectedIds)
        assertEquals(ListSelectionState.NO_SELECTION, result.state.firstSelectedIndex)
        assertEquals(ListSelectionState.NO_SELECTION, result.state.lastSelectedIndex)
        assertEquals(listOf(false, true, true, true), result.items.map { it.selected })
    }

    @Test
    fun deselectingAnchorAppliesAdditionalTargetIdsBeforeRecomputingAnchor() {
        val result = ListSelectionPolicy.toggleSelection(
            items = ListSelectionPolicy.selectItems(
                itemIds = listOf(1L, 2L, 3L, 4L),
                selectedIds = setOf(1L, 2L, 3L, 4L),
            ),
            state = ListSelectionState(
                selectedIds = setOf(1L, 2L, 3L, 4L),
                firstSelectedIndex = 0,
                lastSelectedIndex = 3,
            ),
            targetId = 1L,
            selected = false,
            userSelected = true,
            additionalTargetIds = setOf(2L),
        )

        assertEquals(setOf(3L, 4L), result.state.selectedIds)
        assertEquals(2, result.state.firstSelectedIndex)
        assertEquals(3, result.state.lastSelectedIndex)
        assertEquals(listOf(false, false, true, true), result.items.map { it.selected })
    }

    @Test
    fun deselectingRangeAnchorRecomputesAnchorFromRemainingSelection() {
        val result = ListSelectionPolicy.toggleSelection(
            items = ListSelectionPolicy.selectItems(
                itemIds = listOf(1L, 2L, 3L),
                selectedIds = setOf(1L, 2L, 3L),
            ),
            state = ListSelectionState(
                selectedIds = setOf(1L, 2L, 3L),
                firstSelectedIndex = 0,
                lastSelectedIndex = 2,
            ),
            targetId = 1L,
            selected = false,
            userSelected = true,
        )

        assertEquals(setOf(2L, 3L), result.state.selectedIds)
        assertEquals(1, result.state.firstSelectedIndex)
        assertEquals(2, result.state.lastSelectedIndex)
        assertEquals(listOf(false, true, true), result.items.map { it.selected })
    }

    @Test
    fun toggleAllSelectionUpdatesCurrentItemsAndKeepsUnseenSelections() {
        val result = ListSelectionPolicy.toggleAllSelection(
            items = ListSelectionPolicy.selectItems(listOf(1L, 2L), emptySet()),
            state = ListSelectionState(
                selectedIds = setOf(9L),
                firstSelectedIndex = 0,
                lastSelectedIndex = 0,
            ),
            selected = true,
        )

        assertEquals(setOf(1L, 2L, 9L), result.state.selectedIds)
        assertEquals(ListSelectionState.NO_SELECTION, result.state.firstSelectedIndex)
        assertEquals(ListSelectionState.NO_SELECTION, result.state.lastSelectedIndex)
        assertEquals(listOf(true, true), result.items.map { it.selected })
    }

    @Test
    fun invertSelectionTogglesCurrentItemsAndKeepsUnseenSelections() {
        val result = ListSelectionPolicy.invertSelection(
            items = ListSelectionPolicy.selectItems(
                itemIds = listOf(1L, 2L, 3L),
                selectedIds = setOf(1L, 3L),
            ),
            state = ListSelectionState(
                selectedIds = setOf(1L, 3L, 9L),
                firstSelectedIndex = 0,
                lastSelectedIndex = 2,
            ),
        )

        assertEquals(setOf(2L, 9L), result.state.selectedIds)
        assertEquals(ListSelectionState.NO_SELECTION, result.state.firstSelectedIndex)
        assertEquals(ListSelectionState.NO_SELECTION, result.state.lastSelectedIndex)
        assertEquals(listOf(false, true, false), result.items.map { it.selected })
    }

    @Test
    fun missingTargetKeepsItemsAndStateUnchanged() {
        val items = ListSelectionPolicy.selectItems(
            itemIds = listOf(1L, 2L),
            selectedIds = setOf(2L),
        )
        val state = ListSelectionState(
            selectedIds = setOf(2L),
            firstSelectedIndex = 1,
            lastSelectedIndex = 1,
        )

        val result = ListSelectionPolicy.toggleSelection(
            items = items,
            state = state,
            targetId = 3L,
            selected = true,
            userSelected = true,
        )

        assertEquals(items, result.items)
        assertEquals(state, result.state)
    }
}
