package app.chimahon.shared

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException

class ChimahonDownloadQueueWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        ChimahonAndroidHost.initialize(context.applicationContext)
        return try {
            val services = ChimahonSharedAppServices()
            try {
                services.processDownloadQueue()
            } finally {
                services.close()
            }
            Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            Result.retry()
        }
    }
}
