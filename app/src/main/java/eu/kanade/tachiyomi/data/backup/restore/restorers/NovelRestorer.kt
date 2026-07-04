package eu.kanade.tachiyomi.data.backup.restore.restorers

import android.content.Context
import com.canopus.chimareader.data.BookMetadata
import com.canopus.chimareader.data.BookStorage
import com.canopus.chimareader.data.Bookmark
import com.canopus.chimareader.data.NovelCategory
import com.canopus.chimareader.data.Statistics
import com.canopus.chimareader.data.md5Hex
import eu.kanade.tachiyomi.data.backup.models.BackupNovel
import eu.kanade.tachiyomi.data.backup.models.BackupNovelCategory
import eu.kanade.tachiyomi.data.backup.models.BackupStatEntry
import tachiyomi.domain.library.service.NovelBookIdentityPolicy
import tachiyomi.domain.library.service.NovelBookMergePolicy
import tachiyomi.domain.library.service.NovelCategoryPolicy
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class NovelRestorer(
    private val context: Context,
    private val novelCategoryStorage: com.canopus.chimareader.data.NovelCategoryStorage = Injekt.get()
) {

    fun restore(
        backupNovels: List<BackupNovel>,
        categoryIdMap: Map<String, String> = emptyMap(),
    ) {
        backupNovels.forEach { restoreNovel(it, categoryIdMap) }
    }

    fun restoreNovel(
        backupNovel: BackupNovel,
        categoryIdMap: Map<String, String> = emptyMap(),
    ) {
        val novelId = stableNovelId(backupNovel)
        val bookDir = BookStorage.getBookDirectory(context, novelId)
        val backupCategoryIds = normalizeCategoryIds(
            backupNovel.categoryIds.map { categoryIdMap[it] ?: it },
        )

        if (bookDir.exists()) {
            // Merge Metadata
            val localMetadata = BookStorage.loadMetadata(bookDir)
            if (localMetadata != null) {
                val hasImportedContent = BookStorage.hasImportedBookContent(bookDir)
                val updatedMetadata = localMetadata.copy(
                    author = backupNovel.author ?: localMetadata.author,
                    cover = backupNovel.cover ?: localMetadata.cover,
                    lang = backupNovel.lang ?: localMetadata.lang,
                    isGhost = if (hasImportedContent) false else localMetadata.isGhost,
                    categoryIds = mergeCategoryIds(localMetadata.categoryIds, backupCategoryIds)
                )
                BookStorage.saveMetadata(updatedMetadata, bookDir)
            }

            // Merge Bookmark
            val localBookmark = BookStorage.loadBookmark(bookDir)
            val backupBookmark = backupNovel.toBookmark()
            val mergedBookmark = NovelBookMergePolicy.selectLatestOrNull(
                current = localBookmark,
                incoming = backupBookmark,
                lastModified = { it.lastModified ?: 0L },
            )
            if (mergedBookmark != null && mergedBookmark != localBookmark) {
                BookStorage.saveBookmark(mergedBookmark, bookDir)
            }

            // Merge Statistics
            val localStats = BookStorage.loadStatistics(bookDir).orEmpty()
            val backupStats = backupNovel.stats.map { it.toStatistics(backupNovel.title) }
            val mergedStats = mergeStats(localStats, backupStats)
            if (mergedStats != localStats) {
                BookStorage.saveStatistics(mergedStats, bookDir)
            }
        } else {
            // Ghost book
            bookDir.mkdirs()

            val metadata = BookMetadata(
                id = novelId,
                title = backupNovel.title,
                author = backupNovel.author,
                cover = backupNovel.cover, // Might be broken link until EPUB import
                folder = novelId,
                lastAccess = backupNovel.lastModified,
                hash = novelId,
                isGhost = true,
                lang = backupNovel.lang,
                categoryIds = backupCategoryIds
            )
            BookStorage.saveMetadata(metadata, bookDir)

            // Bookmark
            if (backupNovel.lastModified > 0) {
                BookStorage.saveBookmark(backupNovel.toBookmark(), bookDir)
            }

            // Statistics
            if (backupNovel.stats.isNotEmpty()) {
                val stats = backupNovel.stats.map { it.toStatistics(backupNovel.title) }
                BookStorage.saveStatistics(stats, bookDir)
            }
        }
    }

    fun restoreCategories(backupCategories: List<BackupNovelCategory>): Map<String, String> {
        if (backupCategories.isEmpty()) {
            return mapOf(NovelCategory.UNCATEGORIZED_ID to NovelCategory.UNCATEGORIZED_ID)
        }

        val result = NovelCategoryPolicy.restoreCategories(
            currentCategories = novelCategoryStorage.loadAllCategories(),
            backupCategories = backupCategories,
            uncategorizedCategoryId = NovelCategory.UNCATEGORIZED_ID,
            currentCategoryId = NovelCategory::id,
            currentCategoryName = NovelCategory::name,
            backupCategoryId = BackupNovelCategory::id,
            backupCategoryName = BackupNovelCategory::name,
            createCategory = { backupCategory ->
                NovelCategory(
                    id = backupCategory.id,
                    name = backupCategory.name,
                    order = backupCategory.order.toInt(),
                    flags = backupCategory.flags,
                )
            },
            updateCategory = { existing, backupCategory ->
                existing.copy(
                    name = backupCategory.name,
                    order = backupCategory.order.toInt(),
                    flags = backupCategory.flags,
                )
            },
        )

        if (result.changed) {
            novelCategoryStorage.saveCategories(result.categories)
        }

        return result.categoryIdMap
    }

    private fun mergeCategoryIds(localIds: List<String>, backupIds: List<String>): List<String> {
        return NovelBookMergePolicy.mergeCategoryIds(
            currentCategoryIds = localIds,
            incomingCategoryIds = backupIds,
            uncategorizedCategoryId = NovelCategory.UNCATEGORIZED_ID,
        )
    }

    private fun normalizeCategoryIds(categoryIds: List<String>): List<String> {
        return NovelCategoryPolicy.normalizeCategoryIds(
            categoryIds = categoryIds,
            uncategorizedCategoryId = NovelCategory.UNCATEGORIZED_ID,
        )
    }

    private fun stableNovelId(backupNovel: BackupNovel): String {
        return NovelBookIdentityPolicy.identityKey(
            title = backupNovel.title,
            author = backupNovel.author,
            storedHash = null,
            fallbackId = backupNovel.id,
            hashIdentity = ::md5Hex,
        )
    }

    private fun BackupNovel.toBookmark(): Bookmark {
        return Bookmark(
            chapterIndex = chapterIndex,
            progress = progress,
            characterCount = characterCount,
            lastModified = lastModified,
        )
    }

    private fun BackupStatEntry.toStatistics(title: String): Statistics {
        return Statistics(
            title = title,
            dateKey = dateKey,
            charactersRead = charactersRead,
            readingTime = readingTime,
            minReadingSpeed = minReadingSpeed,
            altMinReadingSpeed = altMinReadingSpeed,
            lastReadingSpeed = lastReadingSpeed,
            maxReadingSpeed = maxReadingSpeed,
            lastStatisticModified = lastStatisticModified,
        )
    }

    private fun mergeStats(
        localStats: List<Statistics>,
        backupStats: List<Statistics>,
    ): List<Statistics> {
        return NovelBookMergePolicy.mergeLatestByKey(
            items = localStats + backupStats,
            itemKey = Statistics::dateKey,
            lastModified = Statistics::lastStatisticModified,
        )
    }
}
