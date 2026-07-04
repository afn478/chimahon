package tachiyomi.domain.library.service

import kotlin.test.Test
import kotlin.test.assertEquals

class NovelCategoryPolicyTest {
    @Test
    fun ensureDefaultCategoryAddsDefaultWhenMissing() {
        val result = NovelCategoryPolicy.ensureDefaultCategory(
            categories = listOf(category("sci-fi")),
            defaultCategory = category("default", system = true),
            categoryId = TestCategory::id,
            uncategorizedCategoryId = "default",
        )

        assertEquals(listOf("default", "sci-fi"), result.map(TestCategory::id))
    }

    @Test
    fun ensureDefaultCategoryKeepsExistingDefault() {
        val categories = listOf(category("default", system = true), category("sci-fi"))
        val result = NovelCategoryPolicy.ensureDefaultCategory(
            categories = categories,
            defaultCategory = category("default", system = true),
            categoryId = TestCategory::id,
            uncategorizedCategoryId = "default",
        )

        assertEquals(categories, result)
    }

    @Test
    fun sortCategoriesPlacesSystemFirstThenOrdersByIndexAndName() {
        val result = NovelCategoryPolicy.sortCategories(
            categories = listOf(
                category("b", name = "Beta", order = 1),
                category("default", name = "Default", order = 99, system = true),
                category("a", name = "Alpha", order = 1),
                category("z", name = "Zeta", order = 0),
            ),
            isSystemCategory = TestCategory::system,
            categoryOrder = TestCategory::order,
            categoryName = TestCategory::name,
        )

        assertEquals(listOf("default", "z", "a", "b"), result.map(TestCategory::id))
    }

    @Test
    fun nextUserCategoryOrderIgnoresSystemCategories() {
        val result = NovelCategoryPolicy.nextUserCategoryOrder(
            categories = listOf(
                category("default", order = 99, system = true),
                category("a", order = 0),
                category("b", order = 3),
            ),
            isSystemCategory = TestCategory::system,
            categoryOrder = TestCategory::order,
        )

        assertEquals(4, result)
    }

    @Test
    fun categoryNameKeyTrimsAndIgnoresCase() {
        val result = NovelCategoryPolicy.categoryNameKey("  Sci-Fi  ")

        assertEquals("sci-fi", result)
    }

    @Test
    fun mergeCategoriesByIdentityKeepsHigherOrderCategoryForSameIdOrName() {
        val result = NovelCategoryPolicy.mergeCategoriesByIdentity(
            categories = listOf(
                category("a", name = "Sci-Fi", order = 1),
                category("b", name = "Classics", order = 0),
                category("a", name = "Science Fiction", order = 4),
                category("c", name = " classics ", order = 3),
            ),
            categoryId = TestCategory::id,
            categoryName = TestCategory::name,
            categoryOrder = { it.order.toLong() },
        )

        assertEquals(listOf("a", "c"), result.map(TestCategory::id))
        assertEquals(listOf("Science Fiction", " classics "), result.map(TestCategory::name))
    }

    @Test
    fun mergeCategoriesByIdentityKeepsExistingCategoryWhenOrderTies() {
        val result = NovelCategoryPolicy.mergeCategoriesByIdentity(
            categories = listOf(
                category("a", name = "Sci-Fi", order = 1),
                category("b", name = " sci-fi ", order = 1),
            ),
            categoryId = TestCategory::id,
            categoryName = TestCategory::name,
            categoryOrder = { it.order.toLong() },
        )

        assertEquals(listOf("a"), result.map(TestCategory::id))
    }

    @Test
    fun restoreCategoriesAddsMissingCategoriesAndMapsTheirIds() {
        val result = NovelCategoryPolicy.restoreCategories(
            currentCategories = listOf(category("default", system = true)),
            backupCategories = listOf(category("backup", name = "Sci-Fi", order = 2, flags = 8)),
            uncategorizedCategoryId = "default",
            currentCategoryId = TestCategory::id,
            currentCategoryName = TestCategory::name,
            backupCategoryId = TestCategory::id,
            backupCategoryName = TestCategory::name,
            createCategory = { it },
            updateCategory = { current, backup ->
                current.copy(name = backup.name, order = backup.order, flags = backup.flags)
            },
        )

        assertEquals(true, result.changed)
        assertEquals(listOf("default", "backup"), result.categories.map(TestCategory::id))
        assertEquals(
            mapOf("default" to "default", "backup" to "backup"),
            result.categoryIdMap,
        )
    }

    @Test
    fun restoreCategoriesUpdatesExistingCategoryByNameAndMapsBackupIdToCurrentId() {
        val result = NovelCategoryPolicy.restoreCategories(
            currentCategories = listOf(category("local", name = "Sci-Fi", order = 0, flags = 1)),
            backupCategories = listOf(category("backup", name = " sci-fi ", order = 5, flags = 8)),
            uncategorizedCategoryId = "default",
            currentCategoryId = TestCategory::id,
            currentCategoryName = TestCategory::name,
            backupCategoryId = TestCategory::id,
            backupCategoryName = TestCategory::name,
            createCategory = { it },
            updateCategory = { current, backup ->
                current.copy(name = backup.name, order = backup.order, flags = backup.flags)
            },
        )

        assertEquals(true, result.changed)
        assertEquals(listOf("local"), result.categories.map(TestCategory::id))
        assertEquals(listOf(" sci-fi "), result.categories.map(TestCategory::name))
        assertEquals(mapOf("default" to "default", "backup" to "local"), result.categoryIdMap)
    }

    @Test
    fun restoreCategoriesReportsUnchangedWhenBackupMatchesCurrent() {
        val categories = listOf(category("sci-fi", name = "Sci-Fi", order = 2, flags = 8))
        val result = NovelCategoryPolicy.restoreCategories(
            currentCategories = categories,
            backupCategories = listOf(category("sci-fi", name = "Sci-Fi", order = 2, flags = 8)),
            uncategorizedCategoryId = "default",
            currentCategoryId = TestCategory::id,
            currentCategoryName = TestCategory::name,
            backupCategoryId = TestCategory::id,
            backupCategoryName = TestCategory::name,
            createCategory = { it },
            updateCategory = { current, backup ->
                current.copy(name = backup.name, order = backup.order, flags = backup.flags)
            },
        )

        assertEquals(false, result.changed)
        assertEquals(categories, result.categories)
        assertEquals(mapOf("default" to "default", "sci-fi" to "sci-fi"), result.categoryIdMap)
    }

    @Test
    fun restoreCategoriesReturnsDefaultMappingWhenBackupIsEmpty() {
        val categories = listOf(category("sci-fi", name = "Sci-Fi", order = 2, flags = 8))
        val result = NovelCategoryPolicy.restoreCategories(
            currentCategories = categories,
            backupCategories = emptyList<TestCategory>(),
            uncategorizedCategoryId = "default",
            currentCategoryId = TestCategory::id,
            currentCategoryName = TestCategory::name,
            backupCategoryId = TestCategory::id,
            backupCategoryName = TestCategory::name,
            createCategory = { it },
            updateCategory = { current, backup ->
                current.copy(name = backup.name, order = backup.order, flags = backup.flags)
            },
        )

        assertEquals(false, result.changed)
        assertEquals(categories, result.categories)
        assertEquals(mapOf("default" to "default"), result.categoryIdMap)
    }

    @Test
    fun resolveImportedCategoryIdsMergesExistingAndRequestedCategories() {
        val result = NovelCategoryPolicy.resolveImportedCategoryIds(
            existingCategoryIds = listOf("default", "sci-fi"),
            requestedCategoryIds = listOf("default", "classics", "sci-fi"),
            uncategorizedCategoryId = "default",
        )

        assertEquals(listOf("sci-fi", "classics"), result)
    }

    @Test
    fun resolveImportedCategoryIdsKeepsExistingCategoriesWhenNoCategoryIsRequested() {
        val result = NovelCategoryPolicy.resolveImportedCategoryIds(
            existingCategoryIds = listOf("sci-fi"),
            requestedCategoryIds = null,
            uncategorizedCategoryId = "default",
        )

        assertEquals(listOf("sci-fi"), result)
    }

    @Test
    fun resolveImportedCategoryIdsCanReturnEmptyWhenNothingIsAssigned() {
        val result = NovelCategoryPolicy.resolveImportedCategoryIds(
            existingCategoryIds = emptyList(),
            requestedCategoryIds = null,
            uncategorizedCategoryId = "default",
        )

        assertEquals(emptyList(), result)
    }

    @Test
    fun remapCategoryIdsToMergedCategoriesUsesSourceNameBeforeDirectMergedId() {
        val result = NovelCategoryPolicy.remapCategoryIdsToMergedCategories(
            categoryIds = listOf("default", "old-sci-fi", "merged-direct", "missing"),
            sourceCategories = listOf(
                category("old-sci-fi", name = "Sci-Fi"),
                category("old-direct", name = "Different"),
            ),
            mergedCategories = listOf(
                category("new-sci-fi", name = " sci-fi "),
                category("merged-direct", name = "Direct"),
            ),
            uncategorizedCategoryId = "default",
            sourceCategoryId = TestCategory::id,
            sourceCategoryName = TestCategory::name,
            mergedCategoryId = TestCategory::id,
            mergedCategoryName = TestCategory::name,
        )

        assertEquals(listOf("new-sci-fi", "merged-direct"), result)
    }

    @Test
    fun remapCategoryIdsToMergedCategoriesKeepsDefaultWhenNoUserCategoriesMap() {
        val result = NovelCategoryPolicy.remapCategoryIdsToMergedCategories(
            categoryIds = listOf("default", "missing"),
            sourceCategories = emptyList<TestCategory>(),
            mergedCategories = emptyList<TestCategory>(),
            uncategorizedCategoryId = "default",
            sourceCategoryId = TestCategory::id,
            sourceCategoryName = TestCategory::name,
            mergedCategoryId = TestCategory::id,
            mergedCategoryName = TestCategory::name,
        )

        assertEquals(listOf("default"), result)
    }

    @Test
    fun reorderUserCategoriesKeepsSystemCategoriesAndReassignsUserOrder() {
        val result = NovelCategoryPolicy.reorderUserCategories(
            categories = listOf(
                category("default", order = 99, system = true),
                category("a", order = 0),
                category("b", order = 1),
                category("c", order = 2),
            ),
            targetCategoryId = "c",
            newIndex = 0,
            categoryId = TestCategory::id,
            isSystemCategory = TestCategory::system,
            withOrder = { category, order -> category.copy(order = order) },
        )

        assertEquals(listOf("default", "c", "a", "b"), result.map(TestCategory::id))
        assertEquals(listOf(-1, 0, 1, 2), result.map(TestCategory::order))
    }

    @Test
    fun reorderUserCategoriesKeepsOriginalListWhenTargetIsMissing() {
        val categories = listOf(category("a", order = 0), category("b", order = 1))
        val result = NovelCategoryPolicy.reorderUserCategories(
            categories = categories,
            targetCategoryId = "missing",
            newIndex = 0,
            categoryId = TestCategory::id,
            isSystemCategory = TestCategory::system,
            withOrder = { category, order -> category.copy(order = order) },
        )

        assertEquals(categories, result)
    }

    @Test
    fun normalizeCategoryIdsDropsBlankDuplicateAndDefaultWhenUserCategoryExists() {
        val result = NovelCategoryPolicy.normalizeCategoryIds(
            categoryIds = listOf("", "default", "sci-fi", "sci-fi", "classics"),
            uncategorizedCategoryId = "default",
        )

        assertEquals(listOf("sci-fi", "classics"), result)
    }

    @Test
    fun normalizeCategoryIdsKeepsDefaultWhenNoUserCategoryExists() {
        val result = NovelCategoryPolicy.normalizeCategoryIds(
            categoryIds = listOf("", "default", "default"),
            uncategorizedCategoryId = "default",
        )

        assertEquals(listOf("default"), result)
    }

    @Test
    fun normalizeCategoryIdsCanReturnEmptyWhenNoCategoryIsAssigned() {
        val result = NovelCategoryPolicy.normalizeCategoryIds(
            categoryIds = emptyList(),
            uncategorizedCategoryId = "default",
        )

        assertEquals(emptyList(), result)
    }

    @Test
    fun normalizeCategoryIdsOrDefaultFallsBackToUncategorized() {
        val result = NovelCategoryPolicy.normalizeCategoryIdsOrDefault(
            categoryIds = emptyList(),
            uncategorizedCategoryId = "default",
        )

        assertEquals(listOf("default"), result)
    }

    @Test
    fun removeCategoryIdFallsBackToUncategorizedWhenLastCategoryIsRemoved() {
        val result = NovelCategoryPolicy.removeCategoryId(
            categoryIds = listOf("sci-fi"),
            categoryId = "sci-fi",
            uncategorizedCategoryId = "default",
        )

        assertEquals(listOf("default"), result)
    }

    @Test
    fun removeCategoryIdNormalizesRemainingCategories() {
        val result = NovelCategoryPolicy.removeCategoryId(
            categoryIds = listOf("default", "sci-fi", "classics", "classics"),
            categoryId = "sci-fi",
            uncategorizedCategoryId = "default",
        )

        assertEquals(listOf("classics"), result)
    }

    private fun category(
        id: String,
        name: String = id,
        order: Int = 0,
        system: Boolean = false,
        flags: Long = 0,
    ): TestCategory {
        return TestCategory(
            id = id,
            name = name,
            order = order,
            system = system,
            flags = flags,
        )
    }

    private data class TestCategory(
        val id: String,
        val name: String,
        val order: Int,
        val system: Boolean,
        val flags: Long,
    )
}
