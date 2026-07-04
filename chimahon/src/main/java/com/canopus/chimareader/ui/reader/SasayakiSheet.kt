package com.canopus.chimareader.ui.reader

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import tachiyomi.domain.reader.service.NovelReaderSasayakiSheetPolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SasayakiSheet(
    viewModel: ReaderViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val context = LocalContext.current
    val sheetContent = NovelReaderSasayakiSheetPolicy.sheetState(
        hasAudio = viewModel.sasayakiPlayer?.hasAudio == true,
        isPlaying = viewModel.sasayakiPlayer?.isPlaying == true,
    )

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        when (
            NovelReaderSasayakiSheetPolicy.audioImportSelectionAction(
                uriSelected = uri != null,
            )
        ) {
            NovelReaderSasayakiSheetPolicy.AudioImportSelectionAction.Ignore -> {
                return@rememberLauncherForActivityResult
            }
            NovelReaderSasayakiSheetPolicy.AudioImportSelectionAction.ImportSelectedAudio -> Unit
        }

        // In a real app we'd copy this safely into BookStorage
        val tempFile = File(context.cacheDir, NovelReaderSasayakiSheetPolicy.IMPORTED_AUDIO_FILE_NAME)
        context.contentResolver.openInputStream(checkNotNull(uri))?.use { input ->
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
        }
        viewModel.sasayakiPlayer?.importAudio(tempFile)
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                sheetContent.title,
                style = MaterialTheme.typography.headlineSmall,
            )

            if (sheetContent.showAudioControls) {
                val playbackButton = sheetContent.playbackButton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(sheetContent.audioSectionTitle, style = MaterialTheme.typography.titleMedium)
                    if (playbackButton != null) {
                        IconButton(onClick = { viewModel.sasayakiPlayer?.togglePlayback() }) {
                            Icon(
                                playbackButton.icon.toImageVector(),
                                contentDescription = playbackButton.contentDescription,
                            )
                        }
                    }
                }
            } else {
                Text(
                    sheetContent.emptyMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = {
                    audioPicker.launch(NovelReaderSasayakiSheetPolicy.AUDIO_IMPORT_MIME_TYPE)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(sheetContent.importButtonText)
            }
        }
    }
}

private fun NovelReaderSasayakiSheetPolicy.PlaybackIcon.toImageVector() = when (this) {
    NovelReaderSasayakiSheetPolicy.PlaybackIcon.PLAY -> Icons.Default.PlayArrow
    NovelReaderSasayakiSheetPolicy.PlaybackIcon.PAUSE -> Icons.Default.Pause
}
