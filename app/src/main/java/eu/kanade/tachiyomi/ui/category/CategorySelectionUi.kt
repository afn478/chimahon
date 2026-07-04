package eu.kanade.tachiyomi.ui.category

import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.service.CategorySelection
import tachiyomi.domain.category.service.CategorySelectionState

internal fun CategorySelection.toCheckboxState(): CheckboxState<Category> {
    return when (state) {
        CategorySelectionState.CHECKED -> CheckboxState.State.Checked(category)
        CategorySelectionState.MIXED -> CheckboxState.TriState.Exclude(category)
        CategorySelectionState.NONE -> CheckboxState.State.None(category)
    }
}

internal fun CategorySelection.toStateCheckboxState(): CheckboxState.State<Category> {
    return when (state) {
        CategorySelectionState.CHECKED -> CheckboxState.State.Checked(category)
        CategorySelectionState.MIXED,
        CategorySelectionState.NONE -> CheckboxState.State.None(category)
    }
}
