package tachiyomi.domain.source.service

import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Pins
import tachiyomi.domain.source.model.Source
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceCatalogPolicyTest {
    @Test
    fun selectEnabledSourcesFiltersByLanguageDisabledHiddenAndLocalSources() {
        val result = SourceCatalogPolicy.selectEnabledSources(
            sources = listOf(
                source(id = 4, lang = "ja", name = "Zeta"),
                source(id = 1, lang = "en", name = "Beta"),
                source(id = 2, lang = "en", name = "Alpha"),
                source(id = 3, lang = "en", name = "Gamma"),
                source(id = LOCAL_SOURCE_ID, lang = "local", name = "Archive"),
            ),
            enabledLanguages = setOf("en"),
            disabledSourceIds = setOf("2"),
            hiddenSourceIds = setOf(3),
            pinnedSourceIds = emptySet(),
            lastUsedSourceId = -1,
            dataSaverExcludedSourceIds = emptySet(),
            sourceCategoryPreferences = emptySet(),
            sourceCategoriesFilterEnabled = false,
            isLocalSource = { it.id == LOCAL_SOURCE_ID },
        )

        assertEquals(listOf(LOCAL_SOURCE_ID, 1), result.map(Source::id))
    }

    @Test
    fun selectEnabledSourcesExpandsPinnedLastUsedAndCategoryRows() {
        val result = SourceCatalogPolicy.selectEnabledSources(
            sources = listOf(source(id = 10, lang = "en", name = "Pinned")),
            enabledLanguages = setOf("en"),
            disabledSourceIds = emptySet(),
            hiddenSourceIds = emptySet(),
            pinnedSourceIds = setOf("10"),
            lastUsedSourceId = 10,
            dataSaverExcludedSourceIds = setOf("10"),
            sourceCategoryPreferences = setOf("10|Favorites", "10|Long reads"),
            sourceCategoriesFilterEnabled = false,
            isLocalSource = { false },
        )

        assertEquals(4, result.size)

        val base = result[0]
        assertEquals(Pins.pinned, base.pin)
        assertEquals(setOf("Favorites", "Long reads"), base.categories)
        assertTrue(base.isExcludedFromDataSaver)
        assertFalse(base.isUsedLast)
        assertEquals(null, base.category)

        val lastUsed = result[1]
        assertTrue(lastUsed.isUsedLast)
        assertTrue(Pin.Pinned in lastUsed.pin)
        assertFalse(Pin.Actual in lastUsed.pin)

        assertEquals(listOf("Favorites", "Long reads"), result.drop(2).map(Source::category))
        result.drop(2).forEach {
            assertFalse(Pin.Actual in it.pin)
        }
    }

    @Test
    fun selectEnabledSourcesHidesBaseRowForUnpinnedCategorizedSourcesWhenFilterIsEnabled() {
        val result = SourceCatalogPolicy.selectEnabledSources(
            sources = listOf(source(id = 5, lang = "en", name = "Categorized")),
            enabledLanguages = setOf("en"),
            disabledSourceIds = emptySet(),
            hiddenSourceIds = emptySet(),
            pinnedSourceIds = emptySet(),
            lastUsedSourceId = -1,
            dataSaverExcludedSourceIds = emptySet(),
            sourceCategoryPreferences = setOf("5|Group"),
            sourceCategoriesFilterEnabled = true,
            isLocalSource = { false },
        )

        assertEquals(listOf("Group"), result.map(Source::category))
        assertEquals(listOf(5L), result.map(Source::id))
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
        const val LOCAL_SOURCE_ID = 99L
    }
}
