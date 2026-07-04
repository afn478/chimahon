package tachiyomi.domain.extension.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtensionSearchPolicyTest {
    @Test
    fun parseSearchQuerySplitsCommaSeparatedTermsAndDropsBlankValues() {
        assertEquals(
            listOf("manga", "123", "site"),
            ExtensionSearchPolicy.parseSearchQuery(" manga, ,123,site "),
        )
    }

    @Test
    fun matchesExtensionReturnsTrueWhenThereAreNoSubqueries() {
        assertTrue(
            ExtensionSearchPolicy.matchesExtension(
                extensionName = "Extension",
                sources = emptyList(),
                subqueries = emptyList(),
            ),
        )
    }

    @Test
    fun matchesExtensionMatchesExtensionNameIgnoringCase() {
        assertTrue(
            ExtensionSearchPolicy.matchesExtension(
                extensionName = "Manga Reader",
                sources = emptyList(),
                subqueries = listOf("reader"),
            ),
        )
    }

    @Test
    fun matchesExtensionMatchesSourceNameBaseUrlAndId() {
        val sources = listOf(
            source(
                name = "Alpha Source",
                baseUrl = "https://alpha.example",
                id = 10,
            ),
            source(
                name = "Beta Source",
                baseUrl = "https://beta.example",
                id = 20,
            ),
        )

        assertTrue(
            ExtensionSearchPolicy.matchesExtension(
                extensionName = "Extension",
                sources = sources,
                subqueries = listOf("beta"),
            ),
        )
        assertTrue(
            ExtensionSearchPolicy.matchesExtension(
                extensionName = "Extension",
                sources = sources,
                subqueries = listOf("alpha.example"),
            ),
        )
        assertTrue(
            ExtensionSearchPolicy.matchesExtension(
                extensionName = "Extension",
                sources = sources,
                subqueries = listOf("20"),
            ),
        )
    }

    @Test
    fun matchesExtensionRejectsUnknownSubqueries() {
        assertFalse(
            ExtensionSearchPolicy.matchesExtension(
                extensionName = "Extension",
                sources = listOf(source(name = "Source", baseUrl = null, id = 1)),
                subqueries = listOf("missing"),
            ),
        )
    }

    private fun source(
        name: String,
        baseUrl: String?,
        id: Long,
    ): ExtensionSearchSource {
        return ExtensionSearchSource(
            name = name,
            baseUrl = baseUrl,
            id = id,
        )
    }
}
