package tachiyomi.domain.source.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GlobalSearchSourceSelectionPolicyTest {
    @Test
    fun selectEnabledSourcesFiltersDisabledSourcesAndLanguages() {
        val result = GlobalSearchSourceSelectionPolicy.selectEnabledSources(
            sources = listOf(
                source(id = 1, lang = "en", name = "Alpha"),
                source(id = 2, lang = "ja", name = "Beta"),
                source(id = 3, lang = "en", name = "Gamma"),
            ),
            enabledLanguages = setOf("en"),
            disabledSourceIds = setOf("3"),
            pinnedSourceIds = emptySet(),
            sourceId = TestSource::id,
            sourceLanguage = TestSource::lang,
            sourceName = TestSource::name,
        )

        assertEquals(listOf(1L), result.map(TestSource::id))
    }

    @Test
    fun selectEnabledSourcesSortsPinnedSourcesFirstThenByNameAndLanguage() {
        val result = GlobalSearchSourceSelectionPolicy.selectEnabledSources(
            sources = listOf(
                source(id = 1, lang = "en", name = "zeta"),
                source(id = 2, lang = "ja", name = "Beta"),
                source(id = 3, lang = "en", name = "alpha"),
                source(id = 4, lang = "de", name = "Alpha"),
            ),
            enabledLanguages = setOf("de", "en", "ja"),
            disabledSourceIds = emptySet(),
            pinnedSourceIds = setOf("2", "1"),
            sourceId = TestSource::id,
            sourceLanguage = TestSource::lang,
            sourceName = TestSource::name,
        )

        assertEquals(listOf(2L, 1L, 4L, 3L), result.map(TestSource::id))
    }

    @Test
    fun selectPinnedSourcesKeepsOnlyPinnedSourcesInExistingOrder() {
        val sources = listOf(
            source(id = 1, lang = "en", name = "Alpha"),
            source(id = 2, lang = "en", name = "Beta"),
            source(id = 3, lang = "en", name = "Gamma"),
        )

        val result = GlobalSearchSourceSelectionPolicy.selectPinnedSources(
            sources = sources,
            pinnedSourceIds = setOf("3", "1"),
            sourceId = TestSource::id,
        )

        assertEquals(listOf(1L, 3L), result.map(TestSource::id))
    }

    @Test
    fun hasPinnedSourcesReportsWhetherAnySourceIsPinned() {
        val sources = listOf(
            source(id = 1, lang = "en", name = "Alpha"),
            source(id = 2, lang = "en", name = "Beta"),
        )

        assertTrue(
            GlobalSearchSourceSelectionPolicy.hasPinnedSources(
                sources = sources,
                pinnedSourceIds = setOf("2"),
                sourceId = TestSource::id,
            ),
        )
        assertFalse(
            GlobalSearchSourceSelectionPolicy.hasPinnedSources(
                sources = sources,
                pinnedSourceIds = setOf("3"),
                sourceId = TestSource::id,
            ),
        )
    }

    private fun source(
        id: Long,
        lang: String,
        name: String,
    ): TestSource {
        return TestSource(
            id = id,
            lang = lang,
            name = name,
        )
    }

    private data class TestSource(
        val id: Long,
        val lang: String,
        val name: String,
    )
}
