package tachiyomi.domain.reader.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NovelReaderBookInfoTest {
    @Test
    fun resolveCharacterPositionMapsCharactersToSpineIndexAndProgress() {
        val bookInfo = bookInfo()

        assertEquals(1 to 0.5, bookInfo.resolveCharacterPosition(50))
        assertEquals(2 to 0.25, bookInfo.resolveCharacterPosition(125))
    }

    @Test
    fun resolveCharacterPositionClampsToAvailableCharacterRange() {
        val bookInfo = bookInfo()

        assertEquals(1 to 0.0, bookInfo.resolveCharacterPosition(-20))
        assertEquals(2 to 0.99, bookInfo.resolveCharacterPosition(999))
    }

    @Test
    fun resolveCharacterPositionSkipsMissingSpineAndEmptyChapters() {
        val bookInfo = NovelReaderBookInfo(
            characterCount = 100,
            chapterInfo = mapOf(
                "front-matter" to NovelReaderChapterInfo(
                    spineIndex = null,
                    currentTotal = 0,
                    chapterCount = 20,
                ),
                "image" to NovelReaderChapterInfo(
                    spineIndex = 1,
                    currentTotal = 20,
                    chapterCount = 0,
                ),
            ),
        )

        assertNull(bookInfo.resolveCharacterPosition(10))
    }

    private fun bookInfo(): NovelReaderBookInfo {
        return NovelReaderBookInfo(
            characterCount = 200,
            chapterInfo = mapOf(
                "chapter-1" to NovelReaderChapterInfo(
                    spineIndex = 1,
                    currentTotal = 0,
                    chapterCount = 100,
                ),
                "chapter-2" to NovelReaderChapterInfo(
                    spineIndex = 2,
                    currentTotal = 100,
                    chapterCount = 100,
                ),
            ),
        )
    }
}
