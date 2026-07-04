package com.canopus.chimareader.ui.reader

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.chimahon.shared.reader.ReaderAppearanceSheetContent
import app.chimahon.shared.reader.ReaderAppearanceSheetContentActions
import app.chimahon.shared.reader.ReaderAppearanceSheetContentState
import app.chimahon.shared.reader.ReaderCustomThemeDialog
import app.chimahon.shared.reader.ReaderDeleteThemeDialog
import app.chimahon.shared.reader.ReaderRenameThemeDialog
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

        val appearanceContentState = ReaderAppearanceSheetContentState(
            theme = themeSectionState,
            selectedTheme = viewModel.theme,
            readerMode = NovelReaderAppearanceSheetPolicy.readerModeControlState(
                continuousMode = viewModel.continuousMode,
            ),
            typography = typographySectionState,
            margins = NovelReaderAppearanceSheetPolicy.marginsSectionState(
                horizontalPadding = viewModel.horizontalPadding,
                verticalPadding = viewModel.verticalPadding,
            ),
            layout = NovelReaderAppearanceSheetPolicy.layoutSectionState(
                verticalWriting = viewModel.verticalWriting,
                tapZonePercent = viewModel.tapZonePercent,
                layoutAdvanced = viewModel.layoutAdvanced,
                avoidPageBreak = viewModel.avoidPageBreak,
                justifyText = viewModel.justifyText,
                characterSpacing = viewModel.characterSpacing,
                paragraphSpacing = viewModel.paragraphSpacing,
            ),
        )

        ReaderAppearanceSheetContent(
            state = appearanceContentState,
            actions = ReaderAppearanceSheetContentActions(
                onThemeSelected = { viewModel.updateTheme(it) },
                onCustomThemeSelected = { viewModel.applyCustomTheme(it) },
                onCustomThemeRename = { customTheme ->
                    renameTarget = customTheme
                    renameInput = customTheme.name
                },
                onCustomThemeDelete = { deleteTarget = it },
                onAddCustomTheme = {
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
                onSystemLightSepiaChange = { viewModel.updateSystemLightSepia(it) },
                onReaderModeChange = { viewModel.updateContinuousMode(it) },
                onFontSelected = { viewModel.updateSelectedFont(it) },
                onImportFontClick = {
                    fontPickerLauncher.launch(
                        typographySectionState.fontImportButton.mimeTypes.toTypedArray(),
                    )
                },
                onDeleteFontClick = {
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
                onFontSizeChange = {
                    viewModel.updateFontSize(NovelReaderAppearanceSheetPolicy.snapHalf(it))
                },
                onLineHeightChange = {
                    viewModel.updateLineHeight(NovelReaderAppearanceSheetPolicy.snapTwentieth(it))
                },
                onHideFuriganaChange = { viewModel.updateHideFurigana(it) },
                onKeepScreenOnChange = { viewModel.updateKeepScreenOn(it) },
                onHorizontalPaddingChange = {
                    viewModel.updateHorizontalPadding(NovelReaderAppearanceSheetPolicy.snapHalf(it))
                },
                onVerticalPaddingChange = {
                    viewModel.updateVerticalPadding(NovelReaderAppearanceSheetPolicy.snapHalf(it))
                },
                onWritingModeChange = { viewModel.updateVerticalWriting(it) },
                onTapZonePercentChange = {
                    viewModel.updateTapZonePercent(NovelReaderAppearanceSheetPolicy.snapWhole(it))
                },
                onAdvancedExpandedChange = { viewModel.updateLayoutAdvanced(it) },
                onAvoidPageBreakChange = { viewModel.updateAvoidPageBreak(it) },
                onJustifyTextChange = { viewModel.updateJustifyText(it) },
                onCharacterSpacingChange = {
                    viewModel.updateCharacterSpacing(NovelReaderAppearanceSheetPolicy.snapTwentieth(it))
                },
                onParagraphSpacingChange = {
                    viewModel.updateParagraphSpacing(NovelReaderAppearanceSheetPolicy.snapTwentieth(it))
                },
            ),
            additionalSettings = additionalSettings,
        )
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
