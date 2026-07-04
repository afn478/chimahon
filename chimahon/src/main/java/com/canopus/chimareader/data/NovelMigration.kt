package com.canopus.chimareader.data

import android.content.Context
import android.util.Log
import tachiyomi.domain.library.service.NovelBookIdentityPolicy
import tachiyomi.domain.library.service.NovelBookMigrationPolicy
import tachiyomi.domain.library.service.NovelBookMergePolicy
import java.io.File

object NovelMigration {
    private const val TAG = "NovelMigration"

    fun migrateOldBooks(context: Context) {
        val prefs = context.getSharedPreferences("novel_sync_migration", Context.MODE_PRIVATE)
        val booksDir = BookStorage.getBooksDirectory(context)

        // v3: Fix old backup ghosts that used MD5(title) instead of MD5(title|author)
        if (!prefs.getBoolean("novel_migration_v3_done", false) && booksDir.exists()) {
            cleanupOldGhostDirs(booksDir)
            prefs.edit().putBoolean("novel_migration_v3_done", true).apply()
            Log.d(TAG, "Novel Migration v3 complete")
        }

        // v4: Merge duplicate dirs left behind by sync ghosts/imported books.
        if (!prefs.getBoolean("novel_migration_v4_dedupe_done", false) && booksDir.exists()) {
            cleanupDuplicateDirs(booksDir)
            prefs.edit().putBoolean("novel_migration_v4_dedupe_done", true).apply()
            Log.d(TAG, "Novel Migration v4 complete")
        }

        // v2: re-runs to handle books that had no metadata.json (skipped by v1)
        if (prefs.getBoolean("novel_migration_v2_done", false)) {
            return
        }

        Log.d(TAG, "Starting Novel Migration to stable IDs")

        if (!booksDir.exists()) {
            prefs.edit().putBoolean("novel_migration_v2_done", true).apply()
            return
        }

        val folders = booksDir.listFiles()?.filter { it.isDirectory } ?: emptyList()

        for (bookDir in folders) {
            val metadata = BookStorage.loadMetadata(bookDir)

            when {
                // Case 1: Already fully migrated (has metadata with hash)
                metadata != null && metadata.hash != null -> {
                    Log.d(TAG, "${bookDir.name} already migrated, skipping")
                }

                // Case 2: Has metadata.json but no hash yet
                metadata != null && metadata.hash == null -> {
                    try {
                        val book = BookStorage.loadEpub(bookDir)
                        val migration = NovelBookMigrationPolicy.migrateExistingMetadataWithoutHash(
                            existingMetadata = metadata,
                            parsedTitle = book.title,
                            parsedAuthor = book.author,
                            currentFolderName = bookDir.name,
                        )
                        val hash = migration.stableId
                        Log.d(
                            TAG,
                            "Migrating (has meta) ${bookDir.name} -> $hash (${migration.title} | ${migration.author})",
                        )

                        BookStorage.saveMetadata(migration.metadata, bookDir)

                        val newBookDir = File(booksDir, hash)
                        if (!newBookDir.exists()) {
                            if (!bookDir.renameTo(newBookDir)) {
                                Log.e(TAG, "Failed to rename folder from ${bookDir.name} to $hash")
                                BookStorage.saveMetadata(metadata.copy(hash = hash), bookDir)
                            } else {
                                Log.d(TAG, "Renamed folder successfully to $hash")
                            }
                        } else {
                            // Target already exists — merge this dir's data into it, then remove the duplicate
                            Log.w(TAG, "Target $hash already exists; merging ${bookDir.name} into it and removing duplicate")
                            mergeIntoTarget(sourceDir = bookDir, targetDir = newBookDir)
                            if (!bookDir.deleteRecursively()) {
                                Log.e(TAG, "Failed to delete duplicate source dir ${bookDir.name}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to migrate ${bookDir.name}: ${e.message}")
                    }
                }

                // Case 3: No metadata.json at all — parse EPUB to create it
                metadata == null -> {
                    try {
                        val book = BookStorage.loadEpub(bookDir)
                        val coverAbsPath = book.coverPath?.let { File(bookDir, it).absolutePath }
                        val migration = NovelBookMigrationPolicy.migrateParsedBookWithoutExistingMetadata(
                            parsedTitle = book.title,
                            parsedAuthor = book.author,
                            coverPath = coverAbsPath,
                            migrationTimeMillis = System.currentTimeMillis(),
                        )
                        val hash = migration.stableId
                        Log.d(
                            TAG,
                            "Migrating (no meta) ${bookDir.name} -> $hash (${migration.title} | ${migration.author})",
                        )

                        BookStorage.saveMetadata(migration.metadata, bookDir)

                        val newBookDir = File(booksDir, hash)
                        if (!newBookDir.exists()) {
                            if (!bookDir.renameTo(newBookDir)) {
                                Log.e(TAG, "Failed to rename folder from ${bookDir.name} to $hash")
                            } else {
                                Log.d(TAG, "Renamed folder successfully to $hash")
                            }
                        } else {
                            // Target already exists — merge this dir's data into it, then remove the duplicate
                            Log.w(TAG, "Target $hash already exists; merging ${bookDir.name} into it and removing duplicate")
                            mergeIntoTarget(sourceDir = bookDir, targetDir = newBookDir)
                            if (!bookDir.deleteRecursively()) {
                                Log.e(TAG, "Failed to delete duplicate source dir ${bookDir.name}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "No metadata and EPUB parse failed for ${bookDir.name}: ${e.message}")
                    }
                }
            }
        }

        prefs.edit().putBoolean("novel_migration_v2_done", true).apply()
        Log.d(TAG, "Novel Migration v2 complete")
    }

    private fun cleanupOldGhostDirs(booksDir: File) {
        val folders = booksDir.listFiles()?.filter { it.isDirectory } ?: return

        for (bookDir in folders) {
            val metadata = BookStorage.loadMetadata(bookDir) ?: continue
            val migration = NovelBookMigrationPolicy.correctStableDirectoryMetadata(
                metadata = metadata,
                currentFolderName = bookDir.name,
            ) ?: continue
            val correctId = migration.stableId

            if (bookDir.name == correctId) continue

            Log.d(
                TAG,
                "v3: fixing old ghost dir ${bookDir.name} -> $correctId (${migration.title} | ${migration.author})",
            )
            val newDir = File(booksDir, correctId)

            if (newDir.exists()) {
                mergeIntoTarget(sourceDir = bookDir, targetDir = newDir)
                if (!bookDir.deleteRecursively()) {
                    Log.e(TAG, "v3: failed to delete old ghost dir ${bookDir.name}")
                }
            } else {
                BookStorage.saveMetadata(migration.metadata, bookDir)
                if (!bookDir.renameTo(newDir)) {
                    Log.e(TAG, "v3: failed to rename ${bookDir.name} to $correctId")
                }
            }
        }
    }

    private fun cleanupDuplicateDirs(booksDir: File) {
        val folders = booksDir.listFiles()?.filter { it.isDirectory } ?: return
        val books = folders.mapNotNull { dir ->
            val metadata = BookStorage.loadMetadata(dir) ?: return@mapNotNull null
            MigrationBookDir(dir, metadata)
        }

        books.groupBy { BookStorage.bookIdentityKey(it.metadata) }
            .filterValues { it.size > 1 }
            .forEach { (identity, duplicates) ->
                val target = NovelBookIdentityPolicy.selectPreferredDuplicate(
                    books = duplicates,
                    identityKey = identity,
                    isGhost = { it.metadata.isGhost },
                    hasImportedContent = { BookStorage.hasImportedBookContent(it.directory) },
                    folderName = { it.directory.name },
                    lastAccess = { it.metadata.lastAccess },
                ) ?: return@forEach

                duplicates
                    .filterNot { it.directory == target.directory }
                    .forEach { duplicate ->
                        Log.d(TAG, "v4: merging duplicate ${duplicate.directory.name} into ${target.directory.name}")
                        mergeIntoTarget(sourceDir = duplicate.directory, targetDir = target.directory)
                        if (!duplicate.directory.deleteRecursively()) {
                            Log.e(TAG, "v4: failed to delete duplicate dir ${duplicate.directory.name}")
                        }
                    }
            }
    }

    /**
     * Merges content and progress data from [sourceDir] into [targetDir], keeping whichever
     * version of each user-progress record is newer.
     */
    private fun mergeIntoTarget(sourceDir: File, targetDir: File) {
        val sourceMetadata = BookStorage.loadMetadata(sourceDir)
        val targetMetadata = BookStorage.loadMetadata(targetDir)
        val sourceBookmark = BookStorage.loadBookmark(sourceDir)
        val targetBookmark = BookStorage.loadBookmark(targetDir)

        val mergedBookmark = NovelBookMergePolicy.selectLatestOrNull(
            current = targetBookmark,
            incoming = sourceBookmark,
            lastModified = { it.lastModified ?: 0L },
        )

        val sourceStats = BookStorage.loadStatistics(sourceDir) ?: emptyList()
        val targetStats = BookStorage.loadStatistics(targetDir) ?: emptyList()
        val mergedStats = NovelBookMergePolicy.mergeLatestByKey(
            items = targetStats + sourceStats,
            itemKey = Statistics::dateKey,
            lastModified = Statistics::lastStatisticModified,
        )
        val statsChanged = mergedStats != targetStats

        val sourceHasContent = BookStorage.hasImportedBookContent(sourceDir)
        val targetHasContent = BookStorage.hasImportedBookContent(targetDir)
        var contentCopied = false
        if (sourceHasContent && !targetHasContent) {
            sourceDir.copyRecursively(targetDir, overwrite = true)
            contentCopied = true
            Log.d(TAG, "mergeIntoTarget: copied EPUB content from source")
        }

        mergeMetadata(sourceMetadata, targetMetadata, targetDir)?.let {
            BookStorage.saveMetadata(it, targetDir)
        }
        mergedBookmark?.let {
            BookStorage.saveBookmark(it, targetDir)
            Log.d(TAG, "mergeIntoTarget: saved merged bookmark")
        }
        if (statsChanged || contentCopied) {
            BookStorage.saveStatistics(mergedStats, targetDir)
            Log.d(TAG, "mergeIntoTarget: merged ${sourceStats.size} source stat entries")
        }
    }

    private fun mergeMetadata(
        sourceMetadata: BookMetadata?,
        targetMetadata: BookMetadata?,
        targetDir: File,
    ): BookMetadata? {
        return NovelBookMergePolicy.mergeMetadata(
            sourceMetadata = sourceMetadata,
            targetMetadata = targetMetadata,
            targetFolderName = targetDir.name,
            targetHasImportedContent = BookStorage.hasImportedBookContent(targetDir),
            uncategorizedCategoryId = NovelCategory.UNCATEGORIZED_ID,
            identityKey = BookStorage::bookIdentityKey,
        )
    }

    private data class MigrationBookDir(
        val directory: File,
        val metadata: BookMetadata,
    )

}
