package tachiyomi.domain.category.service

import tachiyomi.domain.category.model.Category
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryCreationPolicyTest {
    @Test
    fun nextOrderStartsAtZeroWhenThereAreNoCategories() {
        assertEquals(0L, CategoryCreationPolicy.nextOrder(emptyList()))
    }

    @Test
    fun nextOrderUsesHighestExistingOrder() {
        val categories = listOf(
            category(id = Category.UNCATEGORIZED_ID, order = -1),
            category(id = 1, order = 2),
            category(id = 2, order = 7),
        )

        assertEquals(8L, CategoryCreationPolicy.nextOrder(categories))
    }

    @Test
    fun createBuildsCategoryWithNextOrderAndDefaults() {
        val categories = listOf(category(id = 1, order = 3))
        val category = CategoryCreationPolicy.create(
            name = "Reading",
            existingCategories = categories,
            flags = 0b01000000,
        )

        assertEquals(
            Category(
                id = 0,
                name = "Reading",
                order = 4,
                flags = 0b01000000,
                hidden = false,
            ),
            category,
        )
    }

    @Test
    fun createPreservesRequestedHiddenState() {
        val category = CategoryCreationPolicy.create(
            name = "Private",
            existingCategories = emptyList(),
            flags = 0,
            hidden = true,
        )

        assertEquals(true, category.hidden)
    }

    private fun category(
        id: Long,
        order: Long,
    ): Category {
        return Category(
            id = id,
            name = "Category $id",
            order = order,
            flags = 0,
            hidden = false,
        )
    }
}
