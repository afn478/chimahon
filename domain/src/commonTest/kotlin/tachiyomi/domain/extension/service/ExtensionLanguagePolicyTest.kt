package tachiyomi.domain.extension.service

import kotlin.test.Test
import kotlin.test.assertEquals

class ExtensionLanguagePolicyTest {
    @Test
    fun selectLanguagesUsesExtensionLanguageWhenExtensionHasNoSourceLanguages() {
        val languages = ExtensionLanguagePolicy.selectLanguages(
            extensions = listOf(
                extension(language = "en"),
                extension(language = "ja", sourceLanguages = listOf("fr")),
            ),
            enabledLanguages = setOf("en", "fr"),
            extensionLanguage = Extension::language,
            sourceLanguages = Extension::sourceLanguages,
            compareLanguages = ::compareLanguages,
        )

        assertEquals(listOf("en", "fr"), languages)
    }

    @Test
    fun selectLanguagesUsesSourceLanguagesWhenPresent() {
        val languages = ExtensionLanguagePolicy.selectLanguages(
            extensions = listOf(
                extension(language = "ja", sourceLanguages = listOf("en", "fr")),
            ),
            enabledLanguages = setOf("en", "fr", "ja"),
            extensionLanguage = Extension::language,
            sourceLanguages = Extension::sourceLanguages,
            compareLanguages = ::compareLanguages,
        )

        assertEquals(listOf("en", "fr"), languages)
    }

    @Test
    fun selectLanguagesDeduplicatesLanguagesAndSortsEnabledLanguagesFirst() {
        val languages = ExtensionLanguagePolicy.selectLanguages(
            extensions = listOf(
                extension(language = "ja", sourceLanguages = listOf("ja", "en")),
                extension(language = "fr", sourceLanguages = listOf("en", "all")),
                extension(language = "es"),
            ),
            enabledLanguages = setOf("all", "ja"),
            extensionLanguage = Extension::language,
            sourceLanguages = Extension::sourceLanguages,
            compareLanguages = ::compareLanguages,
        )

        assertEquals(listOf("all", "ja", "en", "es"), languages)
    }

    private data class Extension(
        val language: String,
        val sourceLanguages: List<String>,
    )

    private fun extension(
        language: String,
        sourceLanguages: List<String> = emptyList(),
    ): Extension {
        return Extension(
            language = language,
            sourceLanguages = sourceLanguages,
        )
    }

    private companion object {
        private val LANGUAGE_ORDER = listOf("all", "en", "fr", "ja", "es")

        fun compareLanguages(left: String, right: String): Int {
            return LANGUAGE_ORDER.indexOf(left).compareTo(LANGUAGE_ORDER.indexOf(right))
        }
    }
}
