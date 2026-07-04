package exh.source

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.all.EhBasedSource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceClassificationTest {
    @Test
    fun ehBasedSourcesNeedMarkerAndKnownSourceId() {
        assertTrue(isEhBasedSourceId(EH_SOURCE_ID))
        assertTrue(isEhBasedSourceId(EXH_SOURCE_ID))

        assertTrue(TestEhSource(EH_SOURCE_ID).isEhBasedSource())
        assertFalse(TestEhSource(123L).isEhBasedSource())
        assertFalse(TestSource(EH_SOURCE_ID).isEhBasedSource())
    }

    @Test
    fun mangaDexSourcesUseDelegatedSourceState() = withSourceState(
        mangaDexIds = listOf(10L, 20L),
    ) {
        assertTrue(isMdBasedSourceId(10L))
        assertTrue(TestSource(20L).isMdBasedSource())
        assertFalse(TestSource(30L).isMdBasedSource())
    }

    @Test
    fun metadataSourcesIncludeLegacyRangeAndDelegatedSources() = withSourceState(
        metadataIds = listOf(42L, 100L),
    ) {
        assertTrue(isMetadataSource(6900L))
        assertTrue(isMetadataSource(6999L))
        assertTrue(isMetadataSource(EH_SOURCE_ID))
        assertTrue(isMetadataSource(EXH_SOURCE_ID))
        assertTrue(isMetadataSource(42L))
        assertFalse(isMetadataSource(7000L))
        assertFalse(isMetadataSource(41L))
    }

    private fun withSourceState(
        metadataIds: List<Long> = metadataDelegatedSourceIds,
        nHentaiIds: List<Long> = nHentaiSourceIds,
        lanraragiIds: List<Long> = lanraragiSourceIds,
        mangaDexIds: List<Long> = mangaDexSourceIds,
        libraryUpdateExcludedSources: List<Long> = LIBRARY_UPDATE_EXCLUDED_SOURCES,
        block: () -> Unit,
    ) {
        val previousMetadataIds = metadataDelegatedSourceIds
        val previousNHentaiIds = nHentaiSourceIds
        val previousLanraragiIds = lanraragiSourceIds
        val previousMangaDexIds = mangaDexSourceIds
        val previousLibraryUpdateExcludedSources = LIBRARY_UPDATE_EXCLUDED_SOURCES

        metadataDelegatedSourceIds = metadataIds
        nHentaiSourceIds = nHentaiIds
        lanraragiSourceIds = lanraragiIds
        mangaDexSourceIds = mangaDexIds
        LIBRARY_UPDATE_EXCLUDED_SOURCES = libraryUpdateExcludedSources

        try {
            block()
        } finally {
            metadataDelegatedSourceIds = previousMetadataIds
            nHentaiSourceIds = previousNHentaiIds
            lanraragiSourceIds = previousLanraragiIds
            mangaDexSourceIds = previousMangaDexIds
            LIBRARY_UPDATE_EXCLUDED_SOURCES = previousLibraryUpdateExcludedSources
        }
    }
}

private open class TestSource(
    override val id: Long,
) : Source {
    override val name = "Test"

    override suspend fun getMangaDetails(manga: SManga): SManga = error("Unused")

    override suspend fun getChapterList(manga: SManga): List<SChapter> = error("Unused")

    override suspend fun getPageList(chapter: SChapter): List<Page> = error("Unused")

    override suspend fun getRelatedMangaList(
        manga: SManga,
        exceptionHandler: (Throwable) -> Unit,
        pushResults: suspend (relatedManga: Pair<String, List<SManga>>, completed: Boolean) -> Unit,
    ) = error("Unused")
}

private class TestEhSource(
    id: Long,
) : TestSource(id), EhBasedSource
