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
        val reviewLabel: String,
        val parsedColor: Int?,
        val previewColor: Int,
        val isValid: Boolean,
        val supportingText: String,
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
                reviewLabel = "Background",
                parsedColor = parsedBackgroundColor,
                previewColor = parsedBackgroundColor ?: backgroundColor,
                isValid = parsedBackgroundColor != null,
                supportingText = "Enter a hex or RGB color",
            ),
            text = ColorInputState(
                label = "Text",
                placeholder = "Hex or RGB",
                reviewLabel = "Text",
                parsedColor = parsedTextColor,
                previewColor = parsedTextColor ?: textColor,
                isValid = parsedTextColor != null,
                supportingText = "Enter a hex or RGB color",
            ),
            confirmButtonText = "Save",
            dismissButtonText = CANCEL_BUTTON_TEXT,
            confirmEnabled = parsedBackgroundColor != null && parsedTextColor != null,
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
