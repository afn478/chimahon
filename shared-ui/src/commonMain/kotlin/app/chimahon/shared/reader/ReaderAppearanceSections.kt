package app.chimahon.shared.reader

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.domain.reader.model.CustomReaderTheme
import tachiyomi.domain.reader.model.NovelReaderTheme
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.LayoutSectionState
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.MarginsSectionState
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.ThemeSectionState
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.TypographySectionState

@Composable
fun ReaderThemeSection(
    state: ThemeSectionState,
    selectedTheme: NovelReaderTheme,
    onThemeSelected: (NovelReaderTheme) -> Unit,
    onCustomThemeSelected: (CustomReaderTheme) -> Unit,
    onCustomThemeRename: (CustomReaderTheme) -> Unit,
    onCustomThemeDelete: (CustomReaderTheme) -> Unit,
    onAddCustomTheme: () -> Unit,
    onSystemLightSepiaChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ReaderAppearanceSectionTitle(state.title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.fixedOptions.forEach { option ->
                ReaderThemeSwatchButton(
                    label = option.label,
                    backgroundColor = option.backgroundColor,
                    textColor = option.textColor,
                    splitBackgroundColor = option.splitBackgroundColor,
                    selected = selectedTheme == option.theme,
                    onClick = { onThemeSelected(option.theme) },
                )
            }
            state.customChoices.forEach { choice ->
                val customTheme = choice.theme
                ReaderThemeSwatchButton(
                    label = choice.label,
                    backgroundColor = customTheme.backgroundColor,
                    textColor = customTheme.textColor,
                    selected = choice.selected,
                    onClick = { onCustomThemeSelected(customTheme) },
                    onRenameClick = { onCustomThemeRename(customTheme) },
                    onDeleteClick = { onCustomThemeDelete(customTheme) },
                )
            }
            ReaderAddThemeButton(onClick = onAddCustomTheme)
        }

        val systemLightSepiaSwitchState = state.systemLightSepia
        if (systemLightSepiaSwitchState.visible) {
            ReaderAppearanceSwitchRow(
                state = systemLightSepiaSwitchState.switchState,
                onCheckedChange = onSystemLightSepiaChange,
            )
        }
    }
}

@Composable
fun ReaderTypographySection(
    state: TypographySectionState,
    onFontSelected: (String) -> Unit,
    onImportFontClick: () -> Unit,
    onDeleteFontClick: () -> Unit,
    onFontSizeChange: (Double) -> Unit,
    onLineHeightChange: (Double) -> Unit,
    onHideFuriganaChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ReaderAppearanceSectionTitle(state.title)

        ReaderFontDropdown(
            state = state.fontDropdown,
            onFontSelected = onFontSelected,
        )

        ReaderFontActionRow(
            importState = state.fontImportButton,
            deleteState = state.fontDeleteButton,
            onImportClick = onImportFontClick,
            onDeleteClick = onDeleteFontClick,
        )

        ReaderAppearanceSlider(
            state = state.fontSizeSlider,
            onValueChange = onFontSizeChange,
        )

        ReaderAppearanceSlider(
            state = state.lineHeightSlider,
            onValueChange = onLineHeightChange,
        )

        ReaderAppearanceSwitchRow(
            state = state.hideFuriganaSwitch,
            onCheckedChange = onHideFuriganaChange,
        )

        ReaderAppearanceSwitchRow(
            state = state.keepScreenOnSwitch,
            onCheckedChange = onKeepScreenOnChange,
        )
    }
}

@Composable
fun ReaderMarginsSection(
    state: MarginsSectionState,
    onHorizontalPaddingChange: (Double) -> Unit,
    onVerticalPaddingChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ReaderAppearanceSectionTitle(state.title)

        ReaderAppearanceSlider(
            state = state.horizontalPaddingSlider,
            onValueChange = onHorizontalPaddingChange,
        )

        ReaderAppearanceSlider(
            state = state.verticalPaddingSlider,
            onValueChange = onVerticalPaddingChange,
        )
    }
}

@Composable
fun ReaderLayoutSection(
    state: LayoutSectionState,
    onWritingModeChange: (Boolean) -> Unit,
    onTapZonePercentChange: (Double) -> Unit,
    onAdvancedExpandedChange: (Boolean) -> Unit,
    onAvoidPageBreakChange: (Boolean) -> Unit,
    onJustifyTextChange: (Boolean) -> Unit,
    onCharacterSpacingChange: (Double) -> Unit,
    onParagraphSpacingChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ReaderAppearanceSectionTitle(state.title)

        ReaderAppearanceBooleanSegmentedControl(
            state = state.writingMode,
            onOptionSelected = onWritingModeChange,
        )

        ReaderAppearanceSlider(
            state = state.tapZoneSlider,
            onValueChange = onTapZonePercentChange,
        )

        val advancedToggleState = state.advancedToggle
        ReaderAppearanceAdvancedToggleRow(
            label = state.advancedLabel,
            state = advancedToggleState,
            onClick = { onAdvancedExpandedChange(advancedToggleState.nextExpanded) },
        )

        if (advancedToggleState.expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ReaderAppearanceSwitchRow(
                    state = state.avoidPageBreakSwitch,
                    onCheckedChange = onAvoidPageBreakChange,
                    compact = true,
                )

                ReaderAppearanceSwitchRow(
                    state = state.justifyTextSwitch,
                    onCheckedChange = onJustifyTextChange,
                    compact = true,
                )

                ReaderAppearanceSlider(
                    state = state.characterSpacingSlider,
                    onValueChange = onCharacterSpacingChange,
                    compact = true,
                )

                ReaderAppearanceSlider(
                    state = state.paragraphSpacingSlider,
                    onValueChange = onParagraphSpacingChange,
                    compact = true,
                )
            }
        }
    }
}
