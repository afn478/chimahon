package tachiyomi.domain.category.service

import tachiyomi.domain.category.model.Category
import kotlin.test.Test
import kotlin.test.assertEquals

class MangaCategorySelectionPolicyTest {
    @Test
    fun checkedSelectionMarksOnlySelectedCategoryIds() {
        val result = MangaCategorySelectionPolicy.checkedSelection(
            categories = listOf(category(1), category(2), category(3)),
            selectedCategoryIds = listOf(1, 3),
        )

        assertEquals(
            listOf(
                CategorySelection(category(1), CategorySelectionState.CHECKED),
                CategorySelection(category(2), CategorySelectionState.NONE),
                CategorySelection(category(3), CategorySelectionState.CHECKED),
            ),
            result,
        )
    }

    @Test
    fun commonCategoriesReturnsIntersectionAcrossMangaCategorySets() {
        val result = MangaCategorySelectionPolicy.commonCategories(
            listOf(
                setOf(category(1), category(2), category(3)),
                setOf(category(2), category(3), category(4)),
                setOf(category(3), category(4)),
            ),
        )

        assertEquals(setOf(category(3)), result)
    }

    @Test
    fun mixedCategoriesReturnsNonCommonDistinctCategories() {
        val result = MangaCategorySelectionPolicy.mixedCategories(
            listOf(
                setOf(category(1), category(2)),
                setOf(category(2), category(3)),
                setOf(category(2), category(3), category(4)),
            ),
        )

        assertEquals(setOf(category(1), category(3), category(4)), result)
    }

    @Test
    fun initialSelectionMapsCommonMixedAndUnselectedCategories() {
        val categories = listOf(category(1), category(2), category(3), category(4))
        val result = MangaCategorySelectionPolicy.initialSelection(
            categories = categories,
            mangaCategories = listOf(
                setOf(category(1), category(2)),
                setOf(category(2), category(3)),
            ),
        )

        assertEquals(
            listOf(
                CategorySelection(category(1), CategorySelectionState.MIXED),
                CategorySelection(category(2), CategorySelectionState.CHECKED),
                CategorySelection(category(3), CategorySelectionState.MIXED),
                CategorySelection(category(4), CategorySelectionState.NONE),
            ),
            result,
        )
    }

    @Test
    fun emptyMangaCategorySetsSelectNoCategories() {
        val result = MangaCategorySelectionPolicy.initialSelection(
            categories = listOf(category(1), category(2)),
            mangaCategories = emptyList(),
        )

        assertEquals(
            listOf(
                CategorySelection(category(1), CategorySelectionState.NONE),
                CategorySelection(category(2), CategorySelectionState.NONE),
            ),
            result,
        )
    }

    @Test
    fun updatedCategoryIdsRemovesThenAddsCategoryIds() {
        val result = MangaCategorySelectionPolicy.updatedCategoryIds(
            currentCategoryIds = listOf(1L, 2L, 3L),
            addCategoryIds = listOf(4L, 5L),
            removeCategoryIds = listOf(2L),
        )

        assertEquals(listOf(1L, 3L, 4L, 5L), result)
    }

    @Test
    fun updatedCategoryIdsSupportsStringCategoryIds() {
        val result = MangaCategorySelectionPolicy.updatedCategoryIds(
            currentCategoryIds = listOf("uncategorized", "sci-fi"),
            addCategoryIds = listOf("classics"),
            removeCategoryIds = listOf("sci-fi"),
        )

        assertEquals(listOf("uncategorized", "classics"), result)
    }

    private fun category(id: Long): Category {
        return Category(
            id = id,
            name = "Category $id",
            order = id,
            flags = 0,
            hidden = false,
        )
    }
}
