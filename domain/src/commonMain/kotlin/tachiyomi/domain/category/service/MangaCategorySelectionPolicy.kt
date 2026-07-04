package tachiyomi.domain.category.service

import tachiyomi.domain.category.model.Category

data class CategorySelection(
    val category: Category,
    val state: CategorySelectionState,
)

enum class CategorySelectionState {
    CHECKED,
    MIXED,
    NONE,
}

object MangaCategorySelectionPolicy {
    fun checkedSelection(
        categories: List<Category>,
        selectedCategoryIds: Collection<Long>,
    ): List<CategorySelection> {
        val selectedCategoryIdsSet = selectedCategoryIds.toSet()

        return categories.map { category ->
            CategorySelection(
                category = category,
                state = if (category.id in selectedCategoryIdsSet) {
                    CategorySelectionState.CHECKED
                } else {
                    CategorySelectionState.NONE
                },
            )
        }
    }

    fun initialSelection(
        categories: List<Category>,
        mangaCategories: List<Set<Category>>,
    ): List<CategorySelection> {
        val commonCategories = commonCategories(mangaCategories)
        val mixedCategories = mixedCategories(mangaCategories)

        return categories.map { category ->
            CategorySelection(
                category = category,
                state = when (category) {
                    in commonCategories -> CategorySelectionState.CHECKED
                    in mixedCategories -> CategorySelectionState.MIXED
                    else -> CategorySelectionState.NONE
                },
            )
        }
    }

    fun commonCategories(mangaCategories: List<Set<Category>>): Set<Category> {
        if (mangaCategories.isEmpty()) {
            return emptySet()
        }

        return mangaCategories.reduce { common, categories ->
            common.intersect(categories)
        }
    }

    fun mixedCategories(mangaCategories: List<Set<Category>>): Set<Category> {
        if (mangaCategories.isEmpty()) {
            return emptySet()
        }

        val commonCategories = commonCategories(mangaCategories)
        return mangaCategories
            .flatten()
            .distinct()
            .subtract(commonCategories)
    }

    fun <T> updatedCategoryIds(
        currentCategoryIds: Collection<T>,
        addCategoryIds: Collection<T>,
        removeCategoryIds: Collection<T>,
    ): List<T> {
        return currentCategoryIds
            .subtract(removeCategoryIds.toSet())
            .plus(addCategoryIds)
            .toList()
    }
}
