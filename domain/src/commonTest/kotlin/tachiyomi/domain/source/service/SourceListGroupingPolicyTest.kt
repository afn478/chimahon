package tachiyomi.domain.source.service

import tachiyomi.domain.source.model.Pins
import tachiyomi.domain.source.model.Source
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceListGroupingPolicyTest {
    @Test
    fun groupSourcesOrdersPinnedLastUsedCategoriesLanguagesAndBlankLanguages() {
        val result = SourceListGroupingPolicy.groupSources(
            listOf(
                source(id = 1, lang = "ja"),
                source(id = 2, lang = ""),
                source(id = 3, lang = "en", pin = Pins.pinned),
                source(id = 4, lang = "en", category = "Favorites"),
                source(id = 5, lang = "en", isUsedLast = true),
            ),
        )

        assertEquals(
            listOf(
                SourceListGroupingPolicy.LAST_USED_KEY,
                SourceListGroupingPolicy.PINNED_KEY,
                "${SourceListGroupingPolicy.CATEGORY_KEY_PREFIX}Favorites",
                "ja",
                "",
            ),
            result.map(SourceListGroup::key),
        )
    }

    @Test
    fun groupSourcesUsesCategoryHeadersWithoutInternalPrefix() {
        val result = SourceListGroupingPolicy.groupSources(
            listOf(source(id = 1, lang = "en", category = "Long reads")),
        )

        assertEquals("Long reads", result.single().header)
        assertTrue(result.single().isCategory)
    }

    @Test
    fun groupSourcesPreservesSourceOrderWithinGroups() {
        val result = SourceListGroupingPolicy.groupSources(
            listOf(
                source(id = 2, lang = "en"),
                source(id = 1, lang = "en"),
            ),
        )

        assertEquals(listOf(2L, 1L), result.single().sources.map(Source::id))
        assertFalse(result.single().isCategory)
    }

    private fun source(
        id: Long,
        lang: String,
        pin: Pins = Pins.unpinned,
        isUsedLast: Boolean = false,
        category: String? = null,
    ): Source {
        return Source(
            id = id,
            lang = lang,
            name = "Source $id",
            supportsLatest = true,
            isStub = false,
            pin = pin,
            isUsedLast = isUsedLast,
            category = category,
        )
    }
}
