package app.chimahon.shared

import kotlinx.coroutines.CancellationException
import tachiyomi.core.platform.background.BackgroundTaskResult
import tachiyomi.core.platform.background.BackgroundTaskScheduler
import tachiyomi.core.platform.background.BackgroundWorker
import tachiyomi.core.platform.background.BackgroundWorkerRegistry
import tachiyomi.core.platform.background.IosBackgroundTaskScheduler

/**
 * iOS host integration for Chimahon shared background work.
 *
 * Add [permittedIdentifiers] to BGTaskSchedulerPermittedIdentifiers and [requiredBackgroundModes]
 * to UIBackgroundModes in the host Info.plist, then call [register] during application launch
 * before creating the shared UI.
 */
object ChimahonIosBackgroundTasks {
    private const val IDENTIFIER_PREFIX = "app.chimahon.background"

    val downloadQueueIdentifier: String =
        "$IDENTIFIER_PREFIX.${ChimahonDownloadBackgroundCoordinator.DOWNLOAD_QUEUE_TASK_NAME}"
    val permittedIdentifiers: List<String> = listOf(downloadQueueIdentifier)
    val requiredBackgroundModes: List<String> = listOf("processing")

    private var scheduler: BackgroundTaskScheduler? = null

    fun register(): Boolean {
        if (scheduler != null) return true

        val registered = runCatching {
            IosBackgroundTaskScheduler(
                permittedTasks = listOf(ChimahonDownloadBackgroundCoordinator.downloadQueueTask()),
                workerRegistry = BackgroundWorkerRegistry(::workerFor),
                identifierPrefix = IDENTIFIER_PREFIX,
            )
        }.getOrNull() ?: return false

        scheduler = registered
        return true
    }

    internal fun schedulerOrRegister(): BackgroundTaskScheduler? {
        register()
        return scheduler
    }

    private fun workerFor(workerKey: String): BackgroundWorker? {
        return when (workerKey) {
            ChimahonDownloadBackgroundCoordinator.DOWNLOAD_QUEUE_WORKER_KEY ->
                BackgroundWorker { runDownloadQueue() }
            else -> null
        }
    }

    private suspend fun runDownloadQueue(): BackgroundTaskResult {
        return try {
            val services = ChimahonSharedAppServices()
            try {
                services.processDownloadQueue()
            } finally {
                services.close()
            }
            BackgroundTaskResult.Success
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            BackgroundTaskResult.Retry
        }
    }
}
