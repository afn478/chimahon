package tachiyomi.domain.source.service

import tachiyomi.domain.source.model.Source
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceSearchPolicyTest {
    @Test
    fun parseSearchQuerySplitsCommaSeparatedTermsAndDropsBlankValues() {
        assertEquals(
            listOf("alpha", "123", "extension"),
            SourceSearchPolicy.parseSearchQuery(" alpha, ,123,extension "),
        )
    }

    @Test
    fun parseSearchQueryHandlesNullAndBlankQueries() {
        assertEquals(emptyList(), SourceSearchPolicy.parseSearchQuery(null))
        assertEquals(emptyList(), SourceSearchPolicy.parseSearchQuery(" , "))
    }

    @Test
    fun matchesSourceReturnsTrueWhenThereAreNoSubqueries() {
        assertTrue(
            SourceSearchPolicy.matchesSource(
                source = source(name = "Source", id = 1),
                extensionName = null,
                subqueries = emptyList(),
            ),
        )
    }

    @Test
    fun matchesSourceMatchesSourceNameIgnoringCase() {
        assertTrue(
            SourceSearchPolicy.matchesSource(
                source = source(name = "Manga Source", id = 1),
                extensionName = null,
                subqueries = listOf("source"),
            ),
        )
    }

    @Test
    fun matchesSourceMatchesExtensionNameAndSourceId() {
        assertTrue(
            SourceSearchPolicy.matchesSource(
                source = source(name = "Source", id = 123),
                extensionName = "Sample Extension",
                subqueries = listOf("extension"),
            ),
        )
        assertTrue(
            SourceSearchPolicy.matchesSource(
                source = source(name = "Source", id = 123),
                extensionName = null,
                subqueries = listOf("123"),
            ),
        )
    }

    @Test
    fun matchesSourceFieldsMatchSourceNameAndId() {
        assertTrue(
            SourceSearchPolicy.matchesSource(
                sourceName = "Manga Source",
                sourceId = 10,
                extensionName = null,
                subqueries = listOf("manga"),
            ),
        )
        assertTrue(
            SourceSearchPolicy.matchesSource(
                sourceName = "Manga Source",
                sourceId = 10,
                extensionName = null,
                subqueries = listOf("10"),
            ),
        )
    }

    @Test
    fun matchesSourceRejectsUnknownSubqueries() {
        assertFalse(
            SourceSearchPolicy.matchesSource(
                source = source(name = "Source", id = 1),
                extensionName = "Extension",
                subqueries = listOf("missing"),
            ),
        )
    }

    private fun source(
        name: String,
        id: Long,
    ): Source {
        return Source(
            id = id,
            lang = "en",
            name = name,
            supportsLatest = true,
            isStub = false,
        )
    }
}
