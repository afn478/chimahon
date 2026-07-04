package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import tachiyomi.domain.reader.model.NovelReaderSasayakiMatch
import tachiyomi.domain.reader.model.NovelReaderSasayakiMatchData

class NovelReaderSasayakiCueTimelineTest {
    @Test
    fun nextCueReturnsNextStartAndSkipsExactCurrentCue() {
        val timeline = NovelReaderSasayakiCueTimeline(matchData())

        assertEquals(1.0, timeline.nextCue(afterTime = 0.5))
        assertEquals(4.0, timeline.nextCue(afterTime = 1.0))
        assertEquals(4.0, timeline.nextCue(afterTime = 2.0))
        assertNull(timeline.nextCue(afterTime = 8.0))
    }

    @Test
    fun prevCueReturnsPreviousStart() {
        val timeline = NovelReaderSasayakiCueTimeline(matchData())

        assertNull(timeline.prevCue(beforeTime = 0.5))
        assertNull(timeline.prevCue(beforeTime = 1.0))
        assertEquals(1.0, timeline.prevCue(beforeTime = 2.0))
        assertEquals(4.0, timeline.prevCue(beforeTime = 8.0))
    }

    @Test
    fun cueAtUsesStartToleranceAndCueEnd() {
        val timeline = NovelReaderSasayakiCueTimeline(matchData())

        assertEquals(cue1, timeline.cueAt(time = 0.995))
        assertEquals(cue1, timeline.cueAt(time = 1.0))
        assertEquals(cue1, timeline.cueAt(time = 2.5))
        assertNull(timeline.cueAt(time = 2.51))
        assertEquals(cue2, timeline.cueAt(time = 3.995))
        assertNull(timeline.cueAt(time = 9.01))
    }

    @Test
    fun emptyTimelineReturnsNoCues() {
        val timeline = NovelReaderSasayakiCueTimeline(matchData = null)

        assertNull(timeline.nextCue(afterTime = 0.0))
        assertNull(timeline.prevCue(beforeTime = 10.0))
        assertNull(timeline.cueAt(time = 1.0))
    }

    private fun matchData(): NovelReaderSasayakiMatchData {
        return NovelReaderSasayakiMatchData(
            matches = listOf(cue1, cue2, cue3),
            unmatched = 0,
        )
    }

    private companion object {
        val cue1 = NovelReaderSasayakiMatch(
            id = "cue-1",
            startTime = 1.0,
            endTime = 2.5,
            text = "first",
            chapterIndex = 0,
            start = 0,
            length = 5,
        )
        val cue2 = NovelReaderSasayakiMatch(
            id = "cue-2",
            startTime = 4.0,
            endTime = 5.5,
            text = "second",
            chapterIndex = 1,
            start = 10,
            length = 6,
        )
        val cue3 = NovelReaderSasayakiMatch(
            id = "cue-3",
            startTime = 8.0,
            endTime = 9.0,
            text = "third",
            chapterIndex = 1,
            start = 20,
            length = 5,
        )
    }
}
