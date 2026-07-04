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
                playbackButtonContentDescription = "Toggle Playback",
            ),
            NovelReaderSasayakiSheetPolicy.sheetState(hasAudio = false),
        )
    }

    @Test
    fun audioStateShowsControlsAndReplacementAction() {
        assertEquals(
            NovelReaderSasayakiSheetPolicy.SheetState(
                title = "Sasayaki",
                showAudioControls = true,
                audioSectionTitle = "Audio Synchronization",
                emptyMessage = null,
                importButtonText = "Replace Audio File",
                playbackButtonContentDescription = "Toggle Playback",
            ),
            NovelReaderSasayakiSheetPolicy.sheetState(hasAudio = true),
        )
    }

    @Test
    fun importedAudioFileNameRemainsStable() {
        assertEquals(
            "sasayaki_imported.m4a",
            NovelReaderSasayakiSheetPolicy.IMPORTED_AUDIO_FILE_NAME,
        )
    }
}
