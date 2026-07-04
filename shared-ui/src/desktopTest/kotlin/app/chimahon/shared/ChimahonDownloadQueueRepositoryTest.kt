package app.chimahon.shared

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertEquals

class ChimahonDownloadQueueRepositoryTest {
    @Test
    fun claimNextPreservesActiveDownloadsAfterInitialRecovery() = runBlocking {
        val repository = ChimahonDownloadQueueRepository(InMemoryPlatformSettingsStore())
        repository.enqueue(
            listOf(
                downloadEntry(chapterId = 1L),
                downloadEntry(chapterId = 2L),
            ),
        )

        assertEquals(1L, repository.claimNext()?.chapterId)
        assertEquals(
            listOf(ChimahonDownloadState.Downloading, ChimahonDownloadState.Queued),
            repository.load().entries.map(ChimahonDownloadQueueEntry::status),
        )

        assertEquals(2L, repository.claimNext()?.chapterId)
        assertEquals(
            listOf(ChimahonDownloadState.Downloading, ChimahonDownloadState.Downloading),
            repository.load().entries.map(ChimahonDownloadQueueEntry::status),
        )
    }

    @Test
    fun newRepositoryInstanceRecoversPersistedActiveDownloads() = runBlocking {
        val settingsStore = InMemoryPlatformSettingsStore()
        val repository = ChimahonDownloadQueueRepository(settingsStore)
        repository.enqueue(listOf(downloadEntry(chapterId = 9L)))
        repository.claimNext()
        assertEquals(
            ChimahonDownloadState.Downloading,
            repository.load().entries.single().status,
        )

        val restartedRepository = ChimahonDownloadQueueRepository(settingsStore)

        assertEquals(
            ChimahonDownloadState.Queued,
            restartedRepository.load().entries.single().status,
        )
    }

    @Test
    fun loadRepairsMalformedDuplicateAndUnnormalizedPersistedEntries() = runBlocking {
        val settingsStore = InMemoryPlatformSettingsStore(
            mutableMapOf(
                DOWNLOAD_QUEUE_KEY to """
                    [
                      {
                        "id": "kept",
                        "mangaId": 101,
                        "chapterId": 1,
                        "sourceId": 7,
                        "mangaTitle": "Manga 1",
                        "chapterName": "Chapter 1",
                        "chapterUrl": "/chapter-1",
                        "status": "Downloading",
                        "progress": 250,
                        "downloadedBytes": -9,
                        "totalBytes": -3,
                        "addedAt": 11,
                        "errorMessage": null
                      },
                      {
                        "id": "duplicate",
                        "mangaId": 102,
                        "chapterId": 1,
                        "sourceId": 7,
                        "mangaTitle": "Duplicate",
                        "chapterName": "Duplicate",
                        "chapterUrl": "/duplicate",
                        "status": "Queued",
                        "progress": 10,
                        "downloadedBytes": 4,
                        "totalBytes": 8,
                        "addedAt": 12
                      },
                      {
                        "id": "malformed",
                        "chapterId": 2
                      }
                    ]
                """.trimIndent(),
            ),
        )
        val repository = ChimahonDownloadQueueRepository(settingsStore)

        val loaded = repository.load().entries

        assertEquals(1, loaded.size)
        val entry = loaded.single()
        assertEquals(1L, entry.chapterId)
        assertEquals(ChimahonDownloadState.Queued, entry.status)
        assertEquals(100, entry.progress)
        assertEquals(0L, entry.downloadedBytes)
        assertEquals(0L, entry.totalBytes)

        val repairedPayload = settingsStore.readString(DOWNLOAD_QUEUE_KEY).orEmpty()
        val repaired = Json.parseToJsonElement(repairedPayload) as JsonArray

        assertEquals(1, repaired.size)
        with(repaired.single().jsonObject) {
            assertEquals("kept", this["id"]?.jsonPrimitive?.content)
            assertEquals(1L, this["chapterId"]?.jsonPrimitive?.long)
            assertEquals("Queued", this["status"]?.jsonPrimitive?.content)
            assertEquals(100, this["progress"]?.jsonPrimitive?.int)
            assertEquals(0L, this["downloadedBytes"]?.jsonPrimitive?.long)
            assertEquals(0L, this["totalBytes"]?.jsonPrimitive?.long)
        }
    }

    private fun downloadEntry(chapterId: Long): ChimahonDownloadQueueEntry {
        return ChimahonDownloadQueueEntry(
            id = chapterId.toString(),
            mangaId = 100L + chapterId,
            chapterId = chapterId,
            sourceId = 1L,
            mangaTitle = "Manga $chapterId",
            chapterName = "Chapter $chapterId",
            chapterUrl = "/chapter-$chapterId",
            addedAt = chapterId,
        )
    }

    private companion object {
        const val DOWNLOAD_QUEUE_KEY = "__APP_STATE_download_queue"
    }
}
