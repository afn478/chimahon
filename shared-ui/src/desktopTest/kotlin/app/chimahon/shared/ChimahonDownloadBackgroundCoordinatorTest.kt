package app.chimahon.shared

import kotlinx.coroutines.runBlocking
import tachiyomi.core.platform.background.BackgroundTask
import tachiyomi.core.platform.background.BackgroundTaskInfo
import tachiyomi.core.platform.background.BackgroundTaskInputData
import tachiyomi.core.platform.background.BackgroundTaskResult
import tachiyomi.core.platform.background.BackgroundTaskScheduler
import tachiyomi.core.platform.background.BackgroundWorkerRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChimahonDownloadBackgroundCoordinatorTest {
    @Test
    fun schedulesRunnableDownloadQueue() {
        val scheduler = RecordingBackgroundTaskScheduler()
        var registry: BackgroundWorkerRegistry? = null
        val coordinator = ChimahonDownloadBackgroundCoordinator(
            schedulerFactory = { workerRegistry ->
                registry = workerRegistry
                scheduler
            },
            loadQueue = { queueOf(downloadEntry(status = ChimahonDownloadState.Queued)) },
            processNextDownload = { queueOf(downloadEntry(status = ChimahonDownloadState.Downloaded)) },
        )

        coordinator.scheduleIfNeeded(queueOf(downloadEntry(status = ChimahonDownloadState.Queued)))

        val task = scheduler.scheduledTasks.single()
        assertEquals(ChimahonDownloadBackgroundCoordinator.DOWNLOAD_QUEUE_TASK_NAME, task.uniqueName)
        assertEquals(ChimahonDownloadBackgroundCoordinator.DOWNLOAD_QUEUE_WORKER_KEY, task.workerKey)
        assertNotNull(registry?.worker(task.workerKey))
        assertTrue(coordinator.stateLabel("Coroutine scheduler ready").contains("scheduled"))
    }

    @Test
    fun skipsPausedAndEmptyQueues() {
        val scheduler = RecordingBackgroundTaskScheduler()
        val coordinator = ChimahonDownloadBackgroundCoordinator(
            schedulerFactory = { scheduler },
            loadQueue = { queueOf() },
            processNextDownload = { queueOf() },
        )

        coordinator.scheduleIfNeeded(
            queueOf(
                downloadEntry(status = ChimahonDownloadState.Queued),
                paused = true,
            ),
        )
        coordinator.scheduleIfNeeded(queueOf(downloadEntry(status = ChimahonDownloadState.Downloaded)))
        coordinator.scheduleIfNeeded(queueOf())

        assertTrue(scheduler.scheduledTasks.isEmpty())
        assertTrue(coordinator.stateLabel("Coroutine scheduler ready").contains("idle"))
    }

    @Test
    fun workerDrainsQueuedDownloads() = runBlocking {
        val scheduler = RecordingBackgroundTaskScheduler()
        var registry: BackgroundWorkerRegistry? = null
        var queue = queueOf(
            downloadEntry(chapterId = 1L, status = ChimahonDownloadState.Queued),
            downloadEntry(chapterId = 2L, status = ChimahonDownloadState.Queued),
        )
        var processedCount = 0
        ChimahonDownloadBackgroundCoordinator(
            schedulerFactory = { workerRegistry ->
                registry = workerRegistry
                scheduler
            },
            loadQueue = { queue },
            processNextDownload = {
                processedCount += 1
                val next = queue.entries.first { it.status == ChimahonDownloadState.Queued }
                queue = queue.copy(
                    entries = queue.entries.map { entry ->
                        if (entry.chapterId == next.chapterId) {
                            entry.copy(status = ChimahonDownloadState.Downloaded)
                        } else {
                            entry
                        }
                    },
                )
                queue
            },
        )

        val worker = registry?.worker(ChimahonDownloadBackgroundCoordinator.DOWNLOAD_QUEUE_WORKER_KEY)
        assertNotNull(worker)

        assertEquals(BackgroundTaskResult.Success, worker.run(BackgroundTaskInputData.Empty))
        assertEquals(2, processedCount)
        assertFalse(queue.entries.any { it.status == ChimahonDownloadState.Queued })
    }

    private fun queueOf(
        vararg entries: ChimahonDownloadQueueEntry,
        paused: Boolean = false,
    ): ChimahonDownloadQueueData {
        return ChimahonDownloadQueueData(
            entries = entries.toList(),
            paused = paused,
        )
    }

    private fun downloadEntry(
        chapterId: Long = 1L,
        status: ChimahonDownloadState,
    ): ChimahonDownloadQueueEntry {
        return ChimahonDownloadQueueEntry(
            id = chapterId.toString(),
            mangaId = 100L + chapterId,
            chapterId = chapterId,
            sourceId = 200L,
            mangaTitle = "Manga $chapterId",
            chapterName = "Chapter $chapterId",
            chapterUrl = "/chapter-$chapterId",
            status = status,
            addedAt = chapterId,
        )
    }
}

private class RecordingBackgroundTaskScheduler : BackgroundTaskScheduler {
    val scheduledTasks = mutableListOf<BackgroundTask>()

    override fun schedule(task: BackgroundTask) {
        scheduledTasks += task
    }

    override fun cancel(uniqueName: String) {
        scheduledTasks.removeAll { it.uniqueName == uniqueName }
    }

    override fun isScheduled(uniqueName: String): Boolean {
        return scheduledTasks.any { it.uniqueName == uniqueName }
    }

    override fun cancelTask(task: BackgroundTaskInfo) {
        cancel(task.uniqueName)
    }
}
