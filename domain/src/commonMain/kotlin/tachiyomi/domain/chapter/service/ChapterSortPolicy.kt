package tachiyomi.domain.chapter.service

import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.MangaChapterFlags

fun getChapterSort(
    sorting: Long,
    sortDescending: Boolean,
    compareNames: (String, String) -> Int = String::compareTo,
): (
    Chapter,
    Chapter,
) -> Int {
    return when (sorting) {
        MangaChapterFlags.CHAPTER_SORTING_SOURCE -> when (sortDescending) {
            true -> { c1, c2 -> c1.sourceOrder.compareTo(c2.sourceOrder) }
            false -> { c1, c2 -> c2.sourceOrder.compareTo(c1.sourceOrder) }
        }
        MangaChapterFlags.CHAPTER_SORTING_NUMBER -> when (sortDescending) {
            true -> { c1, c2 -> c2.chapterNumber.compareTo(c1.chapterNumber) }
            false -> { c1, c2 -> c1.chapterNumber.compareTo(c2.chapterNumber) }
        }
        MangaChapterFlags.CHAPTER_SORTING_UPLOAD_DATE -> when (sortDescending) {
            true -> { c1, c2 -> c2.dateUpload.compareTo(c1.dateUpload) }
            false -> { c1, c2 -> c1.dateUpload.compareTo(c2.dateUpload) }
        }
        MangaChapterFlags.CHAPTER_SORTING_ALPHABET -> when (sortDescending) {
            true -> { c1, c2 -> compareNames(c2.name, c1.name) }
            false -> { c1, c2 -> compareNames(c1.name, c2.name) }
        }
        else -> throw NotImplementedError("Invalid chapter sorting method: $sorting")
    }
}
