package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import tachiyomi.domain.reader.model.NovelReaderChapterProgress

class NovelReaderProgressPolicyTest {
    @Test
    fun accumulatedCharacterCountsIncludesChapterStartsAndFinalTotal() {
        assertEquals(
            listOf(0, 120, 120, 320),
            NovelReaderProgressPolicy.accumulatedCharacterCounts(listOf(120, 0, 200)),
        )
    }

    @Test
    fun exploredCharacterCountAddsPreviousChaptersAndCurrentProgress() {
        val chapterCounts = listOf(100, 200, 300)

        assertEquals(
            250,
            NovelReaderProgressPolicy.exploredCharacterCount(
                chapterIndex = 1,
                progress = 0.75,
                chapterCharacterCount = chapterCounts::get,
            ),
        )
    }

    @Test
    fun chapterProgressForCharacterCountFindsContainingChapter() {
        assertEquals(
            NovelReaderChapterProgress(chapterIndex = 1, progress = 0.25),
            NovelReaderProgressPolicy.chapterProgressForCharacterCount(
                totalExploredCharacters = 150,
                chapterCharacterCounts = listOf(100, 200, 300),
            ),
        )
    }

    @Test
    fun chapterProgressForCharacterCountHandlesZeroLengthAndOverflow() {
        assertEquals(
            NovelReaderChapterProgress(chapterIndex = 0, progress = 0.0),
            NovelReaderProgressPolicy.chapterProgressForCharacterCount(
                totalExploredCharacters = 0,
                chapterCharacterCounts = listOf(0, 100, 300),
            ),
        )
        assertEquals(
            NovelReaderChapterProgress(chapterIndex = 2, progress = 1.0),
            NovelReaderProgressPolicy.chapterProgressForCharacterCount(
                totalExploredCharacters = 999,
                chapterCharacterCounts = listOf(100, 0, 300),
            ),
        )
    }

    @Test
    fun chapterProgressForCharacterCountClampsNegativeProgressToStart() {
        assertEquals(
            NovelReaderChapterProgress(chapterIndex = 0, progress = 0.0),
            NovelReaderProgressPolicy.chapterProgressForCharacterCount(
                totalExploredCharacters = -20,
                chapterCharacterCounts = listOf(100, 200),
            ),
        )
    }

    @Test
    fun shouldPersistBookmarkTracksForcePositionCharactersAndProgressEpsilon() {
        val unchanged = NovelReaderProgressPolicy.shouldPersistBookmark(
            force = false,
            chapterIndex = 2,
            progress = 0.5,
            characterCount = 250,
            lastChapterIndex = 2,
            lastProgress = 0.50005,
            lastCharacterCount = 250,
            progressEpsilon = 0.0001,
        )

        assertFalse(unchanged)
        assertTrue(
            NovelReaderProgressPolicy.shouldPersistBookmark(
                force = true,
                chapterIndex = 2,
                progress = 0.5,
                characterCount = 250,
                lastChapterIndex = 2,
                lastProgress = 0.5,
                lastCharacterCount = 250,
                progressEpsilon = 0.0001,
            ),
        )
        assertTrue(
            NovelReaderProgressPolicy.shouldPersistBookmark(
                force = false,
                chapterIndex = 2,
                progress = 0.5002,
                characterCount = 250,
                lastChapterIndex = 2,
                lastProgress = 0.5,
                lastCharacterCount = 250,
                progressEpsilon = 0.0001,
            ),
        )
    }

    @Test
    fun shouldPersistPeriodicallyUsesReaderAutoPersistInterval() {
        assertFalse(
            NovelReaderProgressPolicy.shouldPersistPeriodically(
                nowMillis = 60_999L,
                lastPersistTimeMillis = 1_000L,
            ),
        )
        assertTrue(
            NovelReaderProgressPolicy.shouldPersistPeriodically(
                nowMillis = 61_000L,
                lastPersistTimeMillis = 1_000L,
            ),
        )
    }

    @Test
    fun shouldPersistPeriodicallySupportsCustomAndZeroIntervals() {
        assertFalse(
            NovelReaderProgressPolicy.shouldPersistPeriodically(
                nowMillis = 1_249L,
                lastPersistTimeMillis = 1_000L,
                intervalMillis = 250L,
            ),
        )
        assertTrue(
            NovelReaderProgressPolicy.shouldPersistPeriodically(
                nowMillis = 1_250L,
                lastPersistTimeMillis = 1_000L,
                intervalMillis = 250L,
            ),
        )
        assertTrue(
            NovelReaderProgressPolicy.shouldPersistPeriodically(
                nowMillis = 1_000L,
                lastPersistTimeMillis = 1_000L,
                intervalMillis = -1L,
            ),
        )
    }

    @Test
    fun scrollProgressReportActionIgnoresInactiveReaderModes() {
        assertEquals(
            NovelReaderProgressPolicy.ScrollProgressReportAction.Ignore,
            NovelReaderProgressPolicy.scrollProgressReportAction(
                continuousMode = false,
                imageOnly = false,
                nowMillis = 2_500L,
                lastReportMillis = 1_000L,
            ),
        )
        assertEquals(
            NovelReaderProgressPolicy.ScrollProgressReportAction.Ignore,
            NovelReaderProgressPolicy.scrollProgressReportAction(
                continuousMode = true,
                imageOnly = true,
                nowMillis = 2_500L,
                lastReportMillis = 1_000L,
            ),
        )
    }

    @Test
    fun scrollProgressReportActionReportsOnlyAfterIntervalHasElapsed() {
        assertEquals(
            NovelReaderProgressPolicy.ScrollProgressReportAction.ScheduleDelayed(
                NovelReaderProgressPolicy.WEB_SCROLL_PROGRESS_REPORT_INTERVAL_MS,
            ),
            NovelReaderProgressPolicy.scrollProgressReportAction(
                continuousMode = true,
                imageOnly = false,
                nowMillis = 2_000L,
                lastReportMillis = 1_000L,
            ),
        )
        assertEquals(
            NovelReaderProgressPolicy.ScrollProgressReportAction.ReportNow(2_001L),
            NovelReaderProgressPolicy.scrollProgressReportAction(
                continuousMode = true,
                imageOnly = false,
                nowMillis = 2_001L,
                lastReportMillis = 1_000L,
            ),
        )
    }

    @Test
    fun scrollProgressReportActionUsesCustomIntervalForAlternateHosts() {
        assertEquals(
            NovelReaderProgressPolicy.ScrollProgressReportAction.ScheduleDelayed(250L),
            NovelReaderProgressPolicy.scrollProgressReportAction(
                continuousMode = true,
                imageOnly = false,
                nowMillis = 1_200L,
                lastReportMillis = 1_000L,
                reportIntervalMillis = 250L,
            ),
        )
        assertEquals(
            NovelReaderProgressPolicy.ScrollProgressReportAction.ReportNow(1_251L),
            NovelReaderProgressPolicy.scrollProgressReportAction(
                continuousMode = true,
                imageOnly = false,
                nowMillis = 1_251L,
                lastReportMillis = 1_000L,
                reportIntervalMillis = 250L,
            ),
        )
    }

    @Test
    fun scrollProgressReportActionClampsNegativeIntervalToZero() {
        assertEquals(
            NovelReaderProgressPolicy.ScrollProgressReportAction.ScheduleDelayed(0L),
            NovelReaderProgressPolicy.scrollProgressReportAction(
                continuousMode = true,
                imageOnly = false,
                nowMillis = 1_000L,
                lastReportMillis = 1_000L,
                reportIntervalMillis = -1L,
            ),
        )
        assertEquals(
            NovelReaderProgressPolicy.ScrollProgressReportAction.ReportNow(1_001L),
            NovelReaderProgressPolicy.scrollProgressReportAction(
                continuousMode = true,
                imageOnly = false,
                nowMillis = 1_001L,
                lastReportMillis = 1_000L,
                reportIntervalMillis = -1L,
            ),
        )
    }

    @Test
    fun webProgressRequestActionEvaluatesOnlyForContinuousTextReaders() {
        assertEquals(
            NovelReaderProgressPolicy.WebProgressRequestAction.EvaluateScript(
                NovelReaderWebScriptPolicy.calculateProgressScript(),
            ),
            NovelReaderProgressPolicy.webProgressRequestAction(
                continuousMode = true,
                imageOnly = false,
            ),
        )
        assertEquals(
            NovelReaderProgressPolicy.WebProgressRequestAction.Ignore,
            NovelReaderProgressPolicy.webProgressRequestAction(
                continuousMode = false,
                imageOnly = false,
            ),
        )
        assertEquals(
            NovelReaderProgressPolicy.WebProgressRequestAction.Ignore,
            NovelReaderProgressPolicy.webProgressRequestAction(
                continuousMode = true,
                imageOnly = true,
            ),
        )
    }

    @Test
    fun webProgressResultActionReportsOnlyParsedProgress() {
        assertEquals(
            NovelReaderProgressPolicy.WebProgressResultAction.ReportProgress(0.33),
            NovelReaderProgressPolicy.webProgressResultAction("\"0.33\""),
        )
        assertEquals(
            NovelReaderProgressPolicy.WebProgressResultAction.Ignore,
            NovelReaderProgressPolicy.webProgressResultAction("\"limit\""),
        )
        assertEquals(
            NovelReaderProgressPolicy.WebProgressResultAction.Ignore,
            NovelReaderProgressPolicy.webProgressResultAction(null),
        )
    }
}
