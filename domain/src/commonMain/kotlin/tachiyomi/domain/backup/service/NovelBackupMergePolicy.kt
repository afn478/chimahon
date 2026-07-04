package tachiyomi.domain.backup.service

import tachiyomi.domain.backup.model.NovelBackupRecord
import tachiyomi.domain.backup.model.NovelBackupStatistic
import tachiyomi.domain.library.model.NovelBookMetadata
import tachiyomi.domain.library.service.NovelBookMergePolicy

object NovelBackupMergePolicy {
    fun <Stat : NovelBackupStatistic, Record : NovelBackupRecord<Stat>> mergeDuplicateRecord(
        first: Record,
        second: Record,
        uncategorizedCategoryId: String,
        copyRecord: (
            base: Record,
            id: String,
            author: String?,
            cover: String?,
            stats: List<Stat>,
            categoryIds: List<String>,
            lang: String?,
        ) -> Record,
    ): Record {
        val latest = NovelBookMergePolicy.selectLatest(
            current = first,
            incoming = second,
            lastModified = NovelBackupRecord<Stat>::lastModified,
        )
        val fallback = if (latest == first) second else first

        return copyRecord(
            latest,
            first.id,
            latest.author ?: fallback.author,
            latest.cover ?: fallback.cover,
            mergeStats(first.stats + second.stats),
            NovelBookMergePolicy.mergeCategoryIds(
                currentCategoryIds = first.categoryIds,
                incomingCategoryIds = second.categoryIds,
                uncategorizedCategoryId = uncategorizedCategoryId,
            ),
            latest.lang ?: fallback.lang,
        )
    }

    fun <Stat : NovelBackupStatistic> mergeStats(stats: List<Stat>): List<Stat> {
        return NovelBookMergePolicy.mergeLatestByKey(
            items = stats,
            itemKey = { it.dateKey },
            lastModified = { it.lastStatisticModified },
        )
    }

    fun mergeRestoredMetadata(
        localMetadata: NovelBookMetadata,
        backupAuthor: String?,
        backupCover: String?,
        backupLang: String?,
        backupCategoryIds: Collection<String>,
        hasImportedContent: Boolean,
        uncategorizedCategoryId: String,
    ): NovelBookMetadata {
        return localMetadata.copy(
            author = backupAuthor ?: localMetadata.author,
            cover = backupCover ?: localMetadata.cover,
            lang = backupLang ?: localMetadata.lang,
            isGhost = if (hasImportedContent) false else localMetadata.isGhost,
            categoryIds = NovelBookMergePolicy.mergeCategoryIds(
                currentCategoryIds = localMetadata.categoryIds,
                incomingCategoryIds = backupCategoryIds,
                uncategorizedCategoryId = uncategorizedCategoryId,
            ),
        )
    }
}
