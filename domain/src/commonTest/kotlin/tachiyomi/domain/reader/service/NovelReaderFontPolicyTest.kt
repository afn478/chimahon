package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NovelReaderFontPolicyTest {
    @Test
    fun defaultFontNamesRemainStable() {
        assertEquals(
            listOf("System Serif", "System Sans-Serif"),
            NovelReaderFontPolicy.defaultFonts,
        )
    }

    @Test
    fun customFontRequiresNonDefaultImportedFontName() {
        val importedFonts = listOf("Mincho", "Gothic")

        assertTrue(NovelReaderFontPolicy.isCustomFont("Mincho", importedFonts))
        assertFalse(NovelReaderFontPolicy.isCustomFont("System Serif", importedFonts))
        assertFalse(NovelReaderFontPolicy.isCustomFont("Missing", importedFonts))
    }

    @Test
    fun importedFontFileNameUsesTimestampAndTtfFallback() {
        assertEquals(
            "imported_font_1234.ttf",
            NovelReaderFontPolicy.importedFontFileName(1234),
        )
    }

    @Test
    fun fontDisplayNameDropsOnlyLastExtension() {
        assertEquals("Mincho", NovelReaderFontPolicy.fontDisplayName("Mincho.ttf"))
        assertEquals("Font.Name", NovelReaderFontPolicy.fontDisplayName("Font.Name.otf"))
        assertEquals("NoExtension", NovelReaderFontPolicy.fontDisplayName("NoExtension"))
    }

    @Test
    fun importedFontNamesMapFileNamesToDisplayNames() {
        assertEquals(
            listOf("Mincho", "Gothic"),
            NovelReaderFontPolicy.importedFontNames(listOf("Mincho.ttf", "Gothic.otf")),
        )
    }

    @Test
    fun customFontNamesExcludeReservedSystemFonts() {
        assertEquals(
            listOf("Mincho"),
            NovelReaderFontPolicy.customFontNames(listOf("System Serif.ttf", "Mincho.ttf")),
        )
    }

    @Test
    fun matchesImportedFontFileUsesDisplayName() {
        assertTrue(
            NovelReaderFontPolicy.matchesImportedFontFile(
                fileName = "Mincho.ttf",
                fontName = "Mincho",
            ),
        )
        assertFalse(
            NovelReaderFontPolicy.matchesImportedFontFile(
                fileName = "Mincho.ttf",
                fontName = "Gothic",
            ),
        )
        assertFalse(
            NovelReaderFontPolicy.matchesImportedFontFile(
                fileName = "System Serif.ttf",
                fontName = "System Serif",
            ),
        )
    }

    @Test
    fun fontFileUriUsesFileScheme() {
        assertEquals(
            "file:///tmp/fonts/Mincho.ttf",
            NovelReaderFontPolicy.fontFileUri("/tmp/fonts/Mincho.ttf"),
        )
    }

    @Test
    fun cssFontFamilyMapsSystemFontsAndKeepsCustomNames() {
        assertEquals("serif", NovelReaderFontPolicy.cssFontFamily("System Serif"))
        assertEquals("sans-serif", NovelReaderFontPolicy.cssFontFamily("System Sans-Serif"))
        assertEquals("Mincho", NovelReaderFontPolicy.cssFontFamily("Mincho"))
    }
}
