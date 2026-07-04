package app.chimahon.shared.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.AdvancedToggleIcon
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.AdvancedToggleState

@Composable
fun ReaderAppearanceAdvancedToggleRow(
    label: String,
    state: AdvancedToggleState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
        )
        Icon(
            imageVector = when (state.icon) {
                AdvancedToggleIcon.COLLAPSE -> Icons.Default.KeyboardArrowUp
                AdvancedToggleIcon.EXPAND -> Icons.Default.KeyboardArrowDown
            },
            contentDescription = state.contentDescription,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
