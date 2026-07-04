package tachiyomi.domain.source.service

import tachiyomi.domain.source.model.Source
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceListFilterPolicyTest {
    @Test
    fun filterSourcesMatchesSourceNameSourceIdAndExtensionName() {
        val sources = listOf(
            source(id = 1, name = "Alpha"),
            source(id = 2, name = "Beta"),
            source(id = 3, name = "Gamma"),
        )

        val result = SourceListFilterPolicy.filterSources(
            sources = sources,
            searchQuery = "alpha,2,plugin",
            nsfwOnly = false,
            metadata = {
                SourceListFilterMetadata(
                    extensionName = if (it.id == 3L) "Plugin Pack" else null,
                )
            },
        )

        assertEquals(listOf(1L, 2L, 3L), result.map(Source::id))
    }

    @Test
    fun filterSourcesAppliesNsfwOnlyWhileKeepingUnknownExtensionSafety() {
        val sources = listOf(
            source(id = 1, name = "Nsfw"),
            source(id = 2, name = "Sfw"),
            source(id = 3, name = "Unknown"),
        )

        val result = SourceListFilterPolicy.filterSources(
            sources = sources,
            searchQuery = null,
            nsfwOnly = true,
            metadata = {
                SourceListFilterMetadata(
                    extensionIsNsfw = when (it.id) {
                        1L -> true
                        2L -> false
                        else -> null
                    },
                )
            },
        )

        assertEquals(listOf(1L, 3L), result.map(Source::id))
    }

    @Test
    fun filterItemsPreservesWrapperItemsAndUsesSelectedSource() {
        val wrappedSources = listOf(
            source(id = 1, name = "Alpha") to 5L,
            source(id = 2, name = "Beta") to 7L,
        )

        val result = SourceListFilterPolicy.filterItems(
            items = wrappedSources,
            searchQuery = "beta",
            source = { it.first },
        )

        assertEquals(listOf(7L), result.map { it.second })
    }

    @Test
    fun filterSourcesDoesNotReadMetadataWhenNoFilterNeedsIt() {
        var metadataRequests = 0

        val result = SourceListFilterPolicy.filterSources(
            sources = listOf(source(id = 1, name = "Alpha")),
            searchQuery = null,
            nsfwOnly = false,
            metadata = {
                metadataRequests += 1
                SourceListFilterMetadata(extensionName = "Unused")
            },
        )

        assertEquals(listOf(1L), result.map(Source::id))
        assertEquals(0, metadataRequests)
    }

    @Test
    fun matchesNsfwFilterKeepsExistingUnknownExtensionBehavior() {
        assertTrue(
            SourceListFilterPolicy.matchesNsfwFilter(nsfwOnly = true, extensionIsNsfw = true),
        )
        assertTrue(
            SourceListFilterPolicy.matchesNsfwFilter(nsfwOnly = true, extensionIsNsfw = null),
        )
        assertFalse(
            SourceListFilterPolicy.matchesNsfwFilter(nsfwOnly = true, extensionIsNsfw = false),
        )
        assertTrue(
            SourceListFilterPolicy.matchesNsfwFilter(nsfwOnly = false, extensionIsNsfw = false),
        )
    }

    private fun source(
        id: Long,
        name: String,
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
