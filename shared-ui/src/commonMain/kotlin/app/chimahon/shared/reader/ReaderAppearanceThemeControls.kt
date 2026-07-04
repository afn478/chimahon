package app.chimahon.shared.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.ColorReviewState

@Composable
fun ReaderThemeSwatchButton(
    label: String,
    backgroundColor: Int,
    textColor: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier
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
fun ReaderAddThemeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
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
fun ReaderColorReviewChip(
    state: ColorReviewState,
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
