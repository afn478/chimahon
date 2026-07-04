package tachiyomi.domain.source.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceCategoryPolicyTest {
    @Test
    fun canCreateCategoryRejectsLegacyPreferenceSeparator() {
        assertTrue(SourceCategoryPolicy.canCreateCategory("Favorites"))
        assertFalse(SourceCategoryPolicy.canCreateCategory("Favorites|Pinned"))
    }

    @Test
    fun addAndDeleteCategoryUpdateCategorySet() {
        val added = SourceCategoryPolicy.addCategory(setOf("Favorites"), "Long reads")

        assertEquals(setOf("Favorites", "Long reads"), added)
        assertEquals(setOf("Long reads"), SourceCategoryPolicy.deleteCategory(added, "Favorites"))
    }

    @Test
    fun deleteSourceCategoryPreferencesRemovesEntriesForDeletedCategory() {
        val result = SourceCategoryPolicy.deleteSourceCategoryPreferences(
            sourceCategoryPreferences = setOf(
                "1|Favorites",
                "2|Favorites",
                "3|Long reads",
                "Favorites",
            ),
            category = "Favorites",
        )

        assertEquals(setOf("3|Long reads"), result)
    }

    @Test
    fun renameCategoryUpdatesCategorySetAndSourceCategoryPreferences() {
        val renamedCategories = SourceCategoryPolicy.renameCategory(
            categories = setOf("Favorites", "Long reads"),
            oldCategory = "Favorites",
            newCategory = "Pinned",
        )
        val renamedPreferences = SourceCategoryPolicy.renameSourceCategoryPreferences(
            sourceCategoryPreferences = setOf("1|Favorites", "2|Long reads", "malformed"),
            oldCategory = "Favorites",
            newCategory = "Pinned",
        )

        assertEquals(setOf("Long reads", "Pinned"), renamedCategories)
        assertEquals(setOf("1|Pinned", "2|Long reads", "malformed"), renamedPreferences)
    }

    @Test
    fun setCategoriesForSourceReplacesOnlySelectedSourceEntries() {
        val result = SourceCategoryPolicy.setCategoriesForSource(
            sourceCategoryPreferences = setOf("1|Favorites", "2|Long reads", "1|Old"),
            sourceId = 1,
            categories = listOf("Pinned", "Archive"),
        )

        assertEquals(setOf("2|Long reads", "1|Pinned", "1|Archive"), result)
    }

    @Test
    fun parseSourceCategoryPreferencesKeepsLegacyPipeEncoding() {
        val result = SourceCategoryPolicy.parseSourceCategoryPreferences(
            setOf("10|Favorites|ignored", "11|Long reads"),
        ).sortedBy(SourceCategoryPreference::sourceId)

        assertEquals(
            listOf(
                SourceCategoryPreference(sourceId = 10, category = "Favorites"),
                SourceCategoryPreference(sourceId = 11, category = "Long reads"),
            ),
            result,
        )
    }

    @Test
    fun sortCategoriesUsesCaseInsensitiveOrderByDefault() {
        assertEquals(
            listOf("alpha", "Beta", "zeta"),
            SourceCategoryPolicy.sortCategories(setOf("zeta", "Beta", "alpha")),
        )
    }
}
