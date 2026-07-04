package app.chimahon.shared

import kotlinx.coroutines.CancellationException
import tachiyomi.core.platform.background.BackgroundNetworkConstraint
import tachiyomi.core.platform.background.BackgroundTask
import tachiyomi.core.platform.background.BackgroundTaskBackoffCriteria
import tachiyomi.core.platform.background.BackgroundTaskBackoffPolicy
import tachiyomi.core.platform.background.BackgroundTaskCadence
import tachiyomi.core.platform.background.BackgroundTaskConstraints
import tachiyomi.core.platform.background.BackgroundTaskResult
import tachiyomi.core.platform.background.BackgroundTaskScheduler
import tachiyomi.core.platform.background.BackgroundWorker
import tachiyomi.core.platform.background.BackgroundWorkerRegistry
import tachiyomi.core.platform.background.ExistingBackgroundTaskPolicy
import kotlin.time.Duration.Companion.seconds

internal class ChimahonDownloadBackgroundCoordinator(
    schedulerFactory: (BackgroundWorkerRegistry) -> BackgroundTaskScheduler,
    private val loadQueue: suspend () -> ChimahonDownloadQueueData,
    private val processNextDownload: suspend () -> ChimahonDownloadQueueData,
) {
    private val scheduler = schedulerFactory(
        BackgroundWorkerRegistry { workerKey ->
            when (workerKey) {
                DOWNLOAD_QUEUE_WORKER_KEY -> BackgroundWorker { drainQueue() }
                else -> null
            }
        },
    )

    fun scheduleIfNeeded(queue: ChimahonDownloadQueueData) {
        if (queue.hasRunnableDownloads()) {
            scheduler.schedule(downloadQueueTask())
        }
    }

    fun stateLabel(platformState: String): String {
        val workerState = if (scheduler.isScheduled(DOWNLOAD_QUEUE_TASK_NAME)) {
            "download worker scheduled"
        } else {
            "download worker idle"
        }
        return "$platformState; $workerState"
    }

    private suspend fun drainQueue(): BackgroundTaskResult {
        return try {
            var queue = loadQueue()
            while (queue.hasRunnableDownloads()) {
                queue = processNextDownload()
            }
            BackgroundTaskResult.Success
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            BackgroundTaskResult.Retry
        }
    }

    internal companion object {
        const val DOWNLOAD_QUEUE_TASK_NAME = "chimahon.download.queue"
        const val DOWNLOAD_QUEUE_WORKER_KEY = "chimahon.download.queue.worker"
        const val DOWNLOAD_QUEUE_TASK_TAG = "chimahon.download"

        fun downloadQueueTask(): BackgroundTask {
            return BackgroundTask(
                uniqueName = DOWNLOAD_QUEUE_TASK_NAME,
                workerKey = DOWNLOAD_QUEUE_WORKER_KEY,
                cadence = BackgroundTaskCadence.OneTime,
                constraints = BackgroundTaskConstraints(network = BackgroundNetworkConstraint.Connected),
                policy = ExistingBackgroundTaskPolicy.Keep,
                backoffCriteria = BackgroundTaskBackoffCriteria(
                    policy = BackgroundTaskBackoffPolicy.Exponential,
                    delay = 30.seconds,
                ),
                tags = setOf(DOWNLOAD_QUEUE_TASK_TAG),
            )
        }
    }
}

internal fun ChimahonDownloadQueueData.hasRunnableDownloads(): Boolean {
    return !paused && entries.any { it.status == ChimahonDownloadState.Queued }
}
