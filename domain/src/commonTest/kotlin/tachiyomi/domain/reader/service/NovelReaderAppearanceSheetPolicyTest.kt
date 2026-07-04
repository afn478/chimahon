package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import tachiyomi.domain.reader.model.CustomReaderTheme
import tachiyomi.domain.reader.model.NovelReaderTheme

class NovelReaderAppearanceSheetPolicyTest {
    @Test
    fun fixedThemeOptionsRemainStable() {
        assertEquals(
            listOf(
                NovelReaderAppearanceSheetPolicy.ThemeOption(
                    theme = NovelReaderTheme.SYSTEM,
                    label = "System",
                    backgroundColor = 0xFFFFFFFF.toInt(),
                    textColor = 0xFF111111.toInt(),
                    splitBackgroundColor = 0xFF121212.toInt(),
                ),
                NovelReaderAppearanceSheetPolicy.ThemeOption(
                    theme = NovelReaderTheme.LIGHT,
                    label = "Light",
                    backgroundColor = 0xFFFFFFFF.toInt(),
                    textColor = 0xFF111111.toInt(),
                ),
                NovelReaderAppearanceSheetPolicy.ThemeOption(
                    theme = NovelReaderTheme.DARK,
                    label = "Dark",
                    backgroundColor = 0xFF121212.toInt(),
                    textColor = 0xFFE0E0E0.toInt(),
                ),
                NovelReaderAppearanceSheetPolicy.ThemeOption(
                    theme = NovelReaderTheme.SEPIA,
                    label = "Sepia",
                    backgroundColor = 0xFFF2E2C9.toInt(),
                    textColor = 0xFF3C2C1C.toInt(),
                ),
                NovelReaderAppearanceSheetPolicy.ThemeOption(
                    theme = NovelReaderTheme.PURE_BLACK,
                    label = "AMOLED",
                    backgroundColor = 0xFF000000.toInt(),
                    textColor = 0xFFE0E0E0.toInt(),
                ),
            ),
            NovelReaderAppearanceSheetPolicy.fixedThemeOptions,
        )
    }

    @Test
    fun customThemeChoicesIncludeUnsavedSelectedCustomTheme() {
        val choices = NovelReaderAppearanceSheetPolicy.customThemeChoices(
            theme = NovelReaderTheme.CUSTOM,
            customThemes = listOf(
                CustomReaderTheme(
                    name = "Night",
                    backgroundColor = 0xFF101010.toInt(),
                    textColor = 0xFFEAEAEA.toInt(),
                ),
            ),
            customBackgroundColor = 0xFF223344.toInt(),
            customTextColor = 0xFFCCDDEE.toInt(),
        )

        assertEquals(
            listOf(
                NovelReaderAppearanceSheetPolicy.CustomThemeChoice(
                    theme = CustomReaderTheme(
                        backgroundColor = 0xFF223344.toInt(),
                        textColor = 0xFFCCDDEE.toInt(),
                    ),
                    label = "Custom 1",
                    selected = true,
                ),
                NovelReaderAppearanceSheetPolicy.CustomThemeChoice(
                    theme = CustomReaderTheme(
                        name = "Night",
                        backgroundColor = 0xFF101010.toInt(),
                        textColor = 0xFFEAEAEA.toInt(),
                    ),
                    label = "Night",
                    selected = false,
                ),
            ),
            choices,
        )
    }

    @Test
    fun customThemeChoicesSelectExistingCustomTheme() {
        val choices = NovelReaderAppearanceSheetPolicy.customThemeChoices(
            theme = NovelReaderTheme.CUSTOM,
            customThemes = listOf(
                CustomReaderTheme(
                    backgroundColor = 0xFF101010.toInt(),
                    textColor = 0xFFEAEAEA.toInt(),
                ),
            ),
            customBackgroundColor = 0xFF101010.toInt(),
            customTextColor = 0xFFEAEAEA.toInt(),
        )

        assertEquals(
            listOf(
                NovelReaderAppearanceSheetPolicy.CustomThemeChoice(
                    theme = CustomReaderTheme(
                        backgroundColor = 0xFF101010.toInt(),
                        textColor = 0xFFEAEAEA.toInt(),
                    ),
                    label = "Custom 1",
                    selected = true,
                ),
            ),
            choices,
        )
    }

    @Test
    fun fontImportMimeTypesRemainStable() {
        assertEquals(
            listOf(
                "application/font-ttf",
                "application/x-font-ttf",
                "font/ttf",
                "application/octet-stream",
            ),
            NovelReaderAppearanceSheetPolicy.fontImportMimeTypes.toList(),
        )
    }

    @Test
    fun fontChoicesAppendImportedFontsAfterDefaults() {
        assertEquals(
            listOf("System", "Serif", "Mincho", "Gothic"),
            NovelReaderAppearanceSheetPolicy.fontChoices(
                defaultFonts = listOf("System", "Serif"),
                importedFonts = listOf("Mincho", "Gothic"),
            ),
        )
    }

    @Test
    fun deleteFontActionOnlyTargetsImportedFontsAndFallsBackToFirstDefault() {
        assertTrue(
            NovelReaderAppearanceSheetPolicy.shouldShowDeleteFontButton(
                selectedFont = "Mincho",
                importedFonts = listOf("Mincho"),
            ),
        )
        assertFalse(
            NovelReaderAppearanceSheetPolicy.shouldShowDeleteFontButton(
                selectedFont = "System",
                importedFonts = listOf("Mincho"),
            ),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.FontDeleteAction(
                fontName = "Mincho",
                fallbackFont = "System",
            ),
            NovelReaderAppearanceSheetPolicy.fontDeleteAction(
                selectedFont = "Mincho",
                importedFonts = listOf("Mincho"),
                defaultFonts = listOf("System", "Serif"),
            ),
        )
        assertEquals(
            null,
            NovelReaderAppearanceSheetPolicy.fontDeleteAction(
                selectedFont = "System",
                importedFonts = listOf("Mincho"),
                defaultFonts = listOf("System", "Serif"),
            ),
        )
    }

    @Test
    fun deleteFontActionFallsBackToSelectedFontWhenDefaultsAreMissing() {
        assertEquals(
            NovelReaderAppearanceSheetPolicy.FontDeleteAction(
                fontName = "Mincho",
                fallbackFont = "Mincho",
            ),
            NovelReaderAppearanceSheetPolicy.fontDeleteAction(
                selectedFont = "Mincho",
                importedFonts = listOf("Mincho"),
                defaultFonts = emptyList(),
            ),
        )
    }

    @Test
    fun fontImportResultActionRefreshesOnlyAfterSuccessfulImport() {
        assertEquals(
            NovelReaderAppearanceSheetPolicy.FontImportResultAction.RefreshFonts(
                listOf("Mincho"),
            ),
            NovelReaderAppearanceSheetPolicy.fontImportResultAction(
                success = true,
                importedFonts = listOf("Mincho"),
            ),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.FontImportResultAction.KeepExistingFonts,
            NovelReaderAppearanceSheetPolicy.fontImportResultAction(
                success = false,
                importedFonts = listOf("Mincho"),
            ),
        )
    }

    @Test
    fun valueLabelsAndSnappingMatchSheetControls() {
        assertEquals("18px", NovelReaderAppearanceSheetPolicy.fontSizeLabel(18.0))
        assertEquals("18.5px", NovelReaderAppearanceSheetPolicy.fontSizeLabel(18.5))
        assertEquals("1.60", NovelReaderAppearanceSheetPolicy.lineHeightLabel(1.6))
        assertEquals("10%", NovelReaderAppearanceSheetPolicy.paddingPercentLabel(10.0))
        assertEquals("10.5%", NovelReaderAppearanceSheetPolicy.paddingPercentLabel(10.5))
        assertEquals("25%", NovelReaderAppearanceSheetPolicy.tapZonePercentLabel(25))
        assertEquals("0.05", NovelReaderAppearanceSheetPolicy.characterSpacingLabel(0.05))
        assertEquals("1.25 em", NovelReaderAppearanceSheetPolicy.paragraphSpacingLabel(1.25))
        assertEquals(18.5, NovelReaderAppearanceSheetPolicy.snapHalf(18.74))
        assertEquals(1.65, NovelReaderAppearanceSheetPolicy.snapTwentieth(1.63))
        assertEquals(21, NovelReaderAppearanceSheetPolicy.snapWhole(20.6))
    }

    @Test
    fun customThemeDialogStateParsesInputsAndEnablesConfirm() {
        val state = NovelReaderAppearanceSheetPolicy.customThemeDialogState(
            themeName = "",
            backgroundColor = 0xFF000000.toInt(),
            textColor = 0xFFFFFFFF.toInt(),
            backgroundColorInput = "#123456",
            textColorInput = "rgb(10, 20, 30)",
        )

        assertEquals("Custom", state.sampleThemeName)
        assertEquals(0xFF123456.toInt(), state.background.parsedColor)
        assertEquals(0xFF0A141E.toInt(), state.text.parsedColor)
        assertTrue(state.background.isValid)
        assertTrue(state.text.isValid)
        assertTrue(state.confirmEnabled)
    }

    @Test
    fun customThemeDialogStateFallsBackPreviewAndDisablesConfirmForInvalidInput() {
        val state = NovelReaderAppearanceSheetPolicy.customThemeDialogState(
            themeName = "Reader",
            backgroundColor = 0xFF010203.toInt(),
            textColor = 0xFF040506.toInt(),
            backgroundColorInput = "nope",
            textColorInput = "#111111",
        )

        assertEquals("Reader", state.sampleThemeName)
        assertEquals(null, state.background.parsedColor)
        assertEquals(0xFF010203.toInt(), state.background.previewColor)
        assertFalse(state.background.isValid)
        assertFalse(state.confirmEnabled)
        assertEquals("Enter a hex or RGB color", state.background.supportingText)
    }

    @Test
    fun deleteThemeMessageUsesThemeNameOrFallback() {
        assertEquals(
            "Delete \"Night\"?",
            NovelReaderAppearanceSheetPolicy.deleteThemeMessage(
                CustomReaderTheme(
                    name = "Night",
                    backgroundColor = 0,
                    textColor = 1,
                ),
            ),
        )
        assertEquals(
            "Delete \"Custom theme\"?",
            NovelReaderAppearanceSheetPolicy.deleteThemeMessage(
                CustomReaderTheme(
                    backgroundColor = 0,
                    textColor = 1,
                ),
            ),
        )
    }
}
