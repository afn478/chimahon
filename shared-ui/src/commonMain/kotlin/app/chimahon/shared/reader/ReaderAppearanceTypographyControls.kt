package app.chimahon.shared.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.FontDeleteButtonState
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.FontDropdownState
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.FontImportButtonState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderFontDropdown(
    state: FontDropdownState,
    onFontSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = state.selectedFont,
            onValueChange = {},
            readOnly = true,
            label = { Text(state.label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            state.choices.forEach { font ->
                DropdownMenuItem(
                    text = { Text(font) },
                    onClick = {
                        onFontSelected(font)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun ReaderFontActionRow(
    importState: FontImportButtonState,
    deleteState: FontDeleteButtonState,
    onImportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onImportClick,
            modifier = Modifier.weight(1f),
            enabled = importState.enabled,
        ) {
            if (importState.showProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(importState.buttonText)
            }
        }

        if (deleteState.visible) {
            OutlinedButton(
                onClick = onDeleteClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(deleteState.buttonText)
            }
        }
    }
}
