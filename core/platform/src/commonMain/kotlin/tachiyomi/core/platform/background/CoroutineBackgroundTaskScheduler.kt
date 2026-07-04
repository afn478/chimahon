package tachiyomi.core.platform.background

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Executes background work while the host process is alive.
 *
 * Desktop schedulers use this directly for process-lifetime work. Platforms with a suspended
 * application lifecycle can pair it with their operating-system wake-up mechanism.
 */
class CoroutineBackgroundTaskScheduler(
    private val workerRegistry: BackgroundWorkerRegistry,
    private val scope: CoroutineScope,
    private val constraintMonitor: BackgroundTaskConstraintMonitor = BackgroundTaskConstraintMonitor.Unconstrained,
) : BackgroundTaskScheduler {

    private val work = MutableStateFlow<Map<String, ScheduledWork>>(emptyMap())

    override fun schedule(task: BackgroundTask) {
        workerRegistry.requireWorker(task.workerKey)
        enqueue(
            uniqueName = task.uniqueName,
            policy = task.policy,
            tags = task.taskTags(),
        ) { id ->
            runTask(
                task = task,
                trackingName = task.uniqueName,
                id = id,
                periodic = task.cadence is BackgroundTaskCadence.Periodic,
                markSuccess = true,
                runningTags = task.taskTags(),
            )
        }
    }

    override fun schedule(chain: BackgroundTaskChain) {
        chain.tasks.forEach { workerRegistry.requireWorker(it.workerKey) }
        enqueue(
            uniqueName = chain.uniqueName,
            policy = chain.policy,
            tags = chain.scheduledTags(),
        ) { id ->
            for (task in chain.tasks) {
                val completed = runTask(
                    task = task,
                    trackingName = chain.uniqueName,
                    id = id,
                    periodic = false,
                    markSuccess = false,
                    runningTags = task.chainTaskTags(chain.uniqueName),
                )
                if (!completed) {
                    return@enqueue
                }
            }
            updateState(chain.uniqueName, id, BackgroundTaskState.Succeeded)
        }
    }

    override fun cancel(uniqueName: String) {
        val scheduledWork = work.value[uniqueName] ?: return
        scheduledWork.job.cancel()
        updateState(uniqueName, scheduledWork.info.id, BackgroundTaskState.Cancelled)
    }

    override fun isRunning(uniqueName: String): Boolean {
        return work.value[uniqueName]?.info?.state == BackgroundTaskState.Running
    }

    override fun isScheduled(uniqueName: String): Boolean {
        return work.value[uniqueName]?.info?.state in ACTIVE_STATES
    }

    override fun isRunningWithTag(tag: String): Boolean {
        return runningTasksWithTag(tag).isNotEmpty()
    }

    override fun runningTasksWithTag(tag: String): List<BackgroundTaskInfo> {
        return work.value.values
            .map(ScheduledWork::info)
            .filter { it.state == BackgroundTaskState.Running && tag in it.tags }
    }

    override fun cancelTask(task: BackgroundTaskInfo) {
        val scheduledWork = work.value.values.firstOrNull { it.info.id == task.id } ?: return
        cancel(scheduledWork.info.uniqueName)
    }

    override fun isRunningFlow(uniqueName: String): Flow<Boolean> {
        return work
            .map { scheduled -> scheduled[uniqueName]?.info?.state == BackgroundTaskState.Running }
            .distinctUntilChanged()
    }

    override fun prune() {
        work.update { scheduled ->
            scheduled.filterValues { it.info.state in ACTIVE_STATES }
        }
    }

    fun taskInfo(uniqueName: String): BackgroundTaskInfo? = work.value[uniqueName]?.info

    private fun enqueue(
        uniqueName: String,
        policy: ExistingBackgroundTaskPolicy,
        tags: Set<String>,
        runner: suspend (String) -> Unit,
    ) {
        val existing = work.value[uniqueName]
        if (existing?.info?.state in ACTIVE_STATES) {
            if (policy == ExistingBackgroundTaskPolicy.Keep) return
            existing?.job?.cancel()
        }

        val id = newTaskId()
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                runner(id)
            } catch (_: CancellationException) {
                updateState(uniqueName, id, BackgroundTaskState.Cancelled)
                throw CancellationException()
            } catch (_: Throwable) {
                updateState(uniqueName, id, BackgroundTaskState.Failed)
            }
        }
        val info = BackgroundTaskInfo(
            id = id,
            tags = tags,
            uniqueName = uniqueName,
            state = BackgroundTaskState.Enqueued,
        )
        work.update { it + (uniqueName to ScheduledWork(info, job)) }
        job.start()
    }

    private suspend fun runTask(
        task: BackgroundTask,
        trackingName: String,
        id: String,
        periodic: Boolean,
        markSuccess: Boolean,
        runningTags: Set<String>,
    ): Boolean {
        if (task.initialDelay > Duration.ZERO) {
            delay(task.initialDelay)
        }

        do {
            var attempt = 0
            while (true) {
                constraintMonitor.awaitConstraints(task.constraints)
                updateState(trackingName, id, BackgroundTaskState.Running, runningTags)

                val result = try {
                    workerRegistry.requireWorker(task.workerKey).run(task.inputData)
                } catch (_: CancellationException) {
                    throw CancellationException()
                } catch (_: Throwable) {
                    BackgroundTaskResult.Failure
                }

                when (result) {
                    BackgroundTaskResult.Success -> break
                    BackgroundTaskResult.Failure -> {
                        updateState(trackingName, id, BackgroundTaskState.Failed)
                        return false
                    }
                    BackgroundTaskResult.Retry -> {
                        updateState(trackingName, id, BackgroundTaskState.Enqueued)
                        delay(task.retryDelay(attempt++))
                    }
                }
            }

            if (!periodic) {
                if (markSuccess) {
                    updateState(trackingName, id, BackgroundTaskState.Succeeded)
                }
                return true
            }

            updateState(trackingName, id, BackgroundTaskState.Enqueued)
            delay((task.cadence as BackgroundTaskCadence.Periodic).repeatInterval)
        } while (true)
    }

    private fun updateState(
        uniqueName: String,
        id: String,
        state: BackgroundTaskState,
        tags: Set<String>? = null,
    ) {
        work.update { scheduled ->
            val current = scheduled[uniqueName]
            if (current == null || current.info.id != id) {
                scheduled
            } else {
                val updated = current.copy(
                    info = current.info.copy(
                        state = state,
                        tags = tags ?: current.info.tags,
                    )
                )
                scheduled + (uniqueName to updated)
            }
        }
    }

    private data class ScheduledWork(
        val info: BackgroundTaskInfo,
        val job: Job,
    )

    private companion object {
        val ACTIVE_STATES = setOf(BackgroundTaskState.Enqueued, BackgroundTaskState.Running)

        @OptIn(ExperimentalUuidApi::class)
        fun newTaskId(): String = Uuid.random().toString()
    }
}

private fun BackgroundWorkerRegistry.requireWorker(workerKey: String): BackgroundWorker {
    return worker(workerKey) ?: throw MissingBackgroundWorkerException(workerKey)
}

private fun BackgroundTask.taskTags(): Set<String> {
    return tags + uniqueName
}

private fun BackgroundTask.chainTaskTags(chainUniqueName: String): Set<String> {
    return taskTags() + chainUniqueName
}

private fun BackgroundTaskChain.scheduledTags(): Set<String> {
    return tasks.flatMapTo(mutableSetOf(uniqueName)) { task -> task.taskTags() }
}
