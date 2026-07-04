package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import tachiyomi.domain.reader.model.NovelReaderSasayakiMatch
import tachiyomi.domain.reader.service.NovelReaderSasayakiPlaybackPolicy.CueUpdateAction
import tachiyomi.domain.reader.service.NovelReaderSasayakiPlaybackPolicy.RestoreCompletedAction

class NovelReaderSasayakiPlaybackPolicyTest {
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
