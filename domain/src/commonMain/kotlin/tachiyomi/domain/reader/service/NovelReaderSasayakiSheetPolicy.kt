package tachiyomi.domain.reader.service

object NovelReaderSasayakiSheetPolicy {
    const val IMPORTED_AUDIO_FILE_NAME = "sasayaki_imported.m4a"
    const val AUDIO_IMPORT_MIME_TYPE = "audio/*"

    enum class PlaybackIcon {
        PLAY,
        PAUSE,
    }

    data class PlaybackButtonState(
        val icon: PlaybackIcon,
        val contentDescription: String,
    )

    sealed interface AudioImportSelectionAction {
        data object Ignore : AudioImportSelectionAction
        data object ImportSelectedAudio : AudioImportSelectionAction
    }

    data class SheetState(
        val title: String,
        val showAudioControls: Boolean,
        val audioSectionTitle: String,
        val emptyMessage: String?,
        val importButtonText: String,
        val playbackButton: PlaybackButtonState?,
    )

    fun sheetState(
        hasAudio: Boolean,
        isPlaying: Boolean = false,
    ): SheetState {
        return SheetState(
            title = "Sasayaki",
            showAudioControls = hasAudio,
            audioSectionTitle = "Audio Synchronization",
            emptyMessage = if (hasAudio) null else "No audio file matched or imported.",
            importButtonText = if (hasAudio) "Replace Audio File" else "Import Audio File",
            playbackButton = playbackButtonState(
                hasAudio = hasAudio,
                isPlaying = isPlaying,
            ),
        )
    }

    fun audioImportSelectionAction(uriSelected: Boolean): AudioImportSelectionAction {
        return if (uriSelected) {
            AudioImportSelectionAction.ImportSelectedAudio
        } else {
            AudioImportSelectionAction.Ignore
        }
    }

    private fun playbackButtonState(
        hasAudio: Boolean,
        isPlaying: Boolean,
    ): PlaybackButtonState? {
        if (!hasAudio) return null

        return if (isPlaying) {
            PlaybackButtonState(
                icon = PlaybackIcon.PAUSE,
                contentDescription = "Pause Playback",
            )
        } else {
            PlaybackButtonState(
                icon = PlaybackIcon.PLAY,
                contentDescription = "Resume Playback",
            )
        }
    }
}
