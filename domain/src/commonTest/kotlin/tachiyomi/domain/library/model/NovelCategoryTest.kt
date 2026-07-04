package tachiyomi.domain.library.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NovelCategoryTest {
    @Test
    fun defaultCategoryValuesUsePortableGeneratedId() {
        val category = NovelCategory(name = "Reading")

        assertTrue(category.id.matches(UUID_PATTERN))
        assertEquals("Reading", category.name)
        assertEquals(0, category.order)
        assertEquals(0L, category.flags)
        assertFalse(category.isSystemCategory)
    }

    @Test
    fun uncategorizedCategoryKeepsStableSystemIdentity() {
        val category = NovelCategory(
            id = NovelCategory.UNCATEGORIZED_ID,
            name = "Default",
            order = -1,
            flags = 3,
        )

        assertTrue(category.isSystemCategory)
        assertEquals("default", NovelCategory.UNCATEGORIZED_ID)
        assertEquals("Default", category.name)
        assertEquals(-1, category.order)
        assertEquals(3L, category.flags)
    }

    private companion object {
        val UUID_PATTERN = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    }
}
