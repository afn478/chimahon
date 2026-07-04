package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.NovelReaderSasayakiMatch

object NovelReaderSasayakiPlaybackPolicy {
    data class PlaybackActivityState(
        val isPlaying: Boolean,
        val hasPlayedOnce: Boolean,
        val trackProgress: Boolean,
        val savePlayback: Boolean,
    )

    data class PlaybackTickState(
        val currentTime: Double,
        val lastPosition: Double,
        val duration: Double?,
        val stopPlaybackTime: Double?,
        val pausePlayback: Boolean,
    )

    data class TransitionPreparationState(
        val shouldResume: Boolean,
        val chapterTransition: Boolean = true,
        val stopPlaybackTime: Double? = null,
        val clearDisplayedCue: Boolean = true,
        val pausePlayback: Boolean = true,
    )

    sealed interface AudioRestoreAction {
        data object Ignore : AudioRestoreAction
        data class Restore(val audioPath: String) : AudioRestoreAction
    }

    sealed interface AudioImportAction {
        data object Ignore : AudioImportAction
        data class SaveAndLoad(val audioPath: String) : AudioImportAction
    }

    sealed interface PlaybackToggleAction {
        data object Ignore : PlaybackToggleAction
        data object Pause : PlaybackToggleAction
        data object Play : PlaybackToggleAction
    }

    sealed interface PlaybackStateChangedAction {
        data object Ignore : PlaybackStateChangedAction
        data object MarkEnded : PlaybackStateChangedAction
    }

    sealed interface CueNavigationAction {
        data object Ignore : CueNavigationAction
        data class Seek(val seconds: Double) : CueNavigationAction
    }

    sealed interface CueUpdateAction {
        data object Ignore : CueUpdateAction
        data object ClearDisplayedCue : CueUpdateAction
        data class DisplayCue(
            val cue: NovelReaderSasayakiMatch,
            val reveal: Boolean,
        ) : CueUpdateAction
        data class LoadChapterForCue(
            val cue: NovelReaderSasayakiMatch,
            val chapterIndex: Int,
        ) : CueUpdateAction
    }

    sealed interface RestoreCompletedAction {
        data object Ignore : RestoreCompletedAction
        data class Complete(
            val cue: NovelReaderSasayakiMatch?,
            val reveal: Boolean,
            val resume: Boolean,
        ) : RestoreCompletedAction
    }

    fun playbackActivityState(
        isPlayingNow: Boolean,
        hasPlayedOnce: Boolean,
    ): PlaybackActivityState {
        return PlaybackActivityState(
            isPlaying = isPlayingNow,
            hasPlayedOnce = hasPlayedOnce || isPlayingNow,
            trackProgress = isPlayingNow,
            savePlayback = !isPlayingNow,
        )
    }

    fun playbackTickState(
        seconds: Double,
        durationSeconds: Double?,
        stopPlaybackTime: Double?,
    ): PlaybackTickState {
        val shouldStop = stopPlaybackTime != null && seconds >= stopPlaybackTime
        return PlaybackTickState(
            currentTime = seconds,
            lastPosition = seconds,
            duration = durationSeconds?.takeIf { it > 0.0 },
            stopPlaybackTime = if (shouldStop) null else stopPlaybackTime,
            pausePlayback = shouldStop,
        )
    }

    fun playbackStateChangedAction(hasEnded: Boolean): PlaybackStateChangedAction {
        return if (hasEnded) {
            PlaybackStateChangedAction.MarkEnded
        } else {
            PlaybackStateChangedAction.Ignore
        }
    }

    fun transitionPreparationState(isPlaying: Boolean): TransitionPreparationState {
        return TransitionPreparationState(shouldResume = isPlaying)
    }

    fun audioRestoreAction(
        audioBookmark: String?,
        audioExists: Boolean,
    ): AudioRestoreAction {
        val audioPath = audioBookmark?.takeIf { it.isNotBlank() } ?: return AudioRestoreAction.Ignore
        return if (audioExists) {
            AudioRestoreAction.Restore(audioPath)
        } else {
            AudioRestoreAction.Ignore
        }
    }

    fun audioImportAction(audioPath: String): AudioImportAction {
        return if (audioPath.isBlank()) {
            AudioImportAction.Ignore
        } else {
            AudioImportAction.SaveAndLoad(audioPath)
        }
    }

    fun playbackToggleAction(
        hasAudio: Boolean,
        isPlaying: Boolean,
    ): PlaybackToggleAction {
        if (!hasAudio) return PlaybackToggleAction.Ignore
        return if (isPlaying) {
            PlaybackToggleAction.Pause
        } else {
            PlaybackToggleAction.Play
        }
    }

    fun cueNavigationLookupTime(
        currentCueStartTime: Double?,
        currentPlaybackTime: Double,
        delay: Double,
        clampToZero: Boolean,
    ): Double {
        val lookupTime = currentCueStartTime ?: (currentPlaybackTime - delay)
        return if (clampToZero) {
            maxOf(0.0, lookupTime)
        } else {
            lookupTime
        }
    }

    fun nextCueSeekAction(
        nextCueStartTime: Double?,
        delay: Double,
    ): CueNavigationAction {
        return if (nextCueStartTime != null) {
            CueNavigationAction.Seek(nextCueStartTime + delay)
        } else {
            CueNavigationAction.Ignore
        }
    }

    fun previousCueSeekAction(
        previousCueStartTime: Double?,
        delay: Double,
    ): CueNavigationAction {
        return CueNavigationAction.Seek((previousCueStartTime ?: 0.0) + delay)
    }

    fun cueUpdateAction(
        hasAudio: Boolean,
        hasMatch: Boolean,
        chapterTransition: Boolean,
        cue: NovelReaderSasayakiMatch?,
        currentCueId: String?,
        currentChapterIndex: Int,
        autoScroll: Boolean,
        hasPlayedOnce: Boolean,
    ): CueUpdateAction {
        if (!hasAudio || !hasMatch || chapterTransition) return CueUpdateAction.Ignore
        if (cue == null) return CueUpdateAction.ClearDisplayedCue
        if (cue.id == currentCueId) return CueUpdateAction.Ignore

        val shouldReveal = autoScroll && hasPlayedOnce
        return when {
            cue.chapterIndex == currentChapterIndex -> CueUpdateAction.DisplayCue(
                cue = cue,
                reveal = shouldReveal,
            )
            shouldReveal -> CueUpdateAction.LoadChapterForCue(
                cue = cue,
                chapterIndex = cue.chapterIndex,
            )
            else -> CueUpdateAction.ClearDisplayedCue
        }
    }

    fun restoreCompletedAction(
        hasMatch: Boolean,
        chapterTransition: Boolean,
        currentChapterIndex: Int,
        pendingCue: NovelReaderSasayakiMatch?,
        timelineCue: NovelReaderSasayakiMatch?,
        shouldResume: Boolean,
        autoScroll: Boolean,
        hasPlayedOnce: Boolean,
    ): RestoreCompletedAction {
        if (!hasMatch || !chapterTransition) return RestoreCompletedAction.Ignore

        val cue = when {
            pendingCue?.chapterIndex == currentChapterIndex -> pendingCue
            timelineCue?.chapterIndex == currentChapterIndex -> timelineCue
            else -> null
        }

        return RestoreCompletedAction.Complete(
            cue = cue,
            reveal = autoScroll && hasPlayedOnce,
            resume = shouldResume,
        )
    }
}
