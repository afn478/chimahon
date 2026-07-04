package com.canopus.chimareader.ui.reader

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.chimahon.shared.reader.ReaderAddThemeButton
import app.chimahon.shared.reader.ReaderAppearanceBooleanSegmentedControl
import app.chimahon.shared.reader.ReaderAppearanceSectionTitle
import app.chimahon.shared.reader.ReaderAppearanceSlider
import app.chimahon.shared.reader.ReaderAppearanceSwitchRow
import app.chimahon.shared.reader.ReaderCustomThemeDialog
import app.chimahon.shared.reader.ReaderDeleteThemeDialog
import app.chimahon.shared.reader.ReaderRenameThemeDialog
import app.chimahon.shared.reader.ReaderThemeSwatchButton
import com.canopus.chimareader.data.CustomReaderTheme
import com.canopus.chimareader.data.FontManager
import kotlinx.coroutines.launch
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSheet(
    viewModel: ReaderViewModel,
    additionalSettings: @Composable ColumnScope.() -> Unit = {},
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val initialCustomThemeDraft = remember {
        NovelReaderAppearanceSheetPolicy.newCustomThemeDraft(
            backgroundColor = viewModel.customBackgroundColor,
            textColor = viewModel.customTextColor,
        )
    }

    var importedFonts by remember { mutableStateOf(FontManager.getImportedFonts(context)) }
    var isImporting by remember { mutableStateOf(false) }
    var showCustomThemeDialog by remember { mutableStateOf(false) }
    var draftThemeName by remember { mutableStateOf(initialCustomThemeDraft.name) }
    var draftBackgroundColor by remember { mutableIntStateOf(initialCustomThemeDraft.backgroundColor) }
    var draftTextColor by remember { mutableIntStateOf(initialCustomThemeDraft.textColor) }
    var draftBackgroundInput by remember { mutableStateOf(initialCustomThemeDraft.backgroundColorInput) }
    var draftTextInput by remember { mutableStateOf(initialCustomThemeDraft.textColorInput) }
    var renameTarget by remember { mutableStateOf<CustomReaderTheme?>(null) }
    var deleteTarget by remember { mutableStateOf<CustomReaderTheme?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let {
            isImporting = true
            scope.launch {
                val success = FontManager.importFont(context, it)
                when (
                    val action = NovelReaderAppearanceSheetPolicy.fontImportResultAction(
                        success = success,
                        importedFonts = if (success) {
                            FontManager.getImportedFonts(context)
                        } else {
                            importedFonts
                        },
                    )
                ) {
                    NovelReaderAppearanceSheetPolicy.FontImportResultAction.KeepExistingFonts -> Unit
                    is NovelReaderAppearanceSheetPolicy.FontImportResultAction.RefreshFonts -> {
                        importedFonts = action.importedFonts
                    }
                }
                isImporting = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                NovelReaderAppearanceSheetPolicy.TITLE,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp),
            )

            // Theme (moved to top)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val themeSectionState = remember(
                    viewModel.customThemes,
                    viewModel.theme,
                    viewModel.customBackgroundColor,
                    viewModel.customTextColor,
                    viewModel.systemLightSepia,
                ) {
                    NovelReaderAppearanceSheetPolicy.themeSectionState(
                        theme = viewModel.theme,
                        customThemes = viewModel.customThemes,
                        customBackgroundColor = viewModel.customBackgroundColor,
                        customTextColor = viewModel.customTextColor,
                        systemLightSepia = viewModel.systemLightSepia,
                    )
                }
                ReaderAppearanceSectionTitle(themeSectionState.title)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    themeSectionState.fixedOptions.forEach { option ->
                        ReaderThemeSwatchButton(
                            label = option.label,
                            backgroundColor = option.backgroundColor,
                            textColor = option.textColor,
                            splitBackgroundColor = option.splitBackgroundColor,
                            selected = viewModel.theme == option.theme,
                            onClick = { viewModel.updateTheme(option.theme) },
                        )
                    }
                    themeSectionState.customChoices.forEach { choice ->
                        val customTheme = choice.theme
                        ReaderThemeSwatchButton(
                            label = choice.label,
                            backgroundColor = customTheme.backgroundColor,
                            textColor = customTheme.textColor,
                            selected = choice.selected,
                            onClick = { viewModel.applyCustomTheme(customTheme) },
                            onRenameClick = {
                                renameTarget = customTheme
                                renameInput = customTheme.name
                            },
                            onDeleteClick = { deleteTarget = customTheme },
                        )
                    }
                    ReaderAddThemeButton(
                        onClick = {
                            val draft = NovelReaderAppearanceSheetPolicy.newCustomThemeDraft(
                                backgroundColor = viewModel.customBackgroundColor,
                                textColor = viewModel.customTextColor,
                            )
                            draftThemeName = draft.name
                            draftBackgroundColor = draft.backgroundColor
                            draftTextColor = draft.textColor
                            draftBackgroundInput = draft.backgroundColorInput
                            draftTextInput = draft.textColorInput
                            showCustomThemeDialog = true
                        },
                    )
                }

                val systemLightSepiaSwitchState = themeSectionState.systemLightSepia
                if (systemLightSepiaSwitchState.visible) {
                    ReaderAppearanceSwitchRow(
                        state = systemLightSepiaSwitchState.switchState,
                        onCheckedChange = { viewModel.updateSystemLightSepia(it) },
                    )
                }
            }

            // Layout Mode
            ReaderAppearanceBooleanSegmentedControl(
                state = NovelReaderAppearanceSheetPolicy.readerModeControlState(
                    continuousMode = viewModel.continuousMode,
                ),
                onOptionSelected = { viewModel.updateContinuousMode(it) },
                labelStyle = MaterialTheme.typography.labelLarge,
                labelColor = MaterialTheme.colorScheme.primary,
            )

            // Typography
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val typographySectionState = remember(
                    viewModel.selectedFont,
                    importedFonts,
                    isImporting,
                    viewModel.fontSize,
                    viewModel.lineHeight,
                    viewModel.hideFurigana,
                    viewModel.keepScreenOn,
                ) {
                    NovelReaderAppearanceSheetPolicy.typographySectionState(
                        selectedFont = viewModel.selectedFont,
                        defaultFonts = FontManager.defaultFonts,
                        importedFonts = importedFonts,
                        isImporting = isImporting,
                        fontSize = viewModel.fontSize,
                        lineHeight = viewModel.lineHeight,
                        hideFurigana = viewModel.hideFurigana,
                        keepScreenOn = viewModel.keepScreenOn,
                    )
                }
                ReaderAppearanceSectionTitle(typographySectionState.title)

                // Font Family
                val fontDropdownState = typographySectionState.fontDropdown
                var fontExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = fontExpanded,
                    onExpandedChange = { fontExpanded = it },
                ) {
                    OutlinedTextField(
                        value = fontDropdownState.selectedFont,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(fontDropdownState.label) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = fontExpanded,
                        onDismissRequest = { fontExpanded = false },
                    ) {
                        fontDropdownState.choices.forEach { font ->
                            DropdownMenuItem(
                                text = { Text(font) },
                                onClick = {
                                    viewModel.updateSelectedFont(font)
                                    fontExpanded = false
                                },
                            )
                        }
                    }
                }

                // Import Font Button
                val fontImportButtonState = typographySectionState.fontImportButton
                val fontDeleteButtonState = typographySectionState.fontDeleteButton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            fontPickerLauncher.launch(
                                fontImportButtonState.mimeTypes.toTypedArray(),
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = fontImportButtonState.enabled,
                    ) {
                        if (fontImportButtonState.showProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(fontImportButtonState.buttonText)
                        }
                    }

                    // Delete imported font button
                    if (fontDeleteButtonState.visible) {
                        OutlinedButton(
                            onClick = {
                                NovelReaderAppearanceSheetPolicy.fontDeleteAction(
                                    selectedFont = viewModel.selectedFont,
                                    importedFonts = importedFonts,
                                    defaultFonts = FontManager.defaultFonts,
                                )?.let { action ->
                                    FontManager.deleteFont(context, action.fontName)
                                    importedFonts = FontManager.getImportedFonts(context)
                                    viewModel.updateSelectedFont(action.fallbackFont)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text(fontDeleteButtonState.buttonText)
                        }
                    }
                }

                // Font Size
                ReaderAppearanceSlider(
                    state = typographySectionState.fontSizeSlider,
                    onValueChange = {
                        viewModel.updateFontSize(NovelReaderAppearanceSheetPolicy.snapHalf(it))
                    },
                )

                // Line Height
                ReaderAppearanceSlider(
                    state = typographySectionState.lineHeightSlider,
                    onValueChange = {
                        viewModel.updateLineHeight(NovelReaderAppearanceSheetPolicy.snapTwentieth(it))
                    },
                )

                // Hide Furigana
                ReaderAppearanceSwitchRow(
                    state = typographySectionState.hideFuriganaSwitch,
                    onCheckedChange = { viewModel.updateHideFurigana(it) },
                )

                // Keep screen on
                ReaderAppearanceSwitchRow(
                    state = typographySectionState.keepScreenOnSwitch,
                    onCheckedChange = { viewModel.updateKeepScreenOn(it) },
                )
            }

            // Margins
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val marginsSectionState = NovelReaderAppearanceSheetPolicy.marginsSectionState(
                    horizontalPadding = viewModel.horizontalPadding,
                    verticalPadding = viewModel.verticalPadding,
                )
                ReaderAppearanceSectionTitle(marginsSectionState.title)

                ReaderAppearanceSlider(
                    state = marginsSectionState.horizontalPaddingSlider,
                    onValueChange = {
                        viewModel.updateHorizontalPadding(NovelReaderAppearanceSheetPolicy.snapHalf(it))
                    },
                )

                ReaderAppearanceSlider(
                    state = marginsSectionState.verticalPaddingSlider,
                    onValueChange = {
                        viewModel.updateVerticalPadding(NovelReaderAppearanceSheetPolicy.snapHalf(it))
                    },
                )
            }

            // Layout Settings
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val layoutSectionState = NovelReaderAppearanceSheetPolicy.layoutSectionState(
                    verticalWriting = viewModel.verticalWriting,
                    tapZonePercent = viewModel.tapZonePercent,
                    layoutAdvanced = viewModel.layoutAdvanced,
                    avoidPageBreak = viewModel.avoidPageBreak,
                    justifyText = viewModel.justifyText,
                    characterSpacing = viewModel.characterSpacing,
                    paragraphSpacing = viewModel.paragraphSpacing,
                )
                ReaderAppearanceSectionTitle(layoutSectionState.title)

                // Writing Mode
                ReaderAppearanceBooleanSegmentedControl(
                    state = layoutSectionState.writingMode,
                    onOptionSelected = { viewModel.updateVerticalWriting(it) },
                )

                // Tap Zone Size
                ReaderAppearanceSlider(
                    state = layoutSectionState.tapZoneSlider,
                    onValueChange = {
                        viewModel.updateTapZonePercent(NovelReaderAppearanceSheetPolicy.snapWhole(it))
                    },
                )

                // Advanced Header
                val advancedToggleState = layoutSectionState.advancedToggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateLayoutAdvanced(advancedToggleState.nextExpanded) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        layoutSectionState.advancedLabel,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Icon(
                        imageVector = when (advancedToggleState.icon) {
                            NovelReaderAppearanceSheetPolicy.AdvancedToggleIcon.COLLAPSE -> {
                                Icons.Default.KeyboardArrowUp
                            }
                            NovelReaderAppearanceSheetPolicy.AdvancedToggleIcon.EXPAND -> {
                                Icons.Default.KeyboardArrowDown
                            }
                        },
                        contentDescription = advancedToggleState.contentDescription,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                // Advanced settings - only visible when enabled
                if (advancedToggleState.expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Avoid Page Break
                        ReaderAppearanceSwitchRow(
                            state = layoutSectionState.avoidPageBreakSwitch,
                            onCheckedChange = { viewModel.updateAvoidPageBreak(it) },
                            compact = true,
                        )

                        // Justify Text
                        ReaderAppearanceSwitchRow(
                            state = layoutSectionState.justifyTextSwitch,
                            onCheckedChange = { viewModel.updateJustifyText(it) },
                            compact = true,
                        )

                        // Character Spacing
                        ReaderAppearanceSlider(
                            state = layoutSectionState.characterSpacingSlider,
                            onValueChange = {
                                viewModel.updateCharacterSpacing(
                                    NovelReaderAppearanceSheetPolicy.snapTwentieth(it),
                                )
                            },
                            compact = true,
                        )

                        // Paragraph Spacing
                        ReaderAppearanceSlider(
                            state = layoutSectionState.paragraphSpacingSlider,
                            onValueChange = {
                                viewModel.updateParagraphSpacing(
                                    NovelReaderAppearanceSheetPolicy.snapTwentieth(it),
                                )
                            },
                            compact = true,
                        )
                    }
                }
            }

            // Additional app-side settings (like volume buttons)
            additionalSettings()
        }
    }

    if (showCustomThemeDialog) {
        ReaderCustomThemeDialog(
            themeName = draftThemeName,
            backgroundColor = draftBackgroundColor,
            textColor = draftTextColor,
            backgroundColorInput = draftBackgroundInput,
            textColorInput = draftTextInput,
            onThemeNameChange = { draftThemeName = it },
            onBackgroundColorInputChange = { input ->
                val update = NovelReaderAppearanceSheetPolicy.customThemeColorInputUpdate(
                    currentColor = draftBackgroundColor,
                    input = input,
                )
                draftBackgroundInput = update.input
                draftBackgroundColor = update.color
            },
            onTextColorInputChange = { input ->
                val update = NovelReaderAppearanceSheetPolicy.customThemeColorInputUpdate(
                    currentColor = draftTextColor,
                    input = input,
                )
                draftTextInput = update.input
                draftTextColor = update.color
            },
            onDismiss = { showCustomThemeDialog = false },
            onConfirm = {
                viewModel.addCustomTheme(
                    NovelReaderAppearanceSheetPolicy.customThemeSaveAction(
                        themeName = draftThemeName,
                        backgroundColor = draftBackgroundColor,
                        textColor = draftTextColor,
                    ),
                )
                showCustomThemeDialog = false
            },
        )
    }

    renameTarget?.let { target ->
        ReaderRenameThemeDialog(
            renameInput = renameInput,
            onRenameInputChange = { renameInput = it },
            onDismiss = { renameTarget = null },
            onConfirm = {
                viewModel.renameCustomTheme(target, renameInput)
                renameTarget = null
            },
        )
    }

    deleteTarget?.let { target ->
        ReaderDeleteThemeDialog(
            theme = target,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.deleteCustomTheme(target)
                deleteTarget = null
            },
        )
    }
}
