package tachiyomi.domain.extension.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtensionSourcePolicyTest {
    @Test
    fun selectSourcesKeepsSingleSourceEnabledAndDoesNotLabelAsName() {
        val result = ExtensionSourcePolicy.selectSources(
            sources = listOf(source(id = 1, name = "Main")),
            disabledSourceIds = emptySet(),
            sourceId = ExtensionSource::id,
            sourceName = ExtensionSource::name,
        )

        assertEquals(listOf(1L), result.map { it.source.id })
        assertTrue(result.single().enabled)
        assertFalse(result.single().labelAsName)
    }

    @Test
    fun selectSourcesUsesDisabledSourceIds() {
        val result = ExtensionSourcePolicy.selectSources(
            sources = listOf(
                source(id = 1, name = "Alpha"),
                source(id = 2, name = "Beta"),
            ),
            disabledSourceIds = setOf("2"),
            sourceId = ExtensionSource::id,
            sourceName = ExtensionSource::name,
        )

        assertEquals(listOf(true, false), result.map { it.enabled })
    }

    @Test
    fun selectSourcesLabelsMultiSourceEntriesByNameWhenSourceNamesDiffer() {
        val result = ExtensionSourcePolicy.selectSources(
            sources = listOf(
                source(id = 1, name = "Alpha"),
                source(id = 2, name = "Beta"),
            ),
            disabledSourceIds = emptySet(),
            sourceId = ExtensionSource::id,
            sourceName = ExtensionSource::name,
        )

        assertEquals(listOf(true, true), result.map { it.labelAsName })
    }

    @Test
    fun selectSourcesDoesNotLabelMultiLanguageSingleSourceEntriesByName() {
        val result = ExtensionSourcePolicy.selectSources(
            sources = listOf(
                source(id = 1, name = "Same"),
                source(id = 2, name = "Same"),
            ),
            disabledSourceIds = emptySet(),
            sourceId = ExtensionSource::id,
            sourceName = ExtensionSource::name,
        )

        assertEquals(listOf(false, false), result.map { it.labelAsName })
    }

    private data class ExtensionSource(
        val id: Long,
        val name: String,
    )

    private fun source(
        id: Long,
        name: String,
    ): ExtensionSource {
        return ExtensionSource(
            id = id,
            name = name,
        )
    }
}
