package tachiyomi.domain.chapter.service

import tachiyomi.domain.chapter.model.Chapter

data class NewChaptersSyncResult(
    val chaptersToAdd: List<Chapter>,
    val changedOrDuplicateReadUrls: Set<String>,
)

object ChapterSyncPolicy {
    fun prepareNewChaptersForInsert(
        newChapters: List<Chapter>,
        dbChapters: List<Chapter>,
        removedChapters: List<Chapter>,
        nowMillis: Long,
        markDuplicateAsRead: Boolean,
        carryOverLastPageRead: Boolean,
    ): NewChaptersSyncResult {
        val changedOrDuplicateReadUrls = mutableSetOf<String>()
        val deletedChapterNumbers = removedChapters.map { it.chapterNumber }.toSet()
        val deletedReadChapterNumbers = removedChapters
            .filter { it.read }
            .map { it.chapterNumber }
            .toSet()
        val deletedBookmarkedChapterNumbers = removedChapters
            .filter { it.bookmark }
            .map { it.chapterNumber }
            .toSet()
        val deletedChapterNumberDateFetchMap = removedChapters
            .sortedByDescending { it.dateFetch }
            .associate { it.chapterNumber to it.dateFetch }

        val readChapterNumbers = dbChapters
            .asSequence()
            .filter { it.read && it.isRecognizedNumber }
            .map { it.chapterNumber }
            .toSet()

        var itemCount = newChapters.size
        var chaptersToAdd = newChapters.map { chapterToAdd ->
            var chapter = chapterToAdd.copy(dateFetch = nowMillis + itemCount--)

            if (chapter.chapterNumber in readChapterNumbers && markDuplicateAsRead) {
                changedOrDuplicateReadUrls.add(chapter.url)
                chapter = chapter.copy(read = true)
            }

            if (!chapter.isRecognizedNumber || chapter.chapterNumber !in deletedChapterNumbers) {
                return@map chapter
            }

            chapter = chapter.copy(
                read = chapter.chapterNumber in deletedReadChapterNumbers,
                bookmark = chapter.chapterNumber in deletedBookmarkedChapterNumbers,
            )

            deletedChapterNumberDateFetchMap[chapter.chapterNumber]?.let {
                chapter = chapter.copy(dateFetch = it)
            }

            changedOrDuplicateReadUrls.add(chapter.url)

            chapter
        }

        if (carryOverLastPageRead) {
            val hasNewChapters = chaptersToAdd.any { it.url !in changedOrDuplicateReadUrls }
            val lastPageRead = dbChapters.maxOfOrNull { it.lastPageRead }

            if (hasNewChapters && lastPageRead != null && lastPageRead > 0) {
                chaptersToAdd = chaptersToAdd.map {
                    if (it.url in changedOrDuplicateReadUrls) {
                        it
                    } else {
                        it.copy(lastPageRead = lastPageRead)
                    }
                }
            }
        }

        return NewChaptersSyncResult(
            chaptersToAdd = chaptersToAdd,
            changedOrDuplicateReadUrls = changedOrDuplicateReadUrls,
        )
    }
}
