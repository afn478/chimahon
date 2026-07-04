package app.chimahon.shared.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tachiyomi.domain.reader.model.CustomReaderTheme
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy

@Composable
fun ReaderCustomThemeDialog(
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
                    ReaderColorReviewChip(
                        state = dialogState.background.review,
                        modifier = Modifier.weight(1f),
                    )
                    ReaderColorReviewChip(
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

@Composable
fun ReaderRenameThemeDialog(
    renameInput: String,
    onRenameInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val dialogState = NovelReaderAppearanceSheetPolicy.renameThemeDialogState(renameInput)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogState.title) },
        text = {
            OutlinedTextField(
                value = renameInput,
                onValueChange = onRenameInputChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(dialogState.nameLabel) },
                placeholder = { Text(dialogState.namePlaceholder) },
            )
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

@Composable
fun ReaderDeleteThemeDialog(
    theme: CustomReaderTheme,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val dialogState = NovelReaderAppearanceSheetPolicy.deleteThemeDialogState(theme)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogState.title) },
        text = { Text(dialogState.message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    dialogState.confirmButtonText,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dialogState.dismissButtonText)
            }
        },
    )
}
