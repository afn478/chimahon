package tachiyomi.domain.source.model

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StubSourceTest {
    @Test
    fun toStringFormatsInstalledSourceName() {
        val source = StubSource(id = 1, lang = "en", name = "MangaDex")

        assertEquals("MangaDex (EN)", source.toString())
    }

    @Test
    fun toStringFallsBackToIdWhenMetadataIsMissing() {
        assertEquals("2", StubSource(id = 2, lang = "", name = "MangaDex").toString())
        assertEquals("3", StubSource(id = 3, lang = "en", name = "").toString())
    }

    @Test
    fun fromCopiesSourceMetadata() {
        val source = StubSource(id = 10, lang = "ja", name = "Manga Source")
        val stubSource = StubSource.from(source)

        assertEquals(source.id, stubSource.id)
        assertEquals(source.lang, stubSource.lang)
        assertEquals(source.name, stubSource.name)
    }

    @Test
    fun sourceRequestsFailAsNotInstalled() = runTest {
        val source = StubSource(id = 1, lang = "en", name = "MangaDex")
        val manga = SManga.create()
        val chapter = SChapter.create()

        assertFailsWith<SourceNotInstalledException> {
            source.getMangaDetails(manga)
        }
        assertFailsWith<SourceNotInstalledException> {
            source.getChapterList(manga)
        }
        assertFailsWith<SourceNotInstalledException> {
            source.getPageList(chapter)
        }
        assertFailsWith<SourceNotInstalledException> {
            source.getRelatedMangaList(
                manga = manga,
                exceptionHandler = {},
                pushResults = { _, _ -> },
            )
        }
    }
}
