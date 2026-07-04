package tachiyomi.domain.reader.service

import kotlin.math.abs
import kotlin.math.roundToInt
import tachiyomi.domain.reader.model.CustomReaderTheme
import tachiyomi.domain.reader.model.NovelReaderTheme

object NovelReaderAppearanceSheetPolicy {
    const val TITLE = "Appearance"
    const val THEME_SECTION_TITLE = "Theme"
    const val SYSTEM_LIGHT_SEPIA_LABEL = "System uses Sepia"
    const val MODE_SECTION_TITLE = "Mode"
    const val PAGINATED_MODE_LABEL = "Paginated"
    const val CONTINUOUS_MODE_LABEL = "Continuous"
    const val TYPOGRAPHY_SECTION_TITLE = "Typography"
    const val FONT_FAMILY_LABEL = "Font Family"
    const val IMPORT_FONT_BUTTON_TEXT = "Import Font"
    const val DELETE_FONT_BUTTON_TEXT = "Delete Font"
    const val FONT_SIZE_LABEL = "Font Size"
    const val LINE_HEIGHT_LABEL = "Line Height"
    const val HIDE_FURIGANA_LABEL = "Hide Furigana"
    const val KEEP_SCREEN_ON_LABEL = "Keep screen on"
    const val MARGINS_SECTION_TITLE = "Margins"
    const val HORIZONTAL_PADDING_LABEL = "Horizontal Padding"
    const val VERTICAL_PADDING_LABEL = "Vertical Padding"
    const val LAYOUT_SECTION_TITLE = "Layout"
    const val WRITING_MODE_LABEL = "Writing Mode"
    const val VERTICAL_WRITING_LABEL = "Vertical"
    const val HORIZONTAL_WRITING_LABEL = "Horizontal"
    const val TAP_ZONE_SIZE_LABEL = "Tap Zone Size (Navigation)"
    const val ADVANCED_LABEL = "Advanced"
    const val COLLAPSE_CONTENT_DESCRIPTION = "Collapse"
    const val EXPAND_CONTENT_DESCRIPTION = "Expand"
    const val AVOID_PAGE_BREAK_LABEL = "Avoid Page Break"
    const val JUSTIFY_TEXT_LABEL = "Justify Text"
    const val CHARACTER_SPACING_LABEL = "Character Spacing"
    const val PARAGRAPH_SPACING_LABEL = "Paragraph Spacing"
    const val ADD_CUSTOM_THEME_CONTENT_DESCRIPTION = "Add custom theme"
    const val ADD_CUSTOM_THEME_LABEL = "New"
    const val CUSTOM_THEME_FALLBACK_LABEL = "Custom theme"
    const val CUSTOM_THEME_PREVIEW_TEXT = "Aa"
    const val RENAME_MENU_TEXT = "Rename"
    const val DELETE_MENU_TEXT = "Delete"
    const val RENAME_THEME_TITLE = "Rename theme"
    const val THEME_NAME_LABEL = "Theme name"
    const val THEME_NAME_PLACEHOLDER = "Enter a name"
    const val RENAME_BUTTON_TEXT = "Rename"
    const val CANCEL_BUTTON_TEXT = "Cancel"
    const val DELETE_THEME_TITLE = "Delete theme"
    const val DELETE_BUTTON_TEXT = "Delete"
    const val INVALID_COLOR_LABEL = "Invalid"
    val fontImportMimeTypes = listOf(
        "application/font-ttf",
        "application/x-font-ttf",
        "font/ttf",
        "application/octet-stream",
    )

    data class ThemeOption(
        val theme: NovelReaderTheme,
        val label: String,
        val backgroundColor: Int,
        val textColor: Int,
        val splitBackgroundColor: Int? = null,
    )

    data class CustomThemeChoice(
        val theme: CustomReaderTheme,
        val label: String,
        val selected: Boolean,
    )

    data class ColorInputState(
        val label: String,
        val placeholder: String,
        val parsedColor: Int?,
        val previewColor: Int,
        val isValid: Boolean,
        val supportingText: String,
        val review: ColorReviewState,
    )

    data class ColorReviewState(
        val label: String,
        val color: Int,
        val valueText: String,
        val isValid: Boolean,
    )

    data class CustomThemeDialogState(
        val title: String,
        val sampleThemeName: String,
        val sampleText: String,
        val nameLabel: String,
        val namePlaceholder: String,
        val reviewLabel: String,
        val background: ColorInputState,
        val text: ColorInputState,
        val confirmButtonText: String,
        val dismissButtonText: String,
        val confirmEnabled: Boolean,
    )

    data class CustomThemeDraftState(
        val name: String,
        val backgroundColor: Int,
        val textColor: Int,
        val backgroundColorInput: String,
        val textColorInput: String,
    )

    data class CustomThemeColorInputUpdate(
        val input: String,
        val color: Int,
    )

    data class RenameThemeDialogState(
        val title: String,
        val nameLabel: String,
        val namePlaceholder: String,
        val confirmButtonText: String,
        val dismissButtonText: String,
        val confirmEnabled: Boolean,
    )

    data class DeleteThemeDialogState(
        val title: String,
        val message: String,
        val confirmButtonText: String,
        val dismissButtonText: String,
    )

    data class ThemeSwatchMenuState(
        val showRename: Boolean,
        val showDelete: Boolean,
        val renameMenuText: String,
        val deleteMenuText: String,
    ) {
        val enabled: Boolean
            get() = showRename || showDelete
    }

    data class FontDeleteAction(
        val fontName: String,
        val fallbackFont: String,
    )

    data class FontDropdownState(
        val label: String,
        val selectedFont: String,
        val choices: List<String>,
    )

    data class FontImportButtonState(
        val buttonText: String,
        val enabled: Boolean,
        val showProgress: Boolean,
        val mimeTypes: List<String>,
    )

    data class FontDeleteButtonState(
        val visible: Boolean,
        val buttonText: String,
    )

    sealed interface FontImportResultAction {
        data object KeepExistingFonts : FontImportResultAction
        data class RefreshFonts(val importedFonts: List<String>) : FontImportResultAction
    }

    data class BooleanSegmentOption(
        val label: String,
        val value: Boolean,
        val selected: Boolean,
    )

    data class BooleanSegmentedControlState(
        val label: String,
        val options: List<BooleanSegmentOption>,
    )

    data class SwitchControlState(
        val label: String,
        val checked: Boolean,
    )

    data class ConditionalSwitchControlState(
        val visible: Boolean,
        val switchState: SwitchControlState,
    )

    enum class AdvancedToggleIcon {
        EXPAND,
        COLLAPSE,
    }

    data class AdvancedToggleState(
        val expanded: Boolean,
        val nextExpanded: Boolean,
        val icon: AdvancedToggleIcon,
        val contentDescription: String,
    )

    data class SliderSpec(
        val min: Double,
        val max: Double,
        val steps: Int,
    )

    data class SliderControlState(
        val label: String,
        val value: Double,
        val valueText: String,
        val spec: SliderSpec,
    )

    val fixedThemeOptions = listOf(
        ThemeOption(
            theme = NovelReaderTheme.SYSTEM,
            label = "System",
            backgroundColor = 0xFFFFFFFF.toInt(),
            textColor = 0xFF111111.toInt(),
            splitBackgroundColor = 0xFF121212.toInt(),
        ),
        ThemeOption(
            theme = NovelReaderTheme.LIGHT,
            label = "Light",
            backgroundColor = 0xFFFFFFFF.toInt(),
            textColor = 0xFF111111.toInt(),
        ),
        ThemeOption(
            theme = NovelReaderTheme.DARK,
            label = "Dark",
            backgroundColor = 0xFF121212.toInt(),
            textColor = 0xFFE0E0E0.toInt(),
        ),
        ThemeOption(
            theme = NovelReaderTheme.SEPIA,
            label = "Sepia",
            backgroundColor = 0xFFF2E2C9.toInt(),
            textColor = 0xFF3C2C1C.toInt(),
        ),
        ThemeOption(
            theme = NovelReaderTheme.PURE_BLACK,
            label = "AMOLED",
            backgroundColor = 0xFF000000.toInt(),
            textColor = 0xFFE0E0E0.toInt(),
        ),
    )

    val fontSizeSliderSpec = SliderSpec(min = 12.0, max = 72.0, steps = 119)
    val lineHeightSliderSpec = SliderSpec(min = 1.0, max = 2.5, steps = 29)
    val paddingSliderSpec = SliderSpec(min = 0.0, max = 50.0, steps = 99)
    val tapZoneSliderSpec = SliderSpec(min = 0.0, max = 40.0, steps = 39)
    val characterSpacingSliderSpec = SliderSpec(min = 0.0, max = 0.5, steps = 9)
    val paragraphSpacingSliderSpec = SliderSpec(min = 0.0, max = 2.0, steps = 39)

    fun shouldShowSystemLightSepia(theme: NovelReaderTheme): Boolean {
        return theme == NovelReaderTheme.SYSTEM
    }

    fun systemLightSepiaSwitchState(
        theme: NovelReaderTheme,
        systemLightSepia: Boolean,
    ): ConditionalSwitchControlState {
        return ConditionalSwitchControlState(
            visible = shouldShowSystemLightSepia(theme),
            switchState = SwitchControlState(
                label = SYSTEM_LIGHT_SEPIA_LABEL,
                checked = systemLightSepia,
            ),
        )
    }

    fun readerModeOptions(continuousMode: Boolean): List<BooleanSegmentOption> {
        return listOf(
            BooleanSegmentOption(
                label = PAGINATED_MODE_LABEL,
                value = false,
                selected = !continuousMode,
            ),
            BooleanSegmentOption(
                label = CONTINUOUS_MODE_LABEL,
                value = true,
                selected = continuousMode,
            ),
        )
    }

    fun readerModeControlState(continuousMode: Boolean): BooleanSegmentedControlState {
        return BooleanSegmentedControlState(
            label = MODE_SECTION_TITLE,
            options = readerModeOptions(continuousMode),
        )
    }

    fun writingModeOptions(verticalWriting: Boolean): List<BooleanSegmentOption> {
        return listOf(
            BooleanSegmentOption(
                label = VERTICAL_WRITING_LABEL,
                value = true,
                selected = verticalWriting,
            ),
            BooleanSegmentOption(
                label = HORIZONTAL_WRITING_LABEL,
                value = false,
                selected = !verticalWriting,
            ),
        )
    }

    fun writingModeControlState(verticalWriting: Boolean): BooleanSegmentedControlState {
        return BooleanSegmentedControlState(
            label = WRITING_MODE_LABEL,
            options = writingModeOptions(verticalWriting),
        )
    }

    fun advancedToggleState(layoutAdvanced: Boolean): AdvancedToggleState {
        return if (layoutAdvanced) {
            AdvancedToggleState(
                expanded = true,
                nextExpanded = false,
                icon = AdvancedToggleIcon.COLLAPSE,
                contentDescription = COLLAPSE_CONTENT_DESCRIPTION,
            )
        } else {
            AdvancedToggleState(
                expanded = false,
                nextExpanded = true,
                icon = AdvancedToggleIcon.EXPAND,
                contentDescription = EXPAND_CONTENT_DESCRIPTION,
            )
        }
    }

    fun fontChoices(
        defaultFonts: List<String>,
        importedFonts: List<String>,
    ): List<String> {
        return defaultFonts + importedFonts
    }

    fun fontDropdownState(
        selectedFont: String,
        defaultFonts: List<String>,
        importedFonts: List<String>,
    ): FontDropdownState {
        return FontDropdownState(
            label = FONT_FAMILY_LABEL,
            selectedFont = selectedFont,
            choices = fontChoices(
                defaultFonts = defaultFonts,
                importedFonts = importedFonts,
            ),
        )
    }

    fun fontImportButtonState(isImporting: Boolean): FontImportButtonState {
        return FontImportButtonState(
            buttonText = IMPORT_FONT_BUTTON_TEXT,
            enabled = !isImporting,
            showProgress = isImporting,
            mimeTypes = fontImportMimeTypes,
        )
    }

    fun fontDeleteButtonState(
        selectedFont: String,
        importedFonts: List<String>,
    ): FontDeleteButtonState {
        return FontDeleteButtonState(
            visible = shouldShowDeleteFontButton(
                selectedFont = selectedFont,
                importedFonts = importedFonts,
            ),
            buttonText = DELETE_FONT_BUTTON_TEXT,
        )
    }

    fun shouldShowDeleteFontButton(
        selectedFont: String,
        importedFonts: List<String>,
    ): Boolean {
        return selectedFont in importedFonts
    }

    fun fontDeleteAction(
        selectedFont: String,
        importedFonts: List<String>,
        defaultFonts: List<String>,
    ): FontDeleteAction? {
        if (!shouldShowDeleteFontButton(selectedFont, importedFonts)) return null

        return FontDeleteAction(
            fontName = selectedFont,
            fallbackFont = defaultFonts.firstOrNull() ?: selectedFont,
        )
    }

    fun fontImportResultAction(
        success: Boolean,
        importedFonts: List<String>,
    ): FontImportResultAction {
        return if (success) {
            FontImportResultAction.RefreshFonts(importedFonts)
        } else {
            FontImportResultAction.KeepExistingFonts
        }
    }

    fun hideFuriganaSwitchState(hideFurigana: Boolean): SwitchControlState {
        return SwitchControlState(
            label = HIDE_FURIGANA_LABEL,
            checked = hideFurigana,
        )
    }

    fun keepScreenOnSwitchState(keepScreenOn: Boolean): SwitchControlState {
        return SwitchControlState(
            label = KEEP_SCREEN_ON_LABEL,
            checked = keepScreenOn,
        )
    }

    fun avoidPageBreakSwitchState(avoidPageBreak: Boolean): SwitchControlState {
        return SwitchControlState(
            label = AVOID_PAGE_BREAK_LABEL,
            checked = avoidPageBreak,
        )
    }

    fun justifyTextSwitchState(justifyText: Boolean): SwitchControlState {
        return SwitchControlState(
            label = JUSTIFY_TEXT_LABEL,
            checked = justifyText,
        )
    }

    fun fontSizeSliderState(fontSize: Double): SliderControlState {
        return SliderControlState(
            label = FONT_SIZE_LABEL,
            value = fontSize,
            valueText = fontSizeLabel(fontSize),
            spec = fontSizeSliderSpec,
        )
    }

    fun lineHeightSliderState(lineHeight: Double): SliderControlState {
        return SliderControlState(
            label = LINE_HEIGHT_LABEL,
            value = lineHeight,
            valueText = lineHeightLabel(lineHeight),
            spec = lineHeightSliderSpec,
        )
    }

    fun horizontalPaddingSliderState(horizontalPadding: Double): SliderControlState {
        return SliderControlState(
            label = HORIZONTAL_PADDING_LABEL,
            value = horizontalPadding,
            valueText = paddingPercentLabel(horizontalPadding),
            spec = paddingSliderSpec,
        )
    }

    fun verticalPaddingSliderState(verticalPadding: Double): SliderControlState {
        return SliderControlState(
            label = VERTICAL_PADDING_LABEL,
            value = verticalPadding,
            valueText = paddingPercentLabel(verticalPadding),
            spec = paddingSliderSpec,
        )
    }

    fun tapZoneSliderState(tapZonePercent: Int): SliderControlState {
        return SliderControlState(
            label = TAP_ZONE_SIZE_LABEL,
            value = tapZonePercent.toDouble(),
            valueText = tapZonePercentLabel(tapZonePercent),
            spec = tapZoneSliderSpec,
        )
    }

    fun characterSpacingSliderState(characterSpacing: Double): SliderControlState {
        return SliderControlState(
            label = CHARACTER_SPACING_LABEL,
            value = characterSpacing,
            valueText = characterSpacingLabel(characterSpacing),
            spec = characterSpacingSliderSpec,
        )
    }

    fun paragraphSpacingSliderState(paragraphSpacing: Double): SliderControlState {
        return SliderControlState(
            label = PARAGRAPH_SPACING_LABEL,
            value = paragraphSpacing,
            valueText = paragraphSpacingLabel(paragraphSpacing),
            spec = paragraphSpacingSliderSpec,
        )
    }

    fun customThemeChoices(
        theme: NovelReaderTheme,
        customThemes: List<CustomReaderTheme>,
        customBackgroundColor: Int,
        customTextColor: Int,
    ): List<CustomThemeChoice> {
        val currentCustomTheme = customThemes.firstOrNull {
            it.backgroundColor == customBackgroundColor && it.textColor == customTextColor
        } ?: CustomReaderTheme(
            backgroundColor = customBackgroundColor,
            textColor = customTextColor,
        )
        val choices = if (theme == NovelReaderTheme.CUSTOM && currentCustomTheme !in customThemes) {
            listOf(currentCustomTheme) + customThemes
        } else {
            customThemes
        }
        return choices.mapIndexed { index, customTheme ->
            CustomThemeChoice(
                theme = customTheme,
                label = customTheme.name.ifBlank { customThemeLabel(index) },
                selected = theme == NovelReaderTheme.CUSTOM &&
                    customBackgroundColor == customTheme.backgroundColor &&
                    customTextColor == customTheme.textColor,
            )
        }
    }

    fun customThemeDialogState(
        themeName: String,
        backgroundColor: Int,
        textColor: Int,
        backgroundColorInput: String,
        textColorInput: String,
    ): CustomThemeDialogState {
        val parsedBackgroundColor = NovelReaderAppearancePolicy.parseColorInput(backgroundColorInput)
        val parsedTextColor = NovelReaderAppearancePolicy.parseColorInput(textColorInput)

        return CustomThemeDialogState(
            title = "Save theme",
            sampleThemeName = themeName.ifBlank { "Custom" },
            sampleText = "Sample reader text",
            nameLabel = "Name",
            namePlaceholder = "Theme name",
            reviewLabel = "Review",
            background = ColorInputState(
                label = "Background",
                placeholder = "Hex or RGB",
                parsedColor = parsedBackgroundColor,
                previewColor = parsedBackgroundColor ?: backgroundColor,
                isValid = parsedBackgroundColor != null,
                supportingText = "Enter a hex or RGB color",
                review = colorReviewState(
                    label = "Background",
                    parsedColor = parsedBackgroundColor,
                ),
            ),
            text = ColorInputState(
                label = "Text",
                placeholder = "Hex or RGB",
                parsedColor = parsedTextColor,
                previewColor = parsedTextColor ?: textColor,
                isValid = parsedTextColor != null,
                supportingText = "Enter a hex or RGB color",
                review = colorReviewState(
                    label = "Text",
                    parsedColor = parsedTextColor,
                ),
            ),
            confirmButtonText = "Save",
            dismissButtonText = CANCEL_BUTTON_TEXT,
            confirmEnabled = parsedBackgroundColor != null && parsedTextColor != null,
        )
    }

    fun colorReviewState(
        label: String,
        parsedColor: Int?,
    ): ColorReviewState {
        return ColorReviewState(
            label = label,
            color = parsedColor ?: 0x00000000,
            valueText = parsedColor?.let(NovelReaderAppearancePolicy::colorHex) ?: INVALID_COLOR_LABEL,
            isValid = parsedColor != null,
        )
    }

    fun newCustomThemeDraft(
        backgroundColor: Int,
        textColor: Int,
    ): CustomThemeDraftState {
        return CustomThemeDraftState(
            name = "",
            backgroundColor = backgroundColor,
            textColor = textColor,
            backgroundColorInput = NovelReaderAppearancePolicy.colorHex(backgroundColor),
            textColorInput = NovelReaderAppearancePolicy.colorHex(textColor),
        )
    }

    fun customThemeColorInputUpdate(
        currentColor: Int,
        input: String,
    ): CustomThemeColorInputUpdate {
        return CustomThemeColorInputUpdate(
            input = input,
            color = NovelReaderAppearancePolicy.parseColorInput(input) ?: currentColor,
        )
    }

    fun customThemeSaveAction(
        themeName: String,
        backgroundColor: Int,
        textColor: Int,
    ): CustomReaderTheme {
        return CustomReaderTheme(
            name = themeName,
            backgroundColor = backgroundColor,
            textColor = textColor,
        )
    }

    fun renameThemeDialogState(renameInput: String): RenameThemeDialogState {
        return RenameThemeDialogState(
            title = RENAME_THEME_TITLE,
            nameLabel = THEME_NAME_LABEL,
            namePlaceholder = THEME_NAME_PLACEHOLDER,
            confirmButtonText = RENAME_BUTTON_TEXT,
            dismissButtonText = CANCEL_BUTTON_TEXT,
            confirmEnabled = renameInput.isNotBlank(),
        )
    }

    fun deleteThemeDialogState(theme: CustomReaderTheme): DeleteThemeDialogState {
        return DeleteThemeDialogState(
            title = DELETE_THEME_TITLE,
            message = deleteThemeMessage(theme),
            confirmButtonText = DELETE_BUTTON_TEXT,
            dismissButtonText = CANCEL_BUTTON_TEXT,
        )
    }

    fun themeSwatchMenuState(
        canRename: Boolean,
        canDelete: Boolean,
    ): ThemeSwatchMenuState {
        return ThemeSwatchMenuState(
            showRename = canRename,
            showDelete = canDelete,
            renameMenuText = RENAME_MENU_TEXT,
            deleteMenuText = DELETE_MENU_TEXT,
        )
    }

    fun deleteThemeMessage(theme: CustomReaderTheme): String {
        return "Delete \"${theme.name.ifBlank { CUSTOM_THEME_FALLBACK_LABEL }}\"?"
    }

    fun fontSizeLabel(value: Double): String {
        return "${decimalLabel(value, decimals = 2, trimTrailingZeros = true)}px"
    }

    fun lineHeightLabel(value: Double): String {
        return decimalLabel(value, decimals = 2, trimTrailingZeros = false)
    }

    fun paddingPercentLabel(value: Double): String {
        return "${decimalLabel(value, decimals = 2, trimTrailingZeros = true)}%"
    }

    fun tapZonePercentLabel(value: Int): String {
        return "$value%"
    }

    fun characterSpacingLabel(value: Double): String {
        return decimalLabel(value, decimals = 2, trimTrailingZeros = false)
    }

    fun paragraphSpacingLabel(value: Double): String {
        return "${decimalLabel(value, decimals = 2, trimTrailingZeros = false)} em"
    }

    fun snapHalf(value: Double): Double {
        return snapToStep(value = value, stepsPerUnit = 2)
    }

    fun snapTwentieth(value: Double): Double {
        return snapToStep(value = value, stepsPerUnit = 20)
    }

    fun snapWhole(value: Double): Int {
        return value.roundToInt()
    }

    private fun customThemeLabel(index: Int): String {
        return "Custom ${index + 1}"
    }

    private fun snapToStep(
        value: Double,
        stepsPerUnit: Int,
    ): Double {
        return (value * stepsPerUnit).roundToInt() / stepsPerUnit.toDouble()
    }

    private fun decimalLabel(
        value: Double,
        decimals: Int,
        trimTrailingZeros: Boolean,
    ): String {
        val scale = 10.pow(decimals)
        val scaled = (value * scale).roundToInt()
        val sign = if (scaled < 0) "-" else ""
        val absoluteScaled = abs(scaled)
        val whole = absoluteScaled / scale
        val fraction = absoluteScaled % scale

        if (trimTrailingZeros && fraction == 0) {
            return "$sign$whole"
        }

        val rawFractionText = fraction.toString().padStart(decimals, padChar = '0')
        val fractionText = if (trimTrailingZeros) {
            rawFractionText.trimEnd('0')
        } else {
            rawFractionText
        }
        return "$sign$whole.$fractionText"
    }

    private fun Int.pow(exponent: Int): Int {
        var result = 1
        repeat(exponent) {
            result *= this
        }
        return result
    }
}
