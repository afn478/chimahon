package app.chimahon.shared.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.domain.reader.model.CustomReaderTheme
import tachiyomi.domain.reader.model.NovelReaderTheme
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.BooleanSegmentedControlState
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.LayoutSectionState
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.MarginsSectionState
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.ThemeSectionState
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.TypographySectionState

data class ReaderAppearanceSheetContentState(
    val theme: ThemeSectionState,
    val selectedTheme: NovelReaderTheme,
    val readerMode: BooleanSegmentedControlState,
    val typography: TypographySectionState,
    val margins: MarginsSectionState,
    val layout: LayoutSectionState,
)

class ReaderAppearanceSheetContentActions(
    val onThemeSelected: (NovelReaderTheme) -> Unit,
    val onCustomThemeSelected: (CustomReaderTheme) -> Unit,
    val onCustomThemeRename: (CustomReaderTheme) -> Unit,
    val onCustomThemeDelete: (CustomReaderTheme) -> Unit,
    val onAddCustomTheme: () -> Unit,
    val onSystemLightSepiaChange: (Boolean) -> Unit,
    val onReaderModeChange: (Boolean) -> Unit,
    val onFontSelected: (String) -> Unit,
    val onImportFontClick: () -> Unit,
    val onDeleteFontClick: () -> Unit,
    val onFontSizeChange: (Double) -> Unit,
    val onLineHeightChange: (Double) -> Unit,
    val onHideFuriganaChange: (Boolean) -> Unit,
    val onKeepScreenOnChange: (Boolean) -> Unit,
    val onHorizontalPaddingChange: (Double) -> Unit,
    val onVerticalPaddingChange: (Double) -> Unit,
    val onWritingModeChange: (Boolean) -> Unit,
    val onTapZonePercentChange: (Double) -> Unit,
    val onAdvancedExpandedChange: (Boolean) -> Unit,
    val onAvoidPageBreakChange: (Boolean) -> Unit,
    val onJustifyTextChange: (Boolean) -> Unit,
    val onCharacterSpacingChange: (Double) -> Unit,
    val onParagraphSpacingChange: (Double) -> Unit,
)

@Composable
fun ReaderAppearanceSheetContent(
    state: ReaderAppearanceSheetContentState,
    actions: ReaderAppearanceSheetContentActions,
    modifier: Modifier = Modifier,
    additionalSettings: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            NovelReaderAppearanceSheetPolicy.TITLE,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp),
        )

        ReaderThemeSection(
            state = state.theme,
            selectedTheme = state.selectedTheme,
            onThemeSelected = actions.onThemeSelected,
            onCustomThemeSelected = actions.onCustomThemeSelected,
            onCustomThemeRename = actions.onCustomThemeRename,
            onCustomThemeDelete = actions.onCustomThemeDelete,
            onAddCustomTheme = actions.onAddCustomTheme,
            onSystemLightSepiaChange = actions.onSystemLightSepiaChange,
        )

        ReaderModeSection(
            state = state.readerMode,
            onModeSelected = actions.onReaderModeChange,
        )

        ReaderTypographySection(
            state = state.typography,
            onFontSelected = actions.onFontSelected,
            onImportFontClick = actions.onImportFontClick,
            onDeleteFontClick = actions.onDeleteFontClick,
            onFontSizeChange = actions.onFontSizeChange,
            onLineHeightChange = actions.onLineHeightChange,
            onHideFuriganaChange = actions.onHideFuriganaChange,
            onKeepScreenOnChange = actions.onKeepScreenOnChange,
        )

        ReaderMarginsSection(
            state = state.margins,
            onHorizontalPaddingChange = actions.onHorizontalPaddingChange,
            onVerticalPaddingChange = actions.onVerticalPaddingChange,
        )

        ReaderLayoutSection(
            state = state.layout,
            onWritingModeChange = actions.onWritingModeChange,
            onTapZonePercentChange = actions.onTapZonePercentChange,
            onAdvancedExpandedChange = actions.onAdvancedExpandedChange,
            onAvoidPageBreakChange = actions.onAvoidPageBreakChange,
            onJustifyTextChange = actions.onJustifyTextChange,
            onCharacterSpacingChange = actions.onCharacterSpacingChange,
            onParagraphSpacingChange = actions.onParagraphSpacingChange,
        )

        additionalSettings()
    }
}
