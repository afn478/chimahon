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
    fun systemLightSepiaVisibilityOnlyAppliesToSystemTheme() {
        assertTrue(NovelReaderAppearanceSheetPolicy.shouldShowSystemLightSepia(NovelReaderTheme.SYSTEM))
        assertFalse(NovelReaderAppearanceSheetPolicy.shouldShowSystemLightSepia(NovelReaderTheme.LIGHT))
        assertFalse(NovelReaderAppearanceSheetPolicy.shouldShowSystemLightSepia(NovelReaderTheme.SEPIA))
        assertEquals(
            NovelReaderAppearanceSheetPolicy.ConditionalSwitchControlState(
                visible = true,
                switchState = NovelReaderAppearanceSheetPolicy.SwitchControlState(
                    label = NovelReaderAppearanceSheetPolicy.SYSTEM_LIGHT_SEPIA_LABEL,
                    checked = true,
                ),
            ),
            NovelReaderAppearanceSheetPolicy.systemLightSepiaSwitchState(
                theme = NovelReaderTheme.SYSTEM,
                systemLightSepia = true,
            ),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.ConditionalSwitchControlState(
                visible = false,
                switchState = NovelReaderAppearanceSheetPolicy.SwitchControlState(
                    label = NovelReaderAppearanceSheetPolicy.SYSTEM_LIGHT_SEPIA_LABEL,
                    checked = false,
                ),
            ),
            NovelReaderAppearanceSheetPolicy.systemLightSepiaSwitchState(
                theme = NovelReaderTheme.LIGHT,
                systemLightSepia = false,
            ),
        )
    }

    @Test
    fun readerModeOptionsReflectContinuousModeSelection() {
        val paginatedModeOptions = listOf(
            NovelReaderAppearanceSheetPolicy.BooleanSegmentOption(
                label = NovelReaderAppearanceSheetPolicy.PAGINATED_MODE_LABEL,
                value = false,
                selected = true,
            ),
            NovelReaderAppearanceSheetPolicy.BooleanSegmentOption(
                label = NovelReaderAppearanceSheetPolicy.CONTINUOUS_MODE_LABEL,
                value = true,
                selected = false,
            ),
        )
        val continuousModeOptions = listOf(
            NovelReaderAppearanceSheetPolicy.BooleanSegmentOption(
                label = NovelReaderAppearanceSheetPolicy.PAGINATED_MODE_LABEL,
                value = false,
                selected = false,
            ),
            NovelReaderAppearanceSheetPolicy.BooleanSegmentOption(
                label = NovelReaderAppearanceSheetPolicy.CONTINUOUS_MODE_LABEL,
                value = true,
                selected = true,
            ),
        )

        assertEquals(
            paginatedModeOptions,
            NovelReaderAppearanceSheetPolicy.readerModeOptions(continuousMode = false),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.BooleanSegmentedControlState(
                label = NovelReaderAppearanceSheetPolicy.MODE_SECTION_TITLE,
                options = paginatedModeOptions,
            ),
            NovelReaderAppearanceSheetPolicy.readerModeControlState(continuousMode = false),
        )
        assertEquals(
            continuousModeOptions,
            NovelReaderAppearanceSheetPolicy.readerModeOptions(continuousMode = true),
        )
    }

    @Test
    fun writingModeOptionsReflectVerticalWritingSelection() {
        val verticalWritingOptions = listOf(
            NovelReaderAppearanceSheetPolicy.BooleanSegmentOption(
                label = NovelReaderAppearanceSheetPolicy.VERTICAL_WRITING_LABEL,
                value = true,
                selected = true,
            ),
            NovelReaderAppearanceSheetPolicy.BooleanSegmentOption(
                label = NovelReaderAppearanceSheetPolicy.HORIZONTAL_WRITING_LABEL,
                value = false,
                selected = false,
            ),
        )
        val horizontalWritingOptions = listOf(
            NovelReaderAppearanceSheetPolicy.BooleanSegmentOption(
                label = NovelReaderAppearanceSheetPolicy.VERTICAL_WRITING_LABEL,
                value = true,
                selected = false,
            ),
            NovelReaderAppearanceSheetPolicy.BooleanSegmentOption(
                label = NovelReaderAppearanceSheetPolicy.HORIZONTAL_WRITING_LABEL,
                value = false,
                selected = true,
            ),
        )

        assertEquals(
            verticalWritingOptions,
            NovelReaderAppearanceSheetPolicy.writingModeOptions(verticalWriting = true),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.BooleanSegmentedControlState(
                label = NovelReaderAppearanceSheetPolicy.WRITING_MODE_LABEL,
                options = verticalWritingOptions,
            ),
            NovelReaderAppearanceSheetPolicy.writingModeControlState(verticalWriting = true),
        )
        assertEquals(
            horizontalWritingOptions,
            NovelReaderAppearanceSheetPolicy.writingModeOptions(verticalWriting = false),
        )
    }

    @Test
    fun advancedToggleStateTracksExpandedAndNextStates() {
        assertEquals(
            NovelReaderAppearanceSheetPolicy.AdvancedToggleState(
                expanded = true,
                nextExpanded = false,
                icon = NovelReaderAppearanceSheetPolicy.AdvancedToggleIcon.COLLAPSE,
                contentDescription = NovelReaderAppearanceSheetPolicy.COLLAPSE_CONTENT_DESCRIPTION,
            ),
            NovelReaderAppearanceSheetPolicy.advancedToggleState(layoutAdvanced = true),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.AdvancedToggleState(
                expanded = false,
                nextExpanded = true,
                icon = NovelReaderAppearanceSheetPolicy.AdvancedToggleIcon.EXPAND,
                contentDescription = NovelReaderAppearanceSheetPolicy.EXPAND_CONTENT_DESCRIPTION,
            ),
            NovelReaderAppearanceSheetPolicy.advancedToggleState(layoutAdvanced = false),
        )
    }

    @Test
    fun sliderSpecsRemainStable() {
        assertEquals(
            NovelReaderAppearanceSheetPolicy.SliderSpec(min = 12.0, max = 72.0, steps = 119),
            NovelReaderAppearanceSheetPolicy.fontSizeSliderSpec,
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.SliderSpec(min = 1.0, max = 2.5, steps = 29),
            NovelReaderAppearanceSheetPolicy.lineHeightSliderSpec,
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.SliderSpec(min = 0.0, max = 50.0, steps = 99),
            NovelReaderAppearanceSheetPolicy.paddingSliderSpec,
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.SliderSpec(min = 0.0, max = 40.0, steps = 39),
            NovelReaderAppearanceSheetPolicy.tapZoneSliderSpec,
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.SliderSpec(min = 0.0, max = 0.5, steps = 9),
            NovelReaderAppearanceSheetPolicy.characterSpacingSliderSpec,
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.SliderSpec(min = 0.0, max = 2.0, steps = 39),
            NovelReaderAppearanceSheetPolicy.paragraphSpacingSliderSpec,
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
    fun newCustomThemeDraftStartsFromCurrentColorsWithHexInputs() {
        assertEquals(
            NovelReaderAppearanceSheetPolicy.CustomThemeDraftState(
                name = "",
                backgroundColor = 0xFF010203.toInt(),
                textColor = 0xFFABCDEF.toInt(),
                backgroundColorInput = "#010203",
                textColorInput = "#ABCDEF",
            ),
            NovelReaderAppearanceSheetPolicy.newCustomThemeDraft(
                backgroundColor = 0xFF010203.toInt(),
                textColor = 0xFFABCDEF.toInt(),
            ),
        )
    }

    @Test
    fun customThemeColorInputUpdateKeepsLastValidColorForInvalidInput() {
        assertEquals(
            NovelReaderAppearanceSheetPolicy.CustomThemeColorInputUpdate(
                input = "#112233",
                color = 0xFF112233.toInt(),
            ),
            NovelReaderAppearanceSheetPolicy.customThemeColorInputUpdate(
                currentColor = 0xFF010203.toInt(),
                input = "#112233",
            ),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.CustomThemeColorInputUpdate(
                input = "not-a-color",
                color = 0xFF010203.toInt(),
            ),
            NovelReaderAppearanceSheetPolicy.customThemeColorInputUpdate(
                currentColor = 0xFF010203.toInt(),
                input = "not-a-color",
            ),
        )
    }

    @Test
    fun customThemeSaveActionBuildsThemeFromDraftValues() {
        assertEquals(
            CustomReaderTheme(
                name = "Night",
                backgroundColor = 0xFF101010.toInt(),
                textColor = 0xFFEAEAEA.toInt(),
            ),
            NovelReaderAppearanceSheetPolicy.customThemeSaveAction(
                themeName = "Night",
                backgroundColor = 0xFF101010.toInt(),
                textColor = 0xFFEAEAEA.toInt(),
            ),
        )
    }

    @Test
    fun renameThemeDialogStateEnablesConfirmOnlyForNonBlankNames() {
        assertEquals(
            NovelReaderAppearanceSheetPolicy.RenameThemeDialogState(
                title = NovelReaderAppearanceSheetPolicy.RENAME_THEME_TITLE,
                nameLabel = NovelReaderAppearanceSheetPolicy.THEME_NAME_LABEL,
                namePlaceholder = NovelReaderAppearanceSheetPolicy.THEME_NAME_PLACEHOLDER,
                confirmButtonText = NovelReaderAppearanceSheetPolicy.RENAME_BUTTON_TEXT,
                dismissButtonText = NovelReaderAppearanceSheetPolicy.CANCEL_BUTTON_TEXT,
                confirmEnabled = true,
            ),
            NovelReaderAppearanceSheetPolicy.renameThemeDialogState(renameInput = "Night"),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.RenameThemeDialogState(
                title = NovelReaderAppearanceSheetPolicy.RENAME_THEME_TITLE,
                nameLabel = NovelReaderAppearanceSheetPolicy.THEME_NAME_LABEL,
                namePlaceholder = NovelReaderAppearanceSheetPolicy.THEME_NAME_PLACEHOLDER,
                confirmButtonText = NovelReaderAppearanceSheetPolicy.RENAME_BUTTON_TEXT,
                dismissButtonText = NovelReaderAppearanceSheetPolicy.CANCEL_BUTTON_TEXT,
                confirmEnabled = false,
            ),
            NovelReaderAppearanceSheetPolicy.renameThemeDialogState(renameInput = "   "),
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
    fun fontDropdownStateExposesLabelSelectionAndChoices() {
        assertEquals(
            NovelReaderAppearanceSheetPolicy.FontDropdownState(
                label = NovelReaderAppearanceSheetPolicy.FONT_FAMILY_LABEL,
                selectedFont = "Mincho",
                choices = listOf("System", "Serif", "Mincho"),
            ),
            NovelReaderAppearanceSheetPolicy.fontDropdownState(
                selectedFont = "Mincho",
                defaultFonts = listOf("System", "Serif"),
                importedFonts = listOf("Mincho"),
            ),
        )
    }

    @Test
    fun fontImportButtonStateDisablesAndShowsProgressOnlyWhileImporting() {
        assertEquals(
            NovelReaderAppearanceSheetPolicy.FontImportButtonState(
                buttonText = NovelReaderAppearanceSheetPolicy.IMPORT_FONT_BUTTON_TEXT,
                enabled = true,
                showProgress = false,
                mimeTypes = NovelReaderAppearanceSheetPolicy.fontImportMimeTypes,
            ),
            NovelReaderAppearanceSheetPolicy.fontImportButtonState(isImporting = false),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.FontImportButtonState(
                buttonText = NovelReaderAppearanceSheetPolicy.IMPORT_FONT_BUTTON_TEXT,
                enabled = false,
                showProgress = true,
                mimeTypes = NovelReaderAppearanceSheetPolicy.fontImportMimeTypes,
            ),
            NovelReaderAppearanceSheetPolicy.fontImportButtonState(isImporting = true),
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
        assertEquals(
            NovelReaderAppearanceSheetPolicy.FontDeleteButtonState(
                visible = true,
                buttonText = NovelReaderAppearanceSheetPolicy.DELETE_FONT_BUTTON_TEXT,
            ),
            NovelReaderAppearanceSheetPolicy.fontDeleteButtonState(
                selectedFont = "Mincho",
                importedFonts = listOf("Mincho"),
            ),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.FontDeleteButtonState(
                visible = false,
                buttonText = NovelReaderAppearanceSheetPolicy.DELETE_FONT_BUTTON_TEXT,
            ),
            NovelReaderAppearanceSheetPolicy.fontDeleteButtonState(
                selectedFont = "System",
                importedFonts = listOf("Mincho"),
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
    fun readerSwitchStatesExposeLabelsAndCheckedValues() {
        assertEquals(
            NovelReaderAppearanceSheetPolicy.SwitchControlState(
                label = NovelReaderAppearanceSheetPolicy.HIDE_FURIGANA_LABEL,
                checked = true,
            ),
            NovelReaderAppearanceSheetPolicy.hideFuriganaSwitchState(hideFurigana = true),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.SwitchControlState(
                label = NovelReaderAppearanceSheetPolicy.KEEP_SCREEN_ON_LABEL,
                checked = false,
            ),
            NovelReaderAppearanceSheetPolicy.keepScreenOnSwitchState(keepScreenOn = false),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.SwitchControlState(
                label = NovelReaderAppearanceSheetPolicy.AVOID_PAGE_BREAK_LABEL,
                checked = true,
            ),
            NovelReaderAppearanceSheetPolicy.avoidPageBreakSwitchState(avoidPageBreak = true),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.SwitchControlState(
                label = NovelReaderAppearanceSheetPolicy.JUSTIFY_TEXT_LABEL,
                checked = false,
            ),
            NovelReaderAppearanceSheetPolicy.justifyTextSwitchState(justifyText = false),
        )
    }

    @Test
    fun primarySliderStatesExposeLabelsValuesAndSpecs() {
        assertEquals(
            NovelReaderAppearanceSheetPolicy.SliderControlState(
                label = NovelReaderAppearanceSheetPolicy.FONT_SIZE_LABEL,
                value = 18.5,
                valueText = "18.5px",
                spec = NovelReaderAppearanceSheetPolicy.fontSizeSliderSpec,
            ),
            NovelReaderAppearanceSheetPolicy.fontSizeSliderState(fontSize = 18.5),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.SliderControlState(
                label = NovelReaderAppearanceSheetPolicy.LINE_HEIGHT_LABEL,
                value = 1.6,
                valueText = "1.60",
                spec = NovelReaderAppearanceSheetPolicy.lineHeightSliderSpec,
            ),
            NovelReaderAppearanceSheetPolicy.lineHeightSliderState(lineHeight = 1.6),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.SliderControlState(
                label = NovelReaderAppearanceSheetPolicy.HORIZONTAL_PADDING_LABEL,
                value = 10.5,
                valueText = "10.5%",
                spec = NovelReaderAppearanceSheetPolicy.paddingSliderSpec,
            ),
            NovelReaderAppearanceSheetPolicy.horizontalPaddingSliderState(horizontalPadding = 10.5),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.SliderControlState(
                label = NovelReaderAppearanceSheetPolicy.VERTICAL_PADDING_LABEL,
                value = 10.0,
                valueText = "10%",
                spec = NovelReaderAppearanceSheetPolicy.paddingSliderSpec,
            ),
            NovelReaderAppearanceSheetPolicy.verticalPaddingSliderState(verticalPadding = 10.0),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.SliderControlState(
                label = NovelReaderAppearanceSheetPolicy.TAP_ZONE_SIZE_LABEL,
                value = 25.0,
                valueText = "25%",
                spec = NovelReaderAppearanceSheetPolicy.tapZoneSliderSpec,
            ),
            NovelReaderAppearanceSheetPolicy.tapZoneSliderState(tapZonePercent = 25),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.SliderControlState(
                label = NovelReaderAppearanceSheetPolicy.CHARACTER_SPACING_LABEL,
                value = 0.05,
                valueText = "0.05",
                spec = NovelReaderAppearanceSheetPolicy.characterSpacingSliderSpec,
            ),
            NovelReaderAppearanceSheetPolicy.characterSpacingSliderState(characterSpacing = 0.05),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.SliderControlState(
                label = NovelReaderAppearanceSheetPolicy.PARAGRAPH_SPACING_LABEL,
                value = 1.25,
                valueText = "1.25 em",
                spec = NovelReaderAppearanceSheetPolicy.paragraphSpacingSliderSpec,
            ),
            NovelReaderAppearanceSheetPolicy.paragraphSpacingSliderState(paragraphSpacing = 1.25),
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
        assertEquals(
            NovelReaderAppearanceSheetPolicy.ColorReviewState(
                label = "Background",
                color = 0xFF123456.toInt(),
                valueText = "#123456",
                isValid = true,
            ),
            state.background.review,
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.ColorReviewState(
                label = "Text",
                color = 0xFF0A141E.toInt(),
                valueText = "#0A141E",
                isValid = true,
            ),
            state.text.review,
        )
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
        assertEquals(
            NovelReaderAppearanceSheetPolicy.ColorReviewState(
                label = "Background",
                color = 0x00000000,
                valueText = NovelReaderAppearanceSheetPolicy.INVALID_COLOR_LABEL,
                isValid = false,
            ),
            state.background.review,
        )
    }

    @Test
    fun deleteThemeDialogStateUsesThemeNameOrFallback() {
        assertEquals(
            NovelReaderAppearanceSheetPolicy.DeleteThemeDialogState(
                title = NovelReaderAppearanceSheetPolicy.DELETE_THEME_TITLE,
                message = "Delete \"Night\"?",
                confirmButtonText = NovelReaderAppearanceSheetPolicy.DELETE_BUTTON_TEXT,
                dismissButtonText = NovelReaderAppearanceSheetPolicy.CANCEL_BUTTON_TEXT,
            ),
            NovelReaderAppearanceSheetPolicy.deleteThemeDialogState(
                CustomReaderTheme(
                    name = "Night",
                    backgroundColor = 0,
                    textColor = 1,
                ),
            ),
        )
        assertEquals(
            NovelReaderAppearanceSheetPolicy.DeleteThemeDialogState(
                title = NovelReaderAppearanceSheetPolicy.DELETE_THEME_TITLE,
                message = "Delete \"Custom theme\"?",
                confirmButtonText = NovelReaderAppearanceSheetPolicy.DELETE_BUTTON_TEXT,
                dismissButtonText = NovelReaderAppearanceSheetPolicy.CANCEL_BUTTON_TEXT,
            ),
            NovelReaderAppearanceSheetPolicy.deleteThemeDialogState(
                CustomReaderTheme(
                    backgroundColor = 0,
                    textColor = 1,
                ),
            ),
        )
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

    @Test
    fun themeSwatchMenuStateTracksAvailableActions() {
        assertEquals(
            NovelReaderAppearanceSheetPolicy.ThemeSwatchMenuState(
                showRename = true,
                showDelete = true,
                renameMenuText = NovelReaderAppearanceSheetPolicy.RENAME_MENU_TEXT,
                deleteMenuText = NovelReaderAppearanceSheetPolicy.DELETE_MENU_TEXT,
            ),
            NovelReaderAppearanceSheetPolicy.themeSwatchMenuState(
                canRename = true,
                canDelete = true,
            ),
        )
        assertTrue(
            NovelReaderAppearanceSheetPolicy.themeSwatchMenuState(
                canRename = true,
                canDelete = false,
            ).enabled,
        )
        assertFalse(
            NovelReaderAppearanceSheetPolicy.themeSwatchMenuState(
                canRename = false,
                canDelete = false,
            ).enabled,
        )
    }
}
