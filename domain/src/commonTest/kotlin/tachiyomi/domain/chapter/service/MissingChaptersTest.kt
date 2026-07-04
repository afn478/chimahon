package tachiyomi.domain.chapter.service

import tachiyomi.domain.chapter.model.Chapter
import kotlin.test.Test
import kotlin.test.assertEquals

class MissingChaptersTest {

    @Test
    fun `missingChaptersCount returns 0 when empty list`() {
        assertEquals(0, emptyList<Double>().missingChaptersCount())
    }

    @Test
    fun `missingChaptersCount returns 0 when all unknown chapter numbers`() {
        assertEquals(0, listOf(-1.0, -1.0, -1.0).missingChaptersCount())
    }

    @Test
    fun `missingChaptersCount handles repeated base chapter numbers`() {
        assertEquals(0, listOf(1.0, 1.0, 1.1, 1.5, 1.6, 1.99).missingChaptersCount())
    }

    @Test
    fun `missingChaptersCount returns number of missing chapters`() {
        assertEquals(5, listOf(-1.0, 1.0, 2.0, 2.2, 4.0, 6.0, 10.0, 11.0).missingChaptersCount())
    }

    @Test
    fun `calculateChapterGap returns difference`() {
        assertEquals(0, calculateChapterGap(chapter(10.0), chapter(9.0)))
        assertEquals(1, calculateChapterGap(chapter(10.0), chapter(8.0)))
        assertEquals(1, calculateChapterGap(chapter(10.0), chapter(8.5)))
        assertEquals(8, calculateChapterGap(chapter(10.0), chapter(1.1)))

        assertEquals(0, calculateChapterGap(10.0, 9.0))
        assertEquals(1, calculateChapterGap(10.0, 8.0))
        assertEquals(1, calculateChapterGap(10.0, 8.5))
        assertEquals(8, calculateChapterGap(10.0, 1.1))
    }

    @Test
    fun `calculateChapterGap returns 0 if either are not valid chapter numbers`() {
        assertEquals(0, calculateChapterGap(chapter(-1.0), chapter(10.0)))
        assertEquals(0, calculateChapterGap(chapter(99.0), chapter(-1.0)))

        assertEquals(0, calculateChapterGap(-1.0, 10.0))
        assertEquals(0, calculateChapterGap(99.0, -1.0))
    }

    private fun chapter(number: Double) = Chapter.create().copy(
        chapterNumber = number,
    )
}
