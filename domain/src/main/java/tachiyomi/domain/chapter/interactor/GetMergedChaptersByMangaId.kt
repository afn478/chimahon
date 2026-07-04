package tachiyomi.domain.chapter.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.chapter.service.transformMergedChapterList
import tachiyomi.domain.manga.interactor.GetMergedReferencesById

class GetMergedChaptersByMangaId(
    private val chapterRepository: ChapterRepository,
    private val getMergedReferencesById: GetMergedReferencesById,
) {

    suspend fun await(
        mangaId: Long,
        dedupe: Boolean = true,
        applyFilter: Boolean = false,
    ): List<Chapter> {
        return transformMergedChapterList(
            getMergedReferencesById.await(mangaId),
            getFromDatabase(mangaId, applyFilter),
            dedupe,
        )
    }

    suspend fun subscribe(
        mangaId: Long,
        dedupe: Boolean = true,
        applyFilter: Boolean = false,
    ): Flow<List<Chapter>> {
        return try {
            chapterRepository.getMergedChapterByMangaIdAsFlow(mangaId, applyFilter)
                .combine(getMergedReferencesById.subscribe(mangaId)) { chapters, references ->
                    transformMergedChapterList(references, chapters, dedupe)
                }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            flowOf(emptyList())
        }
    }

    private suspend fun getFromDatabase(
        mangaId: Long,
        applyFilter: Boolean = false,
    ): List<Chapter> {
        return try {
            chapterRepository.getMergedChapterByMangaId(mangaId, applyFilter)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }
}
