package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals

class NovelReaderSasayakiSheetPolicyTest {
    @Test
    fun noAudioStateShowsImportPrompt() {
        assertEquals(
            NovelReaderSasayakiSheetPolicy.SheetState(
                title = "Sasayaki",
                showAudioControls = false,
                audioSectionTitle = "Audio Synchronization",
                emptyMessage = "No audio file matched or imported.",
                importButtonText = "Import Audio File",
                playbackButton = null,
            ),
            NovelReaderSasayakiSheetPolicy.sheetState(hasAudio = false),
        )
    }

    @Test
    fun pausedAudioStateShowsControlsAndResumeAction() {
        assertEquals(
            NovelReaderSasayakiSheetPolicy.SheetState(
                title = "Sasayaki",
                showAudioControls = true,
                audioSectionTitle = "Audio Synchronization",
                emptyMessage = null,
                importButtonText = "Replace Audio File",
                playbackButton = NovelReaderSasayakiSheetPolicy.PlaybackButtonState(
                    icon = NovelReaderSasayakiSheetPolicy.PlaybackIcon.PLAY,
                    contentDescription = "Resume Playback",
                ),
            ),
            NovelReaderSasayakiSheetPolicy.sheetState(
                hasAudio = true,
                isPlaying = false,
            ),
        )
    }

    @Test
    fun playingAudioStateShowsPauseAction() {
        assertEquals(
            NovelReaderSasayakiSheetPolicy.PlaybackButtonState(
                icon = NovelReaderSasayakiSheetPolicy.PlaybackIcon.PAUSE,
                contentDescription = "Pause Playback",
            ),
            NovelReaderSasayakiSheetPolicy.sheetState(
                hasAudio = true,
                isPlaying = true,
            ).playbackButton,
        )
    }

    @Test
    fun audioImportConstantsRemainStable() {
        assertEquals(
            "sasayaki_imported.m4a",
            NovelReaderSasayakiSheetPolicy.IMPORTED_AUDIO_FILE_NAME,
        )
        assertEquals(
            "audio/*",
            NovelReaderSasayakiSheetPolicy.AUDIO_IMPORT_MIME_TYPE,
        )
    }

    @Test
    fun audioImportSelectionActionIgnoresMissingSelections() {
        assertEquals(
            NovelReaderSasayakiSheetPolicy.AudioImportSelectionAction.ImportSelectedAudio,
            NovelReaderSasayakiSheetPolicy.audioImportSelectionAction(uriSelected = true),
        )
        assertEquals(
            NovelReaderSasayakiSheetPolicy.AudioImportSelectionAction.Ignore,
            NovelReaderSasayakiSheetPolicy.audioImportSelectionAction(uriSelected = false),
        )
    }
}
