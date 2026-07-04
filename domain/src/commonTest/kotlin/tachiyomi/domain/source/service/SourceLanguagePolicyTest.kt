package tachiyomi.domain.source.service

import tachiyomi.domain.source.model.Source
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceLanguagePolicyTest {
    @Test
    fun groupOnlineSourcesByLanguageFiltersHiddenSourcesAndSortsDisabledSourcesLast() {
        val groups = SourceLanguagePolicy.groupOnlineSourcesByLanguage(
            onlineSources = listOf(
                source(id = 2, lang = "en", name = "Alpha"),
                source(id = 1, lang = "en", name = "Beta"),
                source(id = 3, lang = "en", name = "Hidden"),
            ),
            enabledLanguages = setOf("en"),
            disabledSourceIds = setOf("2"),
            hiddenSourceIds = setOf(3),
            compareLanguages = ::compareLanguages,
        )

        assertEquals(listOf("en"), groups.map(SourceLanguageGroup::language))
        assertEquals(listOf(1L, 2L), groups.single().sources.map(Source::id))
    }

    @Test
    fun groupOnlineSourcesByLanguagePrioritizesEnabledLanguagesThenLanguageComparator() {
        val groups = SourceLanguagePolicy.groupOnlineSourcesByLanguage(
            onlineSources = listOf(
                source(id = 1, lang = "en", name = "English"),
                source(id = 2, lang = "all", name = "Multi"),
                source(id = 3, lang = "ja", name = "Japanese"),
                source(id = 4, lang = "fr", name = "French"),
            ),
            enabledLanguages = setOf("ja", "fr"),
            disabledSourceIds = emptySet(),
            hiddenSourceIds = emptySet(),
            compareLanguages = ::compareLanguages,
        )

        assertEquals(listOf("fr", "ja", "all", "en"), groups.map(SourceLanguageGroup::language))
    }

    @Test
    fun groupOnlineSourcesByLanguageUsesInjectedSourceNameComparator() {
        val groups = SourceLanguagePolicy.groupOnlineSourcesByLanguage(
            onlineSources = listOf(
                source(id = 1, lang = "en", name = "aaaa"),
                source(id = 2, lang = "en", name = "bb"),
                source(id = 3, lang = "en", name = "c"),
            ),
            enabledLanguages = setOf("en"),
            disabledSourceIds = emptySet(),
            hiddenSourceIds = emptySet(),
            compareLanguages = ::compareLanguages,
            compareSourceNames = { left, right -> left.length.compareTo(right.length) },
        )

        assertEquals(listOf(3L, 2L, 1L), groups.single().sources.map(Source::id))
    }

    private fun source(
        id: Long,
        lang: String,
        name: String,
    ): Source {
        return Source(
            id = id,
            lang = lang,
            name = name,
            supportsLatest = true,
            isStub = false,
        )
    }

    private companion object {
        private val LANGUAGE_ORDER = listOf("all", "en", "fr", "ja")

        fun compareLanguages(left: String, right: String): Int {
            return LANGUAGE_ORDER.indexOf(left).compareTo(LANGUAGE_ORDER.indexOf(right))
        }
    }
}
