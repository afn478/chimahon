package tachiyomi.domain.source.service

import kotlin.test.Test
import kotlin.test.assertEquals

class FeedSourceSelectionPolicyTest {
    @Test
    fun selectEnabledSourcesFiltersDisabledSourcesAndLanguages() {
        val result = FeedSourceSelectionPolicy.selectEnabledSources(
            sources = listOf(
                source(id = 1, lang = "en", name = "Alpha"),
                source(id = 2, lang = "ja", name = "Beta"),
                source(id = 3, lang = "en", name = "Gamma"),
            ),
            enabledLanguages = setOf("en"),
            disabledSourceIds = setOf(3),
            pinnedSourceIds = emptySet(),
            sourceId = TestSource::id,
            sourceLanguage = TestSource::lang,
            sourceName = TestSource::name,
        )

        assertEquals(listOf(1L), result.map(TestSource::id))
    }

    @Test
    fun selectEnabledSourcesSortsByLanguageThenNameIgnoringCase() {
        val result = FeedSourceSelectionPolicy.selectEnabledSources(
            sources = listOf(
                source(id = 1, lang = "en", name = "zeta"),
                source(id = 2, lang = "de", name = "Beta"),
                source(id = 3, lang = "en", name = "Alpha"),
            ),
            enabledLanguages = setOf("de", "en"),
            disabledSourceIds = emptySet(),
            pinnedSourceIds = emptySet(),
            sourceId = TestSource::id,
            sourceLanguage = TestSource::lang,
            sourceName = TestSource::name,
        )

        assertEquals(listOf(2L, 3L, 1L), result.map(TestSource::id))
    }

    @Test
    fun selectEnabledSourcesPrioritizesPinnedSourcesAfterNameSorting() {
        val result = FeedSourceSelectionPolicy.selectEnabledSources(
            sources = listOf(
                source(id = 1, lang = "en", name = "Zulu"),
                source(id = 2, lang = "en", name = "Alpha"),
                source(id = 3, lang = "en", name = "Beta"),
            ),
            enabledLanguages = setOf("en"),
            disabledSourceIds = emptySet(),
            pinnedSourceIds = setOf("3", "1"),
            sourceId = TestSource::id,
            sourceLanguage = TestSource::lang,
            sourceName = TestSource::name,
        )

        assertEquals(listOf(3L, 1L, 2L), result.map(TestSource::id))
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
