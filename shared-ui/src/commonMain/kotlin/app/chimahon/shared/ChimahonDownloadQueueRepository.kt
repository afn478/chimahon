package app.chimahon.shared

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import tachiyomi.core.platform.settings.PlatformSettingsStore
import tachiyomi.core.platform.settings.readBoolean
import tachiyomi.core.platform.settings.writeBoolean

internal class ChimahonDownloadQueueRepository(
    private val settingsStore: PlatformSettingsStore,
) {
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private var activeDownloadsRecovered = false

    suspend fun load(): ChimahonDownloadQueueData {
        return mutex.withLock {
            loadUnlocked()
        }
    }

    suspend fun enqueue(entries: List<ChimahonDownloadQueueEntry>): ChimahonDownloadQueueData {
        return mutex.withLock {
            val current = loadEntriesUnlocked().associateByTo(linkedMapOf(), ChimahonDownloadQueueEntry::chapterId)
            entries.forEach { entry ->
                val existing = current[entry.chapterId]
                current[entry.chapterId] = when (existing?.status) {
                    ChimahonDownloadState.Downloaded,
                    ChimahonDownloadState.Downloading,
                    ChimahonDownloadState.Queued,
                    ChimahonDownloadState.Paused,
                    -> existing
                    ChimahonDownloadState.Error -> existing.copy(
                        status = ChimahonDownloadState.Queued,
                        errorMessage = null,
                    )
                    null -> entry
                }
            }
            persistEntriesUnlocked(current.values.toList())
            loadUnlocked()
        }
    }

    suspend fun remove(chapterIds: Set<Long>): ChimahonDownloadQueueData {
        return mutex.withLock {
            persistEntriesUnlocked(loadEntriesUnlocked().filterNot { it.chapterId in chapterIds })
            persistIndividuallyPausedChapterIdsUnlocked(
                loadIndividuallyPausedChapterIdsUnlocked() - chapterIds,
            )
            loadUnlocked()
        }
    }

    suspend fun clear(completedOnly: Boolean): ChimahonDownloadQueueData {
        return mutex.withLock {
            val remaining = if (completedOnly) {
                loadEntriesUnlocked().filterNot { it.status == ChimahonDownloadState.Downloaded }
            } else {
                emptyList()
            }
            persistEntriesUnlocked(remaining)
            val remainingIds = remaining.mapTo(mutableSetOf(), ChimahonDownloadQueueEntry::chapterId)
            persistIndividuallyPausedChapterIdsUnlocked(
                loadIndividuallyPausedChapterIdsUnlocked().intersect(remainingIds),
            )
            loadUnlocked()
        }
    }

    suspend fun reorder(chapterIds: List<Long>): ChimahonDownloadQueueData {
        return mutex.withLock {
            val entries = loadEntriesUnlocked()
            val requestedIds = chapterIds.toSet()
            val requestedEntries = chapterIds
                .distinct()
                .mapNotNull(entries.associateBy(ChimahonDownloadQueueEntry::chapterId)::get)
                .iterator()
            val reordered = entries.map { entry ->
                if (entry.chapterId in requestedIds && requestedEntries.hasNext()) {
                    requestedEntries.next()
                } else {
                    entry
                }
            }
            persistEntriesUnlocked(reordered)
            loadUnlocked()
        }
    }

    suspend fun moveToTop(chapterId: Long): ChimahonDownloadQueueData {
        return mutex.withLock {
            val entries = loadEntriesUnlocked()
            val entry = entries.firstOrNull { it.chapterId == chapterId }
                ?: return@withLock loadUnlocked()
            persistEntriesUnlocked(listOf(entry) + entries.filterNot { it.chapterId == chapterId })
            loadUnlocked()
        }
    }

    suspend fun moveToBottom(chapterId: Long): ChimahonDownloadQueueData {
        return mutex.withLock {
            val entries = loadEntriesUnlocked()
            val entry = entries.firstOrNull { it.chapterId == chapterId }
                ?: return@withLock loadUnlocked()
            persistEntriesUnlocked(entries.filterNot { it.chapterId == chapterId } + entry)
            loadUnlocked()
        }
    }

    suspend fun setPaused(paused: Boolean): ChimahonDownloadQueueData {
        return mutex.withLock {
            settingsStore.writeBoolean(DOWNLOAD_QUEUE_PAUSED_KEY, paused)
            val individuallyPaused = loadIndividuallyPausedChapterIdsUnlocked()
            val entries = loadEntriesUnlocked().map { entry ->
                when {
                    paused && entry.status == ChimahonDownloadState.Queued -> {
                        entry.copy(status = ChimahonDownloadState.Paused)
                    }
                    !paused &&
                        entry.status == ChimahonDownloadState.Paused &&
                        entry.chapterId !in individuallyPaused -> {
                        entry.copy(status = ChimahonDownloadState.Queued)
                    }
                    else -> entry
                }
            }
            persistEntriesUnlocked(entries)
            loadUnlocked()
        }
    }

    suspend fun pause(chapterId: Long): ChimahonDownloadQueueData {
        return mutex.withLock {
            var pausedEntry = false
            val entries = loadEntriesUnlocked().map { entry ->
                if (
                    entry.chapterId == chapterId &&
                    entry.status != ChimahonDownloadState.Downloaded &&
                    entry.status != ChimahonDownloadState.Error
                ) {
                    pausedEntry = true
                    entry.copy(status = ChimahonDownloadState.Paused)
                } else {
                    entry
                }
            }
            if (pausedEntry) {
                persistIndividuallyPausedChapterIdsUnlocked(
                    loadIndividuallyPausedChapterIdsUnlocked() + chapterId,
                )
            }
            persistEntriesUnlocked(entries)
            loadUnlocked()
        }
    }

    suspend fun resume(chapterId: Long): ChimahonDownloadQueueData {
        return mutex.withLock {
            val queuePaused = settingsStore.readBoolean(DOWNLOAD_QUEUE_PAUSED_KEY)
            persistIndividuallyPausedChapterIdsUnlocked(
                loadIndividuallyPausedChapterIdsUnlocked() - chapterId,
            )
            val entries = loadEntriesUnlocked().map { entry ->
                if (
                    !queuePaused &&
                    entry.chapterId == chapterId &&
                    entry.status == ChimahonDownloadState.Paused
                ) {
                    entry.copy(
                        status = ChimahonDownloadState.Queued,
                        errorMessage = null,
                    )
                } else {
                    entry
                }
            }
            persistEntriesUnlocked(entries)
            loadUnlocked()
        }
    }

    suspend fun retry(chapterId: Long): ChimahonDownloadQueueData {
        return mutex.withLock {
            val retryState = if (settingsStore.readBoolean(DOWNLOAD_QUEUE_PAUSED_KEY)) {
                ChimahonDownloadState.Paused
            } else {
                ChimahonDownloadState.Queued
            }
            persistIndividuallyPausedChapterIdsUnlocked(
                loadIndividuallyPausedChapterIdsUnlocked() - chapterId,
            )
            val entries = loadEntriesUnlocked().map { entry ->
                if (
                    entry.chapterId == chapterId &&
                    entry.status != ChimahonDownloadState.Downloaded
                ) {
                    entry.copy(
                        status = retryState,
                        progress = 0,
                        downloadedBytes = 0L,
                        totalBytes = null,
                        errorMessage = null,
                    )
                } else {
                    entry
                }
            }
            persistEntriesUnlocked(entries)
            loadUnlocked()
        }
    }

    suspend fun update(
        chapterId: Long,
        transform: (ChimahonDownloadQueueEntry) -> ChimahonDownloadQueueEntry,
    ): ChimahonDownloadQueueData {
        return mutex.withLock {
            val entries = loadEntriesUnlocked().map { entry ->
                if (entry.chapterId == chapterId) transform(entry).normalized() else entry
            }
            persistEntriesUnlocked(entries)
            loadUnlocked()
        }
    }

    suspend fun claimNext(): ChimahonDownloadQueueEntry? {
        return mutex.withLock {
            if (settingsStore.readBoolean(DOWNLOAD_QUEUE_PAUSED_KEY)) return@withLock null
            val entries = loadEntriesUnlocked().toMutableList()
            val index = entries.indexOfFirst { it.status == ChimahonDownloadState.Queued }
            if (index < 0) return@withLock null
            val claimed = entries[index].copy(
                status = ChimahonDownloadState.Downloading,
                errorMessage = null,
            )
            entries[index] = claimed
            persistEntriesUnlocked(entries)
            claimed
        }
    }

    private suspend fun loadUnlocked(): ChimahonDownloadQueueData {
        return ChimahonDownloadQueueData(
            entries = loadEntriesUnlocked(),
            paused = settingsStore.readBoolean(DOWNLOAD_QUEUE_PAUSED_KEY),
        )
    }

    private suspend fun loadEntriesUnlocked(): List<ChimahonDownloadQueueEntry> {
        val persisted = loadPersistedEntriesUnlocked()
        val entries = persisted.entries
        if (activeDownloadsRecovered) {
            if (persisted.needsRepair) {
                persistEntriesUnlocked(entries)
            }
            return entries
        }

        activeDownloadsRecovered = true
        val recoveredEntries = entries.map { entry ->
            if (entry.status == ChimahonDownloadState.Downloading) {
                entry.copy(status = ChimahonDownloadState.Queued)
            } else {
                entry
            }
        }
        if (persisted.needsRepair || recoveredEntries != entries) {
            persistEntriesUnlocked(recoveredEntries)
        }
        return recoveredEntries
    }

    private suspend fun loadPersistedEntriesUnlocked(): PersistedDownloadQueueEntries {
        val payload = settingsStore.readString(DOWNLOAD_QUEUE_KEY)
            ?: return PersistedDownloadQueueEntries(emptyList(), needsRepair = false)
        val array = runCatching { json.parseToJsonElement(payload) as? JsonArray }
            .getOrNull()
            ?: return PersistedDownloadQueueEntries(emptyList(), needsRepair = true)
        var droppedMalformedEntry = false
        val decodedEntries = array.mapNotNull { element ->
            runCatching { element.jsonObject.toQueueEntry() }
                .onFailure { droppedMalformedEntry = true }
                .getOrNull()
        }
        val normalizedEntries = decodedEntries.map { entry -> entry.normalized() }
        val repairedEntries = normalizedEntries.distinctBy(ChimahonDownloadQueueEntry::chapterId)
        return PersistedDownloadQueueEntries(
            entries = repairedEntries,
            needsRepair = droppedMalformedEntry ||
                decodedEntries != normalizedEntries ||
                normalizedEntries.size != repairedEntries.size,
        )
    }

    private suspend fun persistEntriesUnlocked(entries: List<ChimahonDownloadQueueEntry>) {
        val payload = buildJsonArray {
            entries.forEach { entry ->
                add(entry.normalized().toJsonObject())
            }
        }.toString()
        settingsStore.writeString(DOWNLOAD_QUEUE_KEY, payload)
    }

    private suspend fun loadIndividuallyPausedChapterIdsUnlocked(): Set<Long> {
        val payload = settingsStore.readString(INDIVIDUALLY_PAUSED_DOWNLOADS_KEY)
            ?: return emptySet()
        val array = runCatching { json.parseToJsonElement(payload) as? JsonArray }
            .getOrNull()
            ?: return emptySet()
        return array.mapNotNullTo(mutableSetOf()) { element ->
            runCatching { element.jsonPrimitive.longOrNull }.getOrNull()
        }
    }

    private suspend fun persistIndividuallyPausedChapterIdsUnlocked(chapterIds: Set<Long>) {
        val payload = buildJsonArray {
            chapterIds.sorted().forEach { chapterId ->
                add(JsonPrimitive(chapterId))
            }
        }.toString()
        settingsStore.writeString(INDIVIDUALLY_PAUSED_DOWNLOADS_KEY, payload)
    }

    private fun ChimahonDownloadQueueEntry.normalized(): ChimahonDownloadQueueEntry {
        return copy(
            progress = progress.coerceIn(0, 100),
            downloadedBytes = downloadedBytes.coerceAtLeast(0L),
            totalBytes = totalBytes?.coerceAtLeast(0L),
        )
    }

    private fun ChimahonDownloadQueueEntry.toJsonObject(): JsonObject {
        return buildJsonObject {
            put("id", JsonPrimitive(id))
            put("mangaId", JsonPrimitive(mangaId))
            put("chapterId", JsonPrimitive(chapterId))
            put("sourceId", JsonPrimitive(sourceId))
            put("mangaTitle", JsonPrimitive(mangaTitle))
            put("chapterName", JsonPrimitive(chapterName))
            put("chapterUrl", JsonPrimitive(chapterUrl))
            put("status", JsonPrimitive(status.name))
            put("progress", JsonPrimitive(progress))
            put("downloadedBytes", JsonPrimitive(downloadedBytes))
            put("totalBytes", totalBytes?.let(::JsonPrimitive) ?: JsonNull)
            put("addedAt", JsonPrimitive(addedAt))
            put("errorMessage", errorMessage?.let(::JsonPrimitive) ?: JsonNull)
        }
    }

    private fun JsonObject.toQueueEntry(): ChimahonDownloadQueueEntry {
        val chapterId = requiredLong("chapterId")
        return ChimahonDownloadQueueEntry(
            id = string("id") ?: chapterId.toString(),
            mangaId = requiredLong("mangaId"),
            chapterId = chapterId,
            sourceId = requiredLong("sourceId"),
            mangaTitle = string("mangaTitle").orEmpty(),
            chapterName = string("chapterName").orEmpty(),
            chapterUrl = string("chapterUrl").orEmpty(),
            status = string("status")
                ?.let { stored -> ChimahonDownloadState.entries.firstOrNull { it.name == stored } }
                ?: ChimahonDownloadState.Queued,
            progress = this["progress"]?.jsonPrimitive?.intOrNull ?: 0,
            downloadedBytes = this["downloadedBytes"]?.jsonPrimitive?.longOrNull ?: 0L,
            totalBytes = this["totalBytes"]?.jsonPrimitive?.longOrNull,
            addedAt = this["addedAt"]?.jsonPrimitive?.longOrNull ?: 0L,
            errorMessage = string("errorMessage"),
        )
    }

    private fun JsonObject.requiredLong(key: String): Long {
        return this[key]?.jsonPrimitive?.longOrNull
            ?: error("Download queue entry is missing $key.")
    }

    private fun JsonObject.string(key: String): String? {
        val value = this[key] ?: return null
        if (value is JsonNull) return null
        return value.jsonPrimitive.content
    }

    private companion object {
        const val DOWNLOAD_QUEUE_KEY = "__APP_STATE_download_queue"
        const val DOWNLOAD_QUEUE_PAUSED_KEY = "__APP_STATE_download_queue_paused"
        const val INDIVIDUALLY_PAUSED_DOWNLOADS_KEY =
            "__APP_STATE_individually_paused_downloads"
    }
}

private data class PersistedDownloadQueueEntries(
    val entries: List<ChimahonDownloadQueueEntry>,
    val needsRepair: Boolean,
)
