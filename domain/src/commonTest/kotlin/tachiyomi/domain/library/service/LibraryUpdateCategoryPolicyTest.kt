package tachiyomi.domain.library.service

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryUpdateCategoryPolicyTest {
    @Test
    fun shouldIncludeAllowsEverythingWhenNoCategoryPreferencesAreSet() {
        assertTrue(
            LibraryUpdateCategoryPolicy.shouldInclude(
                categoryIds = listOf(1),
                includedCategoryIds = emptySet(),
                excludedCategoryIds = emptySet(),
            ),
        )
    }

    @Test
    fun shouldIncludeRejectsExcludedCategories() {
        assertFalse(
            LibraryUpdateCategoryPolicy.shouldInclude(
                categoryIds = listOf(1, 2),
                includedCategoryIds = emptySet(),
                excludedCategoryIds = setOf(2),
            ),
        )
    }

    @Test
    fun shouldIncludeRequiresIncludedCategoryWhenIncludeListIsSet() {
        assertFalse(
            LibraryUpdateCategoryPolicy.shouldInclude(
                categoryIds = listOf(1),
                includedCategoryIds = setOf(2),
                excludedCategoryIds = emptySet(),
            ),
        )
        assertTrue(
            LibraryUpdateCategoryPolicy.shouldInclude(
                categoryIds = listOf(1, 2),
                includedCategoryIds = setOf(2),
                excludedCategoryIds = emptySet(),
            ),
        )
    }
}
