package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.NovelReaderSasayakiMatch

object NovelReaderSasayakiPlaybackPolicy {
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
