package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import tachiyomi.domain.reader.model.NovelReaderSasayakiMatch
import tachiyomi.domain.reader.service.NovelReaderSasayakiPlaybackPolicy.AudioImportAction
import tachiyomi.domain.reader.service.NovelReaderSasayakiPlaybackPolicy.AudioRestoreAction
import tachiyomi.domain.reader.service.NovelReaderSasayakiPlaybackPolicy.CueNavigationAction
import tachiyomi.domain.reader.service.NovelReaderSasayakiPlaybackPolicy.CueUpdateAction
import tachiyomi.domain.reader.service.NovelReaderSasayakiPlaybackPolicy.PlaybackToggleAction
import tachiyomi.domain.reader.service.NovelReaderSasayakiPlaybackPolicy.RestoreCompletedAction

class NovelReaderSasayakiPlaybackPolicyTest {
    @Test
    fun audioRestoreIgnoresMissingOrUnavailableBookmarks() {
        assertEquals(
            AudioRestoreAction.Ignore,
            NovelReaderSasayakiPlaybackPolicy.audioRestoreAction(
                audioBookmark = null,
                audioExists = true,
            ),
        )
        assertEquals(
            AudioRestoreAction.Ignore,
            NovelReaderSasayakiPlaybackPolicy.audioRestoreAction(
                audioBookmark = "",
                audioExists = true,
            ),
        )
        assertEquals(
            AudioRestoreAction.Ignore,
            NovelReaderSasayakiPlaybackPolicy.audioRestoreAction(
                audioBookmark = "/audio/book.m4a",
                audioExists = false,
            ),
        )
    }

    @Test
    fun audioRestoreLoadsExistingBookmark() {
        assertEquals(
            AudioRestoreAction.Restore(audioPath = "/audio/book.m4a"),
            NovelReaderSasayakiPlaybackPolicy.audioRestoreAction(
                audioBookmark = "/audio/book.m4a",
                audioExists = true,
            ),
        )
    }

    @Test
    fun audioImportSavesOnlyNonBlankPaths() {
        assertEquals(
            AudioImportAction.Ignore,
            NovelReaderSasayakiPlaybackPolicy.audioImportAction(audioPath = " "),
        )
        assertEquals(
            AudioImportAction.SaveAndLoad(audioPath = "/audio/imported.m4a"),
            NovelReaderSasayakiPlaybackPolicy.audioImportAction(audioPath = "/audio/imported.m4a"),
        )
    }

    @Test
    fun playbackToggleMapsCurrentAudioStateToPlayerCommand() {
        assertEquals(
            PlaybackToggleAction.Ignore,
            NovelReaderSasayakiPlaybackPolicy.playbackToggleAction(
                hasAudio = false,
                isPlaying = false,
            ),
        )
        assertEquals(
            PlaybackToggleAction.Play,
            NovelReaderSasayakiPlaybackPolicy.playbackToggleAction(
                hasAudio = true,
                isPlaying = false,
            ),
        )
        assertEquals(
            PlaybackToggleAction.Pause,
            NovelReaderSasayakiPlaybackPolicy.playbackToggleAction(
                hasAudio = true,
                isPlaying = true,
            ),
        )
    }

    @Test
    fun cueNavigationLookupUsesCurrentCueOrPlaybackTimeWithOptionalClamp() {
        assertEquals(
            12.0,
            NovelReaderSasayakiPlaybackPolicy.cueNavigationLookupTime(
                currentCueStartTime = 12.0,
                currentPlaybackTime = 30.0,
                delay = 2.5,
                clampToZero = false,
            ),
        )
        assertEquals(
            27.5,
            NovelReaderSasayakiPlaybackPolicy.cueNavigationLookupTime(
                currentCueStartTime = null,
                currentPlaybackTime = 30.0,
                delay = 2.5,
                clampToZero = false,
            ),
        )
        assertEquals(
            0.0,
            NovelReaderSasayakiPlaybackPolicy.cueNavigationLookupTime(
                currentCueStartTime = null,
                currentPlaybackTime = 1.0,
                delay = 2.5,
                clampToZero = true,
            ),
        )
    }

    @Test
    fun cueNavigationBuildsSeekActionsFromCueStartAndDelay() {
        assertEquals(
            CueNavigationAction.Ignore,
            NovelReaderSasayakiPlaybackPolicy.nextCueSeekAction(
                nextCueStartTime = null,
                delay = 0.5,
            ),
        )
        assertEquals(
            CueNavigationAction.Seek(seconds = 10.5),
            NovelReaderSasayakiPlaybackPolicy.nextCueSeekAction(
                nextCueStartTime = 10.0,
                delay = 0.5,
            ),
        )
        assertEquals(
            CueNavigationAction.Seek(seconds = 9.5),
            NovelReaderSasayakiPlaybackPolicy.previousCueSeekAction(
                previousCueStartTime = 10.0,
                delay = -0.5,
            ),
        )
        assertEquals(
            CueNavigationAction.Seek(seconds = 0.5),
            NovelReaderSasayakiPlaybackPolicy.previousCueSeekAction(
                previousCueStartTime = null,
                delay = 0.5,
            ),
        )
    }

    @Test
    fun cueUpdateIgnoresWhenPlaybackIsUnavailableOrTransitioning() {
        assertEquals(CueUpdateAction.Ignore, cueUpdate(hasAudio = false))
        assertEquals(CueUpdateAction.Ignore, cueUpdate(hasMatch = false))
        assertEquals(CueUpdateAction.Ignore, cueUpdate(chapterTransition = true))
    }

    @Test
    fun cueUpdateClearsWhenThereIsNoActiveCue() {
        assertEquals(
            CueUpdateAction.ClearDisplayedCue,
            cueUpdate(cue = null),
        )
    }

    @Test
    fun cueUpdateIgnoresWhenCueIsAlreadyDisplayed() {
        assertEquals(
            CueUpdateAction.Ignore,
            cueUpdate(cue = cueInChapter0, currentCueId = cueInChapter0.id),
        )
    }

    @Test
    fun cueUpdateDisplaysCueInCurrentChapter() {
        assertEquals(
            CueUpdateAction.DisplayCue(cue = cueInChapter0, reveal = false),
            cueUpdate(cue = cueInChapter0, currentChapterIndex = 0),
        )
        assertEquals(
            CueUpdateAction.DisplayCue(cue = cueInChapter0, reveal = true),
            cueUpdate(
                cue = cueInChapter0,
                currentChapterIndex = 0,
                autoScroll = true,
                hasPlayedOnce = true,
            ),
        )
    }

    @Test
    fun cueUpdateLoadsOtherChapterOnlyWhenRevealIsAllowed() {
        assertEquals(
            CueUpdateAction.LoadChapterForCue(
                cue = cueInChapter1,
                chapterIndex = cueInChapter1.chapterIndex,
            ),
            cueUpdate(
                cue = cueInChapter1,
                currentChapterIndex = 0,
                autoScroll = true,
                hasPlayedOnce = true,
            ),
        )
        assertEquals(
            CueUpdateAction.ClearDisplayedCue,
            cueUpdate(
                cue = cueInChapter1,
                currentChapterIndex = 0,
                autoScroll = false,
                hasPlayedOnce = true,
            ),
        )
    }

    @Test
    fun restoreCompletedIgnoresWhenThereIsNoMatchOrTransition() {
        assertEquals(RestoreCompletedAction.Ignore, restoreCompleted(hasMatch = false))
        assertEquals(RestoreCompletedAction.Ignore, restoreCompleted(chapterTransition = false))
    }

    @Test
    fun restoreCompletedPrefersPendingCueForCurrentChapter() {
        assertEquals(
            RestoreCompletedAction.Complete(
                cue = cueInChapter0,
                reveal = true,
                resume = true,
            ),
            restoreCompleted(
                currentChapterIndex = 0,
                pendingCue = cueInChapter0,
                timelineCue = cueInChapter0Later,
                shouldResume = true,
                autoScroll = true,
                hasPlayedOnce = true,
            ),
        )
    }

    @Test
    fun restoreCompletedUsesTimelineCueWhenPendingCueDoesNotMatch() {
        assertEquals(
            RestoreCompletedAction.Complete(
                cue = cueInChapter0,
                reveal = false,
                resume = false,
            ),
            restoreCompleted(
                currentChapterIndex = 0,
                pendingCue = cueInChapter1,
                timelineCue = cueInChapter0,
            ),
        )
    }

    @Test
    fun restoreCompletedCompletesWithoutCueWhenNothingMatchesCurrentChapter() {
        assertEquals(
            RestoreCompletedAction.Complete(
                cue = null,
                reveal = true,
                resume = true,
            ),
            restoreCompleted(
                currentChapterIndex = 2,
                pendingCue = cueInChapter0,
                timelineCue = cueInChapter1,
                shouldResume = true,
                autoScroll = true,
                hasPlayedOnce = true,
            ),
        )
    }

    private fun cueUpdate(
        hasAudio: Boolean = true,
        hasMatch: Boolean = true,
        chapterTransition: Boolean = false,
        cue: NovelReaderSasayakiMatch? = cueInChapter0,
        currentCueId: String? = null,
        currentChapterIndex: Int = 0,
        autoScroll: Boolean = false,
        hasPlayedOnce: Boolean = false,
    ): CueUpdateAction {
        return NovelReaderSasayakiPlaybackPolicy.cueUpdateAction(
            hasAudio = hasAudio,
            hasMatch = hasMatch,
            chapterTransition = chapterTransition,
            cue = cue,
            currentCueId = currentCueId,
            currentChapterIndex = currentChapterIndex,
            autoScroll = autoScroll,
            hasPlayedOnce = hasPlayedOnce,
        )
    }

    private fun restoreCompleted(
        hasMatch: Boolean = true,
        chapterTransition: Boolean = true,
        currentChapterIndex: Int = 0,
        pendingCue: NovelReaderSasayakiMatch? = cueInChapter0,
        timelineCue: NovelReaderSasayakiMatch? = null,
        shouldResume: Boolean = false,
        autoScroll: Boolean = false,
        hasPlayedOnce: Boolean = false,
    ): RestoreCompletedAction {
        return NovelReaderSasayakiPlaybackPolicy.restoreCompletedAction(
            hasMatch = hasMatch,
            chapterTransition = chapterTransition,
            currentChapterIndex = currentChapterIndex,
            pendingCue = pendingCue,
            timelineCue = timelineCue,
            shouldResume = shouldResume,
            autoScroll = autoScroll,
            hasPlayedOnce = hasPlayedOnce,
        )
    }

    private companion object {
        val cueInChapter0 = NovelReaderSasayakiMatch(
            id = "cue-chapter-0",
            startTime = 1.0,
            endTime = 2.0,
            text = "first",
            chapterIndex = 0,
            start = 0,
            length = 5,
        )
        val cueInChapter0Later = NovelReaderSasayakiMatch(
            id = "cue-chapter-0-later",
            startTime = 3.0,
            endTime = 4.0,
            text = "later",
            chapterIndex = 0,
            start = 10,
            length = 5,
        )
        val cueInChapter1 = NovelReaderSasayakiMatch(
            id = "cue-chapter-1",
            startTime = 5.0,
            endTime = 6.0,
            text = "second",
            chapterIndex = 1,
            start = 20,
            length = 6,
        )
    }
}
