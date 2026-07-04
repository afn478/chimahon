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
}
