package tachiyomi.domain.reader.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NovelReaderSasayakiTest {
    @Test
    fun playbackDataDefaultsRemainStable() {
        val playback = NovelReaderSasayakiPlaybackData(lastPosition = 12.5)

        assertEquals(12.5, playback.lastPosition)
        assertEquals(0.0, playback.delay)
        assertEquals(1f, playback.rate)
        assertNull(playback.audioBookmark)
    }

    @Test
    fun matchDataKeepsSerializableFieldShape() {
        val matchData = NovelReaderSasayakiMatchData(
            matches = listOf(
                NovelReaderSasayakiMatch(
                    id = "cue-1",
                    startTime = 1.25,
                    endTime = 2.5,
                    text = "line",
                    chapterIndex = 3,
                    start = 40,
                    length = 8,
                ),
            ),
            unmatched = 2,
        )

        val encoded = JSON.encodeToString(matchData)

        assertEquals(
            """{"matches":[{"id":"cue-1","startTime":1.25,"endTime":2.5,"text":"line","chapterIndex":3,"start":40,"length":8}],"unmatched":2}""",
            encoded,
        )
        assertEquals(matchData, JSON.decodeFromString(encoded))
    }

    private companion object {
        val JSON = Json
    }
}
