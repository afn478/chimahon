package tachiyomi.domain.reader.service

object NovelReaderSasayakiSheetPolicy {
    const val IMPORTED_AUDIO_FILE_NAME = "sasayaki_imported.m4a"

    data class SheetState(
        val title: String,
        val showAudioControls: Boolean,
        val audioSectionTitle: String,
        val emptyMessage: String?,
        val importButtonText: String,
        val playbackButtonContentDescription: String,
    )

    fun sheetState(hasAudio: Boolean): SheetState {
        return SheetState(
            title = "Sasayaki",
            showAudioControls = hasAudio,
            audioSectionTitle = "Audio Synchronization",
            emptyMessage = if (hasAudio) null else "No audio file matched or imported.",
            importButtonText = if (hasAudio) "Replace Audio File" else "Import Audio File",
            playbackButtonContentDescription = "Toggle Playback",
        )
    }
}
