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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.canopus.chimareader.data.CustomReaderTheme
import com.canopus.chimareader.data.FontManager
import com.canopus.chimareader.data.Theme
import kotlinx.coroutines.launch
import tachiyomi.domain.reader.service.NovelReaderAppearancePolicy
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

    var importedFonts by remember { mutableStateOf(FontManager.getImportedFonts(context)) }
    val allFonts = remember(importedFonts) { FontManager.defaultFonts + importedFonts }

    var isImporting by remember { mutableStateOf(false) }
    var showCustomThemeDialog by remember { mutableStateOf(false) }
    var draftThemeName by remember { mutableStateOf("") }
    var draftBackgroundColor by remember { mutableIntStateOf(viewModel.customBackgroundColor) }
    var draftTextColor by remember { mutableIntStateOf(viewModel.customTextColor) }
    var draftBackgroundInput by remember {
        mutableStateOf(NovelReaderAppearancePolicy.colorHex(viewModel.customBackgroundColor))
    }
    var draftTextInput by remember {
        mutableStateOf(NovelReaderAppearancePolicy.colorHex(viewModel.customTextColor))
    }
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
                if (success) {
                    importedFonts = FontManager.getImportedFonts(context)
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
                val customThemeChoices = remember(
                    viewModel.customThemes,
                    viewModel.theme,
                    viewModel.customBackgroundColor,
                    viewModel.customTextColor,
                ) {
                    NovelReaderAppearanceSheetPolicy.customThemeChoices(
                        theme = viewModel.theme,
                        customThemes = viewModel.customThemes,
                        customBackgroundColor = viewModel.customBackgroundColor,
                        customTextColor = viewModel.customTextColor,
                    )
                }
                Text(
                    NovelReaderAppearanceSheetPolicy.THEME_SECTION_TITLE,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NovelReaderAppearanceSheetPolicy.fixedThemeOptions.forEach { option ->
                        ReaderThemeSwatchButton(
                            label = option.label,
                            backgroundColor = option.backgroundColor,
                            textColor = option.textColor,
                            splitBackgroundColor = option.splitBackgroundColor,
                            selected = viewModel.theme == option.theme,
                            onClick = { viewModel.updateTheme(option.theme) },
                        )
                    }
                    customThemeChoices.forEach { choice ->
                        val customTheme = choice.theme
                        ReaderThemeSwatchButton(
                            label = choice.label,
                            backgroundColor = customTheme.backgroundColor,
                            textColor = customTheme.textColor,
                            selected = choice.selected,
                            onClick = { viewModel.applyCustomTheme(customTheme) },
                            onLongClick = {
                                renameTarget = customTheme
                                renameInput = customTheme.name
                            },
                            onDeleteClick = { deleteTarget = customTheme },
                        )
                    }
                    AddThemeButton(
                        onClick = {
                            draftThemeName = ""
                            draftBackgroundColor = viewModel.customBackgroundColor
                            draftTextColor = viewModel.customTextColor
                            draftBackgroundInput = NovelReaderAppearancePolicy.colorHex(
                                viewModel.customBackgroundColor,
                            )
                            draftTextInput = NovelReaderAppearancePolicy.colorHex(
                                viewModel.customTextColor,
                            )
                            showCustomThemeDialog = true
                        },
                    )
                }

                if (viewModel.theme == Theme.SYSTEM) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Text(
                            NovelReaderAppearanceSheetPolicy.SYSTEM_LIGHT_SEPIA_LABEL,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(
                            checked = viewModel.systemLightSepia,
                            onCheckedChange = { viewModel.updateSystemLightSepia(it) },
                        )
                    }
                }
            }

            // Layout Mode
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    NovelReaderAppearanceSheetPolicy.MODE_SECTION_TITLE,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !viewModel.continuousMode,
                        onClick = { viewModel.updateContinuousMode(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) {
                        Text(NovelReaderAppearanceSheetPolicy.PAGINATED_MODE_LABEL)
                    }
                    SegmentedButton(
                        selected = viewModel.continuousMode,
                        onClick = { viewModel.updateContinuousMode(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) {
                        Text(NovelReaderAppearanceSheetPolicy.CONTINUOUS_MODE_LABEL)
                    }
                }
            }

            // Typography
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    NovelReaderAppearanceSheetPolicy.TYPOGRAPHY_SECTION_TITLE,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )

                // Font Family
                var fontExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = fontExpanded,
                    onExpandedChange = { fontExpanded = it },
                ) {
                    OutlinedTextField(
                        value = viewModel.selectedFont,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(NovelReaderAppearanceSheetPolicy.FONT_FAMILY_LABEL) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = fontExpanded,
                        onDismissRequest = { fontExpanded = false },
                    ) {
                        allFonts.forEach { font ->
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            fontPickerLauncher.launch(
                                arrayOf(
                                    "application/font-ttf",
                                    "application/x-font-ttf",
                                    "font/ttf",
                                    "application/octet-stream",
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isImporting,
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(NovelReaderAppearanceSheetPolicy.IMPORT_FONT_BUTTON_TEXT)
                        }
                    }

                    // Delete imported font button
                    if (importedFonts.contains(viewModel.selectedFont)) {
                        OutlinedButton(
                            onClick = {
                                FontManager.deleteFont(context, viewModel.selectedFont)
                                importedFonts = FontManager.getImportedFonts(context)
                                viewModel.updateSelectedFont(FontManager.defaultFonts.first())
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text(NovelReaderAppearanceSheetPolicy.DELETE_FONT_BUTTON_TEXT)
                        }
                    }
                }

                // Font Size
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            NovelReaderAppearanceSheetPolicy.FONT_SIZE_LABEL,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            NovelReaderAppearanceSheetPolicy.fontSizeLabel(viewModel.fontSize),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Slider(
                        value = viewModel.fontSize.toFloat(),
                        onValueChange = {
                            viewModel.updateFontSize(NovelReaderAppearanceSheetPolicy.snapHalf(it.toDouble()))
                        },
                        valueRange = 12f..72f,
                        steps = 119,
                    )
                }

                // Line Height
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            NovelReaderAppearanceSheetPolicy.LINE_HEIGHT_LABEL,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            NovelReaderAppearanceSheetPolicy.lineHeightLabel(viewModel.lineHeight),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Slider(
                        value = viewModel.lineHeight.toFloat(),
                        onValueChange = {
                            viewModel.updateLineHeight(NovelReaderAppearanceSheetPolicy.snapTwentieth(it.toDouble()))
                        },
                        valueRange = 1.0f..2.5f,
                        steps = 29,
                    )
                }

                // Hide Furigana
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        NovelReaderAppearanceSheetPolicy.HIDE_FURIGANA_LABEL,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = viewModel.hideFurigana,
                        onCheckedChange = { viewModel.updateHideFurigana(it) },
                    )
                }

                // Keep screen on
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        NovelReaderAppearanceSheetPolicy.KEEP_SCREEN_ON_LABEL,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = viewModel.keepScreenOn,
                        onCheckedChange = { viewModel.updateKeepScreenOn(it) },
                    )
                }
            }

            // Margins
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    NovelReaderAppearanceSheetPolicy.MARGINS_SECTION_TITLE,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            NovelReaderAppearanceSheetPolicy.HORIZONTAL_PADDING_LABEL,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            NovelReaderAppearanceSheetPolicy.paddingPercentLabel(viewModel.horizontalPadding),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Slider(
                        value = viewModel.horizontalPadding.toFloat(),
                        onValueChange = {
                            viewModel.updateHorizontalPadding(NovelReaderAppearanceSheetPolicy.snapHalf(it.toDouble()))
                        },
                        valueRange = 0f..50f,
                        steps = 99,
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            NovelReaderAppearanceSheetPolicy.VERTICAL_PADDING_LABEL,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            NovelReaderAppearanceSheetPolicy.paddingPercentLabel(viewModel.verticalPadding),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Slider(
                        value = viewModel.verticalPadding.toFloat(),
                        onValueChange = {
                            viewModel.updateVerticalPadding(NovelReaderAppearanceSheetPolicy.snapHalf(it.toDouble()))
                        },
                        valueRange = 0f..50f,
                        steps = 99,
                    )
                }
            }

            // Layout Settings
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    NovelReaderAppearanceSheetPolicy.LAYOUT_SECTION_TITLE,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )

                // Writing Mode
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        NovelReaderAppearanceSheetPolicy.WRITING_MODE_LABEL,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = viewModel.verticalWriting,
                            onClick = { viewModel.updateVerticalWriting(true) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) {
                            Text(NovelReaderAppearanceSheetPolicy.VERTICAL_WRITING_LABEL)
                        }
                        SegmentedButton(
                            selected = !viewModel.verticalWriting,
                            onClick = { viewModel.updateVerticalWriting(false) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) {
                            Text(NovelReaderAppearanceSheetPolicy.HORIZONTAL_WRITING_LABEL)
                        }
                    }
                }

                // Tap Zone Size
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            NovelReaderAppearanceSheetPolicy.TAP_ZONE_SIZE_LABEL,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            NovelReaderAppearanceSheetPolicy.tapZonePercentLabel(viewModel.tapZonePercent),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Slider(
                        value = viewModel.tapZonePercent.toFloat(),
                        onValueChange = {
                            viewModel.updateTapZonePercent(NovelReaderAppearanceSheetPolicy.snapWhole(it.toDouble()))
                        },
                        valueRange = 0f..40f,
                        steps = 39,
                    )
                }

                // Advanced Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateLayoutAdvanced(!viewModel.layoutAdvanced) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        NovelReaderAppearanceSheetPolicy.ADVANCED_LABEL,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Icon(
                        imageVector = if (viewModel.layoutAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (viewModel.layoutAdvanced) {
                            NovelReaderAppearanceSheetPolicy.COLLAPSE_CONTENT_DESCRIPTION
                        } else {
                            NovelReaderAppearanceSheetPolicy.EXPAND_CONTENT_DESCRIPTION
                        },
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                // Advanced settings - only visible when enabled
                if (viewModel.layoutAdvanced) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Avoid Page Break
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Text(
                                NovelReaderAppearanceSheetPolicy.AVOID_PAGE_BREAK_LABEL,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Switch(
                                checked = viewModel.avoidPageBreak,
                                onCheckedChange = { viewModel.updateAvoidPageBreak(it) },
                                modifier = Modifier.scale(0.85f),
                            )
                        }

                        // Justify Text
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Text(
                                NovelReaderAppearanceSheetPolicy.JUSTIFY_TEXT_LABEL,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Switch(
                                checked = viewModel.justifyText,
                                onCheckedChange = { viewModel.updateJustifyText(it) },
                                modifier = Modifier.scale(0.85f),
                            )
                        }

                        // Character Spacing
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    NovelReaderAppearanceSheetPolicy.CHARACTER_SPACING_LABEL,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    NovelReaderAppearanceSheetPolicy.characterSpacingLabel(viewModel.characterSpacing),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Slider(
                                value = viewModel.characterSpacing.toFloat(),
                                onValueChange = {
                                    viewModel.updateCharacterSpacing(NovelReaderAppearanceSheetPolicy.snapTwentieth(it.toDouble()))
                                },
                                valueRange = 0f..0.5f,
                                steps = 9,
                            )
                        }

                        // Paragraph Spacing
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    NovelReaderAppearanceSheetPolicy.PARAGRAPH_SPACING_LABEL,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    NovelReaderAppearanceSheetPolicy.paragraphSpacingLabel(viewModel.paragraphSpacing),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Slider(
                                value = viewModel.paragraphSpacing.toFloat(),
                                onValueChange = {
                                    viewModel.updateParagraphSpacing(NovelReaderAppearanceSheetPolicy.snapTwentieth(it.toDouble()))
                                },
                                valueRange = 0f..2f,
                                steps = 39,
                            )
                        }
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
                draftBackgroundInput = input
                NovelReaderAppearancePolicy.parseColorInput(input)?.let { draftBackgroundColor = it }
            },
            onTextColorInputChange = { input ->
                draftTextInput = input
                NovelReaderAppearancePolicy.parseColorInput(input)?.let { draftTextColor = it }
            },
            onDismiss = { showCustomThemeDialog = false },
            onConfirm = {
                viewModel.addCustomTheme(
                    CustomReaderTheme(
                        name = draftThemeName,
                        backgroundColor = draftBackgroundColor,
                        textColor = draftTextColor,
                    ),
                )
                showCustomThemeDialog = false
            },
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(NovelReaderAppearanceSheetPolicy.RENAME_THEME_TITLE) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(NovelReaderAppearanceSheetPolicy.THEME_NAME_LABEL) },
                    placeholder = { Text(NovelReaderAppearanceSheetPolicy.THEME_NAME_PLACEHOLDER) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameCustomTheme(target, renameInput)
                        renameTarget = null
                    },
                    enabled = renameInput.isNotBlank(),
                ) { Text(NovelReaderAppearanceSheetPolicy.RENAME_BUTTON_TEXT) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(NovelReaderAppearanceSheetPolicy.CANCEL_BUTTON_TEXT)
                }
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(NovelReaderAppearanceSheetPolicy.DELETE_THEME_TITLE) },
            text = { Text(NovelReaderAppearanceSheetPolicy.deleteThemeMessage(target)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCustomTheme(target)
                        deleteTarget = null
                    },
                ) {
                    Text(
                        NovelReaderAppearanceSheetPolicy.DELETE_BUTTON_TEXT,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(NovelReaderAppearanceSheetPolicy.CANCEL_BUTTON_TEXT)
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
    onLongClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    splitBackgroundColor: Int? = null,
) {
    val haptic = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .width(84.dp)
            .height(64.dp)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = if (onLongClick != null) {
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
            DropdownMenuItem(
                text = { Text(NovelReaderAppearanceSheetPolicy.RENAME_MENU_TEXT) },
                onClick = {
                    showMenu = false
                    onLongClick?.invoke()
                },
            )
            if (onDeleteClick != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            NovelReaderAppearanceSheetPolicy.DELETE_MENU_TEXT,
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        showMenu = false
                        onDeleteClick()
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
    label: String,
    parsedColor: Int?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            1.dp,
            if (parsedColor == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
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
                    .background(Color(parsedColor ?: 0x00000000), RoundedCornerShape(5.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(5.dp)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = parsedColor?.let(NovelReaderAppearancePolicy::colorHex)
                        ?: NovelReaderAppearanceSheetPolicy.INVALID_COLOR_LABEL,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (parsedColor == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
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
                        label = dialogState.background.reviewLabel,
                        parsedColor = dialogState.background.parsedColor,
                        modifier = Modifier.weight(1f),
                    )
                    ColorReviewChip(
                        label = dialogState.text.reviewLabel,
                        parsedColor = dialogState.text.parsedColor,
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
