package tachiyomi.domain.library.service

object NovelCategoryPolicy {
    data class RestoreCategoriesResult<T>(
        val categories: List<T>,
        val categoryIdMap: Map<String, String>,
        val changed: Boolean,
    )

    fun <T> ensureDefaultCategory(
        categories: List<T>,
        defaultCategory: T,
        categoryId: (T) -> String,
        uncategorizedCategoryId: String,
    ): List<T> {
        return if (categories.none { categoryId(it) == uncategorizedCategoryId }) {
            listOf(defaultCategory) + categories
        } else {
            categories
        }
    }

    fun <T> sortCategories(
        categories: List<T>,
        isSystemCategory: (T) -> Boolean,
        categoryOrder: (T) -> Int,
        categoryName: (T) -> String,
    ): List<T> {
        return categories.sortedWith(
            compareBy<T> { if (isSystemCategory(it)) Int.MIN_VALUE else categoryOrder(it) }
                .thenBy(categoryName),
        )
    }

    fun <T> nextUserCategoryOrder(
        categories: List<T>,
        isSystemCategory: (T) -> Boolean,
        categoryOrder: (T) -> Int,
    ): Int {
        return categories
            .filterNot(isSystemCategory)
            .maxOfOrNull(categoryOrder)
            ?.plus(1)
            ?: 0
    }

    fun categoryNameKey(name: String): String {
        return name.trim().lowercase()
    }

    fun <T> mergeCategoriesByIdentity(
        categories: List<T>,
        categoryId: (T) -> String,
        categoryName: (T) -> String,
        categoryOrder: (T) -> Long,
    ): List<T> {
        val mergedCategories = mutableListOf<T>()

        categories.forEach { category ->
            val existingIndex = mergedCategories.indexOfFirst {
                categoriesMatch(
                    firstId = categoryId(it),
                    firstName = categoryName(it),
                    secondId = categoryId(category),
                    secondName = categoryName(category),
                )
            }

            if (existingIndex == -1) {
                mergedCategories.add(category)
            } else if (categoryOrder(category) > categoryOrder(mergedCategories[existingIndex])) {
                mergedCategories[existingIndex] = category
            }
        }

        return mergedCategories
    }

    fun <Current, Backup> restoreCategories(
        currentCategories: List<Current>,
        backupCategories: List<Backup>,
        uncategorizedCategoryId: String,
        currentCategoryId: (Current) -> String,
        currentCategoryName: (Current) -> String,
        backupCategoryId: (Backup) -> String,
        backupCategoryName: (Backup) -> String,
        createCategory: (Backup) -> Current,
        updateCategory: (Current, Backup) -> Current,
    ): RestoreCategoriesResult<Current> {
        val restoredCategories = currentCategories.toMutableList()
        val categoryIdMap = mutableMapOf(
            uncategorizedCategoryId to uncategorizedCategoryId,
        )
        var changed = false

        backupCategories.forEach { backupCategory ->
            val backupId = backupCategoryId(backupCategory)
            val backupName = backupCategoryName(backupCategory)
            val existingIndex = restoredCategories.indexOfFirst {
                categoriesMatch(
                    firstId = currentCategoryId(it),
                    firstName = currentCategoryName(it),
                    secondId = backupId,
                    secondName = backupName,
                )
            }

            if (existingIndex == -1) {
                val restoredCategory = createCategory(backupCategory)
                restoredCategories.add(restoredCategory)
                categoryIdMap[backupId] = currentCategoryId(restoredCategory)
                changed = true
            } else {
                val existing = restoredCategories[existingIndex]
                categoryIdMap[backupId] = currentCategoryId(existing)

                val updatedCategory = updateCategory(existing, backupCategory)
                if (updatedCategory != existing) {
                    restoredCategories[existingIndex] = updatedCategory
                    changed = true
                }
            }
        }

        return RestoreCategoriesResult(
            categories = restoredCategories,
            categoryIdMap = categoryIdMap,
            changed = changed,
        )
    }

    fun resolveImportedCategoryIds(
        existingCategoryIds: Collection<String>,
        requestedCategoryIds: Collection<String>?,
        uncategorizedCategoryId: String,
    ): List<String> {
        val categoryIds = when {
            requestedCategoryIds != null -> existingCategoryIds + requestedCategoryIds
            existingCategoryIds.isNotEmpty() -> existingCategoryIds
            else -> emptyList()
        }

        return normalizeCategoryIds(
            categoryIds = categoryIds,
            uncategorizedCategoryId = uncategorizedCategoryId,
        )
    }

    fun <Source, Merged> remapCategoryIdsToMergedCategories(
        categoryIds: Collection<String>,
        sourceCategories: Collection<Source>,
        mergedCategories: Collection<Merged>,
        uncategorizedCategoryId: String,
        sourceCategoryId: (Source) -> String,
        sourceCategoryName: (Source) -> String,
        mergedCategoryId: (Merged) -> String,
        mergedCategoryName: (Merged) -> String,
    ): List<String> {
        val sourceCategoriesById = sourceCategories.associateBy(sourceCategoryId)
        val mergedCategoriesById = mergedCategories.associateBy(mergedCategoryId)
        val mergedCategoriesByName = mergedCategories.associateBy {
            categoryNameKey(mergedCategoryName(it))
        }
        val remappedIds = categoryIds.mapNotNull { categoryId ->
            when (categoryId) {
                uncategorizedCategoryId -> uncategorizedCategoryId
                else -> {
                    val sourceCategory = sourceCategoriesById[categoryId]
                    val mergedCategoryByName = sourceCategory?.let {
                        mergedCategoriesByName[categoryNameKey(sourceCategoryName(it))]
                    }

                    mergedCategoryByName?.let(mergedCategoryId)
                        ?: mergedCategoriesById[categoryId]?.let(mergedCategoryId)
                }
            }
        }

        return normalizeCategoryIds(
            categoryIds = remappedIds,
            uncategorizedCategoryId = uncategorizedCategoryId,
        )
    }

    fun <T> reorderUserCategories(
        categories: List<T>,
        targetCategoryId: String,
        newIndex: Int,
        categoryId: (T) -> String,
        isSystemCategory: (T) -> Boolean,
        withOrder: (T, Int) -> T,
    ): List<T> {
        val systemCategories = categories
            .filter(isSystemCategory)
            .map { withOrder(it, SYSTEM_CATEGORY_ORDER) }
        val userCategories = categories
            .filterNot(isSystemCategory)
            .toMutableList()
        val oldIndex = userCategories.indexOfFirst { categoryId(it) == targetCategoryId }

        if (oldIndex < 0) {
            return categories
        }

        val item = userCategories.removeAt(oldIndex)
        userCategories.add(newIndex.coerceIn(0, userCategories.size), item)
        val reorderedUserCategories = userCategories.mapIndexed { index, category ->
            withOrder(category, index)
        }

        return systemCategories + reorderedUserCategories
    }

    fun normalizeCategoryIds(
        categoryIds: Collection<String>,
        uncategorizedCategoryId: String,
    ): List<String> {
        val distinctIds = categoryIds
            .filter { it.isNotBlank() }
            .distinct()

        return if (distinctIds.any { it != uncategorizedCategoryId }) {
            distinctIds.filterNot { it == uncategorizedCategoryId }
        } else {
            distinctIds
        }
    }

    fun normalizeCategoryIdsOrDefault(
        categoryIds: Collection<String>,
        uncategorizedCategoryId: String,
    ): List<String> {
        return normalizeCategoryIds(
            categoryIds = categoryIds,
            uncategorizedCategoryId = uncategorizedCategoryId,
        ).ifEmpty {
            listOf(uncategorizedCategoryId)
        }
    }

    fun removeCategoryId(
        categoryIds: Collection<String>,
        categoryId: String,
        uncategorizedCategoryId: String,
    ): List<String> {
        return normalizeCategoryIdsOrDefault(
            categoryIds = categoryIds.filterNot { it == categoryId },
            uncategorizedCategoryId = uncategorizedCategoryId,
        )
    }

    private fun categoriesMatch(
        firstId: String,
        firstName: String,
        secondId: String,
        secondName: String,
    ): Boolean {
        return firstId == secondId || categoryNameKey(firstName) == categoryNameKey(secondName)
    }

    const val SYSTEM_CATEGORY_ORDER = -1
}
