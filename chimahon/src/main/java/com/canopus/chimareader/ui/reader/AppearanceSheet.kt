package com.canopus.chimareader.ui.reader

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.chimahon.shared.reader.ReaderAppearanceBooleanSegmentedControl
import app.chimahon.shared.reader.ReaderAppearanceSectionTitle
import app.chimahon.shared.reader.ReaderAppearanceSlider
import app.chimahon.shared.reader.ReaderAppearanceSwitchRow
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
                    AddThemeButton(
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
        CustomThemeDialog(
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
        val dialogState = NovelReaderAppearanceSheetPolicy.renameThemeDialogState(renameInput)
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(dialogState.title) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(dialogState.nameLabel) },
                    placeholder = { Text(dialogState.namePlaceholder) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameCustomTheme(target, renameInput)
                        renameTarget = null
                    },
                    enabled = dialogState.confirmEnabled,
                ) { Text(dialogState.confirmButtonText) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(dialogState.dismissButtonText)
                }
            },
        )
    }

    deleteTarget?.let { target ->
        val dialogState = NovelReaderAppearanceSheetPolicy.deleteThemeDialogState(target)
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(dialogState.title) },
            text = { Text(dialogState.message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCustomTheme(target)
                        deleteTarget = null
                    },
                ) {
                    Text(
                        dialogState.confirmButtonText,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(dialogState.dismissButtonText)
                }
            },
        )
    }
}

@Composable
private fun ReaderThemeSwatchButton(
    label: String,
    backgroundColor: Int,
    textColor: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onRenameClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    splitBackgroundColor: Int? = null,
) {
    val haptic = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }
    val menuState = NovelReaderAppearanceSheetPolicy.themeSwatchMenuState(
        canRename = onRenameClick != null,
        canDelete = onDeleteClick != null,
    )
    Surface(
        modifier = Modifier
            .width(84.dp)
            .height(64.dp)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = if (menuState.enabled) {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                } else null,
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            if (menuState.showRename) {
                DropdownMenuItem(
                    text = { Text(menuState.renameMenuText) },
                    onClick = {
                        showMenu = false
                        onRenameClick?.invoke()
                    },
                )
            }
            if (menuState.showDelete) {
                DropdownMenuItem(
                    text = {
                        Text(
                            menuState.deleteMenuText,
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        showMenu = false
                        onDeleteClick?.invoke()
                    },
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(backgroundColor))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)),
            ) {
                if (splitBackgroundColor != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .fillMaxWidth(0.48f)
                            .background(Color(splitBackgroundColor)),
                    )
                }
                Text(
                    text = NovelReaderAppearanceSheetPolicy.CUSTOM_THEME_PREVIEW_TEXT,
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(textColor),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun AddThemeButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(64.dp)
            .height(64.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = NovelReaderAppearanceSheetPolicy.ADD_CUSTOM_THEME_CONTENT_DESCRIPTION,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                NovelReaderAppearanceSheetPolicy.ADD_CUSTOM_THEME_LABEL,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun ColorReviewChip(
    state: NovelReaderAppearanceSheetPolicy.ColorReviewState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            1.dp,
            if (!state.isValid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(state.color), RoundedCornerShape(5.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(5.dp)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = state.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = state.valueText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (!state.isValid) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CustomThemeDialog(
    themeName: String,
    backgroundColor: Int,
    textColor: Int,
    backgroundColorInput: String,
    textColorInput: String,
    onThemeNameChange: (String) -> Unit,
    onBackgroundColorInputChange: (String) -> Unit,
    onTextColorInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val dialogState = NovelReaderAppearanceSheetPolicy.customThemeDialogState(
        themeName = themeName,
        backgroundColor = backgroundColor,
        textColor = textColor,
        backgroundColorInput = backgroundColorInput,
        textColorInput = textColorInput,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogState.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(dialogState.background.previewColor),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = dialogState.sampleThemeName,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(dialogState.text.previewColor),
                        )
                        Text(
                            text = dialogState.sampleText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(dialogState.text.previewColor),
                        )
                    }
                }

                OutlinedTextField(
                    value = themeName,
                    onValueChange = onThemeNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(dialogState.nameLabel) },
                    placeholder = { Text(dialogState.namePlaceholder) },
                )

                OutlinedTextField(
                    value = backgroundColorInput,
                    onValueChange = onBackgroundColorInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(dialogState.background.label) },
                    placeholder = { Text(dialogState.background.placeholder) },
                    isError = !dialogState.background.isValid,
                    supportingText = {
                        if (!dialogState.background.isValid) {
                            Text(dialogState.background.supportingText)
                        }
                    },
                )
                OutlinedTextField(
                    value = textColorInput,
                    onValueChange = onTextColorInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(dialogState.text.label) },
                    placeholder = { Text(dialogState.text.placeholder) },
                    isError = !dialogState.text.isValid,
                    supportingText = {
                        if (!dialogState.text.isValid) {
                            Text(dialogState.text.supportingText)
                        }
                    },
                )

                Text(dialogState.reviewLabel, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ColorReviewChip(
                        state = dialogState.background.review,
                        modifier = Modifier.weight(1f),
                    )
                    ColorReviewChip(
                        state = dialogState.text.review,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = dialogState.confirmEnabled,
            ) {
                Text(dialogState.confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dialogState.dismissButtonText)
            }
        },
    )
}
