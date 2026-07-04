package tachiyomi.core.platform.background

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateByAddingTimeInterval
import kotlin.time.Duration

/**
 * Bridges registered Chimahon workers to iOS BGTaskScheduler.
 *
 * Every permitted task identifier must also be present in BGTaskSchedulerPermittedIdentifiers in
 * the host application's Info.plist. Construct this scheduler during application launch so iOS sees
 * all registrations before launch completes.
 */
@OptIn(ExperimentalForeignApi::class)
class IosBackgroundTaskScheduler(
    permittedTasks: Collection<BackgroundTask>,
    private val workerRegistry: BackgroundWorkerRegistry,
    private val identifierPrefix: String = "app.chimahon.background",
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val scheduler: BGTaskScheduler = BGTaskScheduler.sharedScheduler,
) : BackgroundTaskScheduler {

    private val tasks = permittedTasks.associateBy(BackgroundTask::uniqueName)
    private val states = MutableStateFlow<Map<String, BackgroundTaskInfo>>(emptyMap())
    private val runningJobs = MutableStateFlow<Map<String, Job>>(emptyMap())
    private val retryAttempts = MutableStateFlow<Map<String, Int>>(emptyMap())

    init {
        require(tasks.size == permittedTasks.size) {
            "iOS background task unique names must not be duplicated"
        }
        tasks.values.forEach { task ->
            workerRegistry.requireIosWorker(task.workerKey)
            val registered = scheduler.registerForTaskWithIdentifier(
                identifier = identifierFor(task.uniqueName),
                usingQueue = null,
            ) { launchedTask ->
                launchedTask?.let { execute(task, it) }
            }
            check(registered) {
                "Unable to register iOS background task: ${identifierFor(task.uniqueName)}"
            }
        }
    }

    override fun schedule(task: BackgroundTask) {
        val permittedTask = tasks[task.uniqueName]
            ?: error(
                "iOS task '${task.uniqueName}' was not registered at application launch. " +
                    "Add it to permittedTasks and BGTaskSchedulerPermittedIdentifiers.",
            )
        require(permittedTask.workerKey == task.workerKey) {
            "iOS task '${task.uniqueName}' cannot change workerKey after registration"
        }
        workerRegistry.requireIosWorker(task.workerKey)

        val current = states.value[task.uniqueName]
        if (current?.state in ACTIVE_STATES && task.policy == ExistingBackgroundTaskPolicy.Keep) {
            return
        }
        if (current?.state in ACTIVE_STATES) {
            cancel(task.uniqueName)
        }
        resetRetry(task.uniqueName)
        submit(task, task.initialDelay)
    }

    override fun cancel(uniqueName: String) {
        scheduler.cancelTaskRequestWithIdentifier(identifierFor(uniqueName))
        runningJobs.value[uniqueName]?.cancel()
        resetRetry(uniqueName)
        updateState(uniqueName, BackgroundTaskState.Cancelled)
    }

    override fun isRunning(uniqueName: String): Boolean {
        return states.value[uniqueName]?.state == BackgroundTaskState.Running
    }

    override fun isScheduled(uniqueName: String): Boolean {
        return states.value[uniqueName]?.state in ACTIVE_STATES
    }

    override fun isRunningWithTag(tag: String): Boolean = runningTasksWithTag(tag).isNotEmpty()

    override fun runningTasksWithTag(tag: String): List<BackgroundTaskInfo> {
        return states.value.values.filter { it.state == BackgroundTaskState.Running && tag in it.tags }
    }

    override fun cancelTask(task: BackgroundTaskInfo) {
        cancel(task.uniqueName)
    }

    override fun isRunningFlow(uniqueName: String): Flow<Boolean> {
        return states
            .map { it[uniqueName]?.state == BackgroundTaskState.Running }
            .distinctUntilChanged()
    }

    override fun prune() {
        states.update { scheduled ->
            scheduled.filterValues { it.state in ACTIVE_STATES }
        }
    }

    fun identifierFor(uniqueName: String): String {
        val suffix = uniqueName
            .lowercase()
            .replace(Regex("[^a-z0-9.-]"), "-")
            .trim('-')
        require(suffix.isNotEmpty()) { "iOS background task name must contain an alphanumeric character" }
        return "$identifierPrefix.$suffix"
    }

    private fun submit(task: BackgroundTask, delay: Duration) {
        val identifier = identifierFor(task.uniqueName)
        val request = task.toIosRequest(identifier).apply {
            if (delay > Duration.ZERO) {
                earliestBeginDate = NSDate().dateByAddingTimeInterval(delay.inWholeMilliseconds / 1_000.0)
            }
        }
        check(scheduler.submitTaskRequest(request, null)) {
            "iOS rejected background task request: $identifier"
        }
        states.update {
            it + (
                task.uniqueName to BackgroundTaskInfo(
                    id = identifier,
                    tags = task.tags + task.uniqueName,
                    uniqueName = task.uniqueName,
                    state = BackgroundTaskState.Enqueued,
                )
                )
        }
    }

    private fun execute(task: BackgroundTask, launchedTask: BGTask) {
        states.update { scheduled ->
            scheduled + (
                task.uniqueName to (
                    scheduled[task.uniqueName]?.copy(state = BackgroundTaskState.Running)
                        ?: BackgroundTaskInfo(
                            id = identifierFor(task.uniqueName),
                            tags = task.tags + task.uniqueName,
                            uniqueName = task.uniqueName,
                            state = BackgroundTaskState.Running,
                        )
                    )
                )
        }

        var completed = false
        fun complete(success: Boolean) {
            if (!completed) {
                completed = true
                launchedTask.expirationHandler = null
                launchedTask.setTaskCompletedWithSuccess(success)
            }
        }

        val job = scope.launch {
            try {
                when (workerRegistry.requireIosWorker(task.workerKey).run(task.inputData)) {
                    BackgroundTaskResult.Success -> {
                        updateState(task.uniqueName, BackgroundTaskState.Succeeded)
                        resetRetry(task.uniqueName)
                        complete(true)
                        val cadence = task.cadence
                        if (cadence is BackgroundTaskCadence.Periodic) {
                            submit(task, cadence.repeatInterval)
                        }
                    }
                    BackgroundTaskResult.Retry -> {
                        updateState(task.uniqueName, BackgroundTaskState.Enqueued)
                        complete(false)
                        submitRetry(task)
                    }
                    BackgroundTaskResult.Failure -> {
                        updateState(task.uniqueName, BackgroundTaskState.Failed)
                        resetRetry(task.uniqueName)
                        complete(false)
                    }
                }
            } catch (_: CancellationException) {
                updateState(task.uniqueName, BackgroundTaskState.Cancelled)
                resetRetry(task.uniqueName)
                complete(false)
            } catch (_: Throwable) {
                updateState(task.uniqueName, BackgroundTaskState.Failed)
                resetRetry(task.uniqueName)
                complete(false)
            } finally {
                runningJobs.update { it - task.uniqueName }
            }
        }
        runningJobs.update { it + (task.uniqueName to job) }
        launchedTask.expirationHandler = {
            job.cancel()
        }
    }

    private fun BackgroundTask.toIosRequest(identifier: String): BGTaskRequest {
        val needsProcessing = constraints.requiresCharging ||
            constraints.network != BackgroundNetworkConstraint.NotRequired ||
            constraints.requiresWifi

        return if (needsProcessing) {
            BGProcessingTaskRequest(identifier).apply {
                requiresExternalPower = constraints.requiresCharging
                requiresNetworkConnectivity =
                    constraints.network != BackgroundNetworkConstraint.NotRequired || constraints.requiresWifi
            }
        } else {
            BGAppRefreshTaskRequest(identifier)
        }
    }

    private fun submitRetry(task: BackgroundTask) {
        val attempt = retryAttempts.value[task.uniqueName] ?: 0
        retryAttempts.update { it + (task.uniqueName to (attempt + 1)) }
        submit(task, task.retryDelay(attempt))
    }

    private fun resetRetry(uniqueName: String) {
        retryAttempts.update { it - uniqueName }
    }

    private fun updateState(uniqueName: String, state: BackgroundTaskState) {
        states.update { scheduled ->
            val current = scheduled[uniqueName] ?: return@update scheduled
            scheduled + (uniqueName to current.copy(state = state))
        }
    }

    private companion object {
        val ACTIVE_STATES = setOf(BackgroundTaskState.Enqueued, BackgroundTaskState.Running)
    }
}

private fun BackgroundWorkerRegistry.requireIosWorker(workerKey: String): BackgroundWorker {
    return worker(workerKey) ?: throw MissingBackgroundWorkerException(workerKey)
}
