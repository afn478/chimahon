package tachiyomi.domain.chapter.service

import exh.source.MERGED_SOURCE_ID
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.MergedMangaReference
import kotlin.test.Test
import kotlin.test.assertEquals

class MergedChapterListTest {
    @Test
    fun transformMergedChapterListSkipsDedupeWhenDisabled() {
        val chapters = listOf(
            chapter(id = 1, mangaId = 10, number = 1.0),
            chapter(id = 2, mangaId = 20, number = 1.0),
        )

        assertEquals(chapters, transformMergedChapterList(emptyList(), chapters, dedupe = false))
    }

    @Test
    fun dedupeMergedChapterListKeepsAllChaptersForNoneMode() {
        val chapters = listOf(
            chapter(id = 1, mangaId = 10, number = 1.0),
            chapter(id = 2, mangaId = 20, number = 1.0),
        )

        assertEquals(chapters, dedupeMergedChapterList(references(MergedMangaReference.CHAPTER_SORT_NONE), chapters))
    }

    @Test
    fun dedupeMergedChapterListKeepsSourceWithMostChapters() {
        val chapters = listOf(
            chapter(id = 1, mangaId = 10, number = 1.0),
            chapter(id = 2, mangaId = 20, number = 1.0),
            chapter(id = 3, mangaId = 20, number = 2.0),
        )

        val deduped = dedupeMergedChapterList(
            references(MergedMangaReference.CHAPTER_SORT_MOST_CHAPTERS),
            chapters,
        )

        assertEquals(listOf(2L, 3L), deduped.map(Chapter::id))
    }

    @Test
    fun dedupeMergedChapterListKeepsSourceWithHighestChapterNumber() {
        val chapters = listOf(
            chapter(id = 1, mangaId = 10, number = 10.0),
            chapter(id = 2, mangaId = 20, number = 9.0),
            chapter(id = 3, mangaId = 20, number = 11.0),
        )

        val deduped = dedupeMergedChapterList(
            references(MergedMangaReference.CHAPTER_SORT_HIGHEST_CHAPTER_NUMBER),
            chapters,
        )

        assertEquals(listOf(2L, 3L), deduped.map(Chapter::id))
    }

    @Test
    fun dedupeMergedChapterListUsesReferencePriorityForDuplicateNumbers() {
        val chapters = listOf(
            chapter(id = 1, mangaId = 10, number = 1.0),
            chapter(id = 2, mangaId = 10, number = 2.0),
            chapter(id = 3, mangaId = 20, number = 1.0),
            chapter(id = 4, mangaId = 20, number = 2.0),
            chapter(id = 5, mangaId = 20, number = 3.0),
        )

        val deduped = dedupeMergedChapterList(
            references(
                chapterSortMode = MergedMangaReference.CHAPTER_SORT_PRIORITY,
                priorities = mapOf(20L to 0, 10L to 1),
            ),
            chapters,
        )

        assertEquals(listOf(3L, 4L, 5L), deduped.map(Chapter::id))
        assertEquals(listOf(0L, 1L, 2L), deduped.map(Chapter::sourceOrder))
    }

    @Test
    fun dedupeMergedChapterListAllowsDuplicateNumbersWithinSameSource() {
        val chapters = listOf(
            chapter(id = 1, mangaId = 10, number = 1.0),
            chapter(id = 2, mangaId = 10, number = 1.0),
            chapter(id = 3, mangaId = 20, number = 1.0),
        )

        val deduped = dedupeMergedChapterList(
            references(
                chapterSortMode = MergedMangaReference.CHAPTER_SORT_PRIORITY,
                priorities = mapOf(10L to 0, 20L to 1),
            ),
            chapters,
        )

        assertEquals(listOf(1L, 2L), deduped.map(Chapter::id))
    }
}

private fun chapter(
    id: Long,
    mangaId: Long,
    number: Double,
): Chapter {
    return Chapter.create().copy(
        id = id,
        mangaId = mangaId,
        name = "Chapter $id",
        chapterNumber = number,
        sourceOrder = id,
    )
}

private fun references(
    chapterSortMode: Int,
    priorities: Map<Long, Int> = mapOf(10L to 0, 20L to 1),
): List<MergedMangaReference> {
    return listOf(
        reference(
            id = 1,
            mangaId = 100,
            mangaSourceId = MERGED_SOURCE_ID,
            chapterSortMode = chapterSortMode,
        ),
    ) + priorities.map { (mangaId, priority) ->
        reference(
            id = mangaId,
            mangaId = mangaId,
            mangaSourceId = mangaId,
            chapterPriority = priority,
        )
    }
}

private fun reference(
    id: Long,
    mangaId: Long,
    mangaSourceId: Long,
    chapterSortMode: Int = MergedMangaReference.CHAPTER_SORT_NONE,
    chapterPriority: Int = 0,
): MergedMangaReference {
    return MergedMangaReference(
        id = id,
        isInfoManga = false,
        getChapterUpdates = true,
        chapterSortMode = chapterSortMode,
        chapterPriority = chapterPriority,
        downloadChapters = false,
        mergeId = 100,
        mergeUrl = "merged",
        mangaId = mangaId,
        mangaUrl = "manga-$mangaId",
        mangaSourceId = mangaSourceId,
    )
}
