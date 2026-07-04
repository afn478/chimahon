package tachiyomi.domain.chapter.service

import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.MangaChapterFlags
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChapterSortPolicyTest {
    @Test
    fun sourceOrderSortingMatchesExistingDirection() {
        val chapters = listOf(
            chapter(id = 1, sourceOrder = 2),
            chapter(id = 2, sourceOrder = 1),
        )

        assertEquals(
            listOf(2L, 1L),
            chapters.sortedWith(
                getChapterSort(
                    sorting = MangaChapterFlags.CHAPTER_SORTING_SOURCE,
                    sortDescending = true,
                ),
            ).map(Chapter::id),
        )
        assertEquals(
            listOf(1L, 2L),
            chapters.sortedWith(
                getChapterSort(
                    sorting = MangaChapterFlags.CHAPTER_SORTING_SOURCE,
                    sortDescending = false,
                ),
            ).map(Chapter::id),
        )
    }

    @Test
    fun chapterNumberSortingUsesChapterNumber() {
        val chapters = listOf(
            chapter(id = 1, number = 2.0),
            chapter(id = 2, number = 1.0),
        )

        assertEquals(
            listOf(1L, 2L),
            chapters.sortedWith(
                getChapterSort(
                    sorting = MangaChapterFlags.CHAPTER_SORTING_NUMBER,
                    sortDescending = true,
                ),
            ).map(Chapter::id),
        )
        assertEquals(
            listOf(2L, 1L),
            chapters.sortedWith(
                getChapterSort(
                    sorting = MangaChapterFlags.CHAPTER_SORTING_NUMBER,
                    sortDescending = false,
                ),
            ).map(Chapter::id),
        )
    }

    @Test
    fun uploadDateSortingUsesUploadTimestamp() {
        val chapters = listOf(
            chapter(id = 1, uploadDate = 20),
            chapter(id = 2, uploadDate = 10),
        )

        assertEquals(
            listOf(1L, 2L),
            chapters.sortedWith(
                getChapterSort(
                    sorting = MangaChapterFlags.CHAPTER_SORTING_UPLOAD_DATE,
                    sortDescending = true,
                ),
            ).map(Chapter::id),
        )
        assertEquals(
            listOf(2L, 1L),
            chapters.sortedWith(
                getChapterSort(
                    sorting = MangaChapterFlags.CHAPTER_SORTING_UPLOAD_DATE,
                    sortDescending = false,
                ),
            ).map(Chapter::id),
        )
    }

    @Test
    fun alphabetSortingUsesProvidedNameComparator() {
        val chapters = listOf(
            chapter(id = 1, name = "chapter b"),
            chapter(id = 2, name = "Chapter A"),
        )
        val caseInsensitiveComparator: (String, String) -> Int = { left, right ->
            left.lowercase().compareTo(right.lowercase())
        }

        assertEquals(
            listOf(2L, 1L),
            chapters.sortedWith(
                getChapterSort(
                    sorting = MangaChapterFlags.CHAPTER_SORTING_ALPHABET,
                    sortDescending = false,
                    compareNames = caseInsensitiveComparator,
                ),
            ).map(Chapter::id),
        )
        assertEquals(
            listOf(1L, 2L),
            chapters.sortedWith(
                getChapterSort(
                    sorting = MangaChapterFlags.CHAPTER_SORTING_ALPHABET,
                    sortDescending = true,
                    compareNames = caseInsensitiveComparator,
                ),
            ).map(Chapter::id),
        )
    }

    @Test
    fun invalidSortingModeThrows() {
        assertFailsWith<NotImplementedError> {
            getChapterSort(
                sorting = Long.MAX_VALUE,
                sortDescending = false,
            )
        }
    }

    private fun chapter(
        id: Long,
        sourceOrder: Long = 0,
        number: Double = 0.0,
        uploadDate: Long = 0,
        name: String = "",
    ): Chapter {
        return Chapter.create().copy(
            id = id,
            sourceOrder = sourceOrder,
            chapterNumber = number,
            dateUpload = uploadDate,
            name = name,
        )
    }
}
