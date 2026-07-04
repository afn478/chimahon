package tachiyomi.core.platform.background

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineBackgroundTaskSchedulerTest {

    @Test
    fun `one-time task executes registered worker with input`() = runTest {
        var receivedInput = BackgroundTaskInputData.Empty
        val scheduler = scheduler {
            receivedInput = it
            BackgroundTaskResult.Success
        }
        val input = BackgroundTaskInputData.of(
            "libraryId" to BackgroundTaskInputValue.LongValue(42),
        )

        scheduler.schedule(task(inputData = input))
        advanceUntilIdle()

        assertEquals(input, receivedInput)
        assertEquals(BackgroundTaskState.Succeeded, scheduler.taskInfo(TASK_NAME)?.state)
        assertFalse(scheduler.isScheduled(TASK_NAME))
    }

    @Test
    fun `keep policy preserves existing work and replace policy cancels it`() = runTest {
        val receivedValues = mutableListOf<String>()
        val scheduler = scheduler { input ->
            receivedValues += (input.values.getValue("value") as BackgroundTaskInputValue.StringValue).value
            BackgroundTaskResult.Success
        }
        val delayed = task(
            initialDelay = 1.hours,
            inputData = stringInput("first"),
        )

        scheduler.schedule(delayed)
        val firstId = scheduler.taskInfo(TASK_NAME)?.id
        scheduler.schedule(
            delayed.copy(
                initialDelay = DurationZero,
                inputData = stringInput("kept-out"),
                policy = ExistingBackgroundTaskPolicy.Keep,
            ),
        )
        assertEquals(firstId, scheduler.taskInfo(TASK_NAME)?.id)

        scheduler.schedule(
            delayed.copy(
                initialDelay = DurationZero,
                inputData = stringInput("replacement"),
                policy = ExistingBackgroundTaskPolicy.Replace,
            ),
        )
        val replacementId = scheduler.taskInfo(TASK_NAME)?.id
        advanceUntilIdle()

        assertNotEquals(firstId, replacementId)
        assertEquals(listOf("replacement"), receivedValues)
    }

    @Test
    fun `retry uses configured backoff then succeeds`() = runTest {
        var attempts = 0
        val scheduler = scheduler {
            attempts++
            if (attempts == 1) BackgroundTaskResult.Retry else BackgroundTaskResult.Success
        }

        scheduler.schedule(
            task(
                backoffCriteria = BackgroundTaskBackoffCriteria(
                    policy = BackgroundTaskBackoffPolicy.Linear,
                    delay = 1.seconds,
                ),
            ),
        )
        runCurrent()

        assertEquals(1, attempts)
        assertEquals(BackgroundTaskState.Enqueued, scheduler.taskInfo(TASK_NAME)?.state)

        advanceTimeBy(1.seconds)
        runCurrent()

        assertEquals(2, attempts)
        assertEquals(BackgroundTaskState.Succeeded, scheduler.taskInfo(TASK_NAME)?.state)
    }

    @Test
    fun `exponential retry doubles configured delay for each retry`() = runTest {
        var attempts = 0
        val scheduler = scheduler {
            attempts++
            if (attempts <= 3) BackgroundTaskResult.Retry else BackgroundTaskResult.Success
        }

        scheduler.schedule(
            task(
                backoffCriteria = BackgroundTaskBackoffCriteria(
                    policy = BackgroundTaskBackoffPolicy.Exponential,
                    delay = 1.seconds,
                ),
            ),
        )
        runCurrent()

        assertEquals(1, attempts)

        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(2, attempts)

        advanceTimeBy(2.seconds)
        runCurrent()
        assertEquals(3, attempts)

        advanceTimeBy(4.seconds)
        runCurrent()
        assertEquals(4, attempts)
        assertEquals(BackgroundTaskState.Succeeded, scheduler.taskInfo(TASK_NAME)?.state)
    }

    @Test
    fun `task waits until platform constraints are available`() = runTest {
        val constraintsAvailable = CompletableDeferred<Unit>()
        var executions = 0
        val scheduler = CoroutineBackgroundTaskScheduler(
            workerRegistry = registry {
                executions++
                BackgroundTaskResult.Success
            },
            scope = this,
            constraintMonitor = BackgroundTaskConstraintMonitor {
                constraintsAvailable.await()
            },
        )

        scheduler.schedule(task())
        runCurrent()

        assertEquals(0, executions)
        assertEquals(BackgroundTaskState.Enqueued, scheduler.taskInfo(TASK_NAME)?.state)

        constraintsAvailable.complete(Unit)
        runCurrent()

        assertEquals(1, executions)
        assertEquals(BackgroundTaskState.Succeeded, scheduler.taskInfo(TASK_NAME)?.state)
    }

    @Test
    fun `chain runs workers sequentially`() = runTest {
        val calls = mutableListOf<String>()
        val scheduler = CoroutineBackgroundTaskScheduler(
            workerRegistry = BackgroundWorkerRegistry { key ->
                BackgroundWorker {
                    calls += key
                    BackgroundTaskResult.Success
                }
            },
            scope = this,
        )
        val chain = BackgroundTaskChain(
            uniqueName = "library-chain",
            tasks = listOf(
                task(uniqueName = "prepare", workerKey = "prepare"),
                task(uniqueName = "update", workerKey = "update"),
            ),
        )

        scheduler.schedule(chain)
        advanceUntilIdle()

        assertEquals(listOf("prepare", "update"), calls)
        assertEquals(BackgroundTaskState.Succeeded, scheduler.taskInfo(chain.uniqueName)?.state)
    }

    @Test
    fun `chain reports the currently running task tags`() = runTest {
        val releasePrepare = CompletableDeferred<Unit>()
        val releaseUpdate = CompletableDeferred<Unit>()
        val scheduler = CoroutineBackgroundTaskScheduler(
            workerRegistry = BackgroundWorkerRegistry { key ->
                when (key) {
                    "prepare" -> BackgroundWorker {
                        releasePrepare.await()
                        BackgroundTaskResult.Success
                    }
                    "update" -> BackgroundWorker {
                        releaseUpdate.await()
                        BackgroundTaskResult.Success
                    }
                    else -> null
                }
            },
            scope = this,
        )
        val chain = BackgroundTaskChain(
            uniqueName = "library-chain",
            tasks = listOf(
                task(
                    uniqueName = "prepare-library",
                    workerKey = "prepare",
                    tags = setOf("library"),
                ),
                task(
                    uniqueName = "update-library",
                    workerKey = "update",
                    tags = setOf("network"),
                ),
            ),
        )

        scheduler.schedule(chain)
        runCurrent()

        assertTrue(scheduler.isRunningWithTag(chain.uniqueName))
        assertTrue(scheduler.isRunningWithTag("prepare-library"))
        assertTrue(scheduler.isRunningWithTag("library"))
        assertFalse(scheduler.isRunningWithTag("update-library"))
        assertEquals(
            chain.uniqueName,
            scheduler.runningTasksWithTag("prepare-library").single().uniqueName,
        )

        releasePrepare.complete(Unit)
        runCurrent()

        assertTrue(scheduler.isRunningWithTag(chain.uniqueName))
        assertFalse(scheduler.isRunningWithTag("prepare-library"))
        assertTrue(scheduler.isRunningWithTag("update-library"))
        assertTrue(scheduler.isRunningWithTag("network"))

        releaseUpdate.complete(Unit)
        advanceUntilIdle()

        assertFalse(scheduler.isRunningWithTag(chain.uniqueName))
        assertEquals(BackgroundTaskState.Succeeded, scheduler.taskInfo(chain.uniqueName)?.state)
    }

    @Test
    fun `periodic task repeats and can be cancelled`() = runTest {
        var executions = 0
        val scheduler = scheduler {
            executions++
            BackgroundTaskResult.Success
        }
        scheduler.schedule(
            task(
                cadence = BackgroundTaskCadence.Periodic(repeatInterval = 10.seconds),
            ),
        )

        runCurrent()
        assertEquals(1, executions)

        advanceTimeBy(10.seconds)
        runCurrent()
        assertEquals(2, executions)

        scheduler.cancel(TASK_NAME)
        advanceTimeBy(20.seconds)
        runCurrent()

        assertEquals(2, executions)
        assertEquals(BackgroundTaskState.Cancelled, scheduler.taskInfo(TASK_NAME)?.state)
        assertFalse(scheduler.isRunning(TASK_NAME))
    }

    @Test
    fun `tag queries and cancellation use live task identity`() = runTest {
        val releaseWorker = CompletableDeferred<Unit>()
        val scheduler = scheduler {
            releaseWorker.await()
            BackgroundTaskResult.Success
        }
        scheduler.schedule(task(tags = setOf("library")))
        runCurrent()

        val running = scheduler.runningTasksWithTag("library").single()
        assertTrue(scheduler.isRunningWithTag("library"))

        scheduler.cancelTask(running)
        runCurrent()

        assertEquals(BackgroundTaskState.Cancelled, scheduler.taskInfo(TASK_NAME)?.state)
        assertFalse(scheduler.isRunningWithTag("library"))
    }

    private fun kotlinx.coroutines.test.TestScope.scheduler(
        worker: suspend (BackgroundTaskInputData) -> BackgroundTaskResult,
    ): CoroutineBackgroundTaskScheduler {
        return CoroutineBackgroundTaskScheduler(
            workerRegistry = registry(worker),
            scope = this,
        )
    }

    private fun registry(
        worker: suspend (BackgroundTaskInputData) -> BackgroundTaskResult,
    ): BackgroundWorkerRegistry {
        return BackgroundWorkerRegistry { key ->
            if (key == WORKER_KEY) BackgroundWorker(worker) else null
        }
    }

    private fun task(
        uniqueName: String = TASK_NAME,
        workerKey: String = WORKER_KEY,
        cadence: BackgroundTaskCadence = BackgroundTaskCadence.OneTime,
        inputData: BackgroundTaskInputData = BackgroundTaskInputData.Empty,
        initialDelay: kotlin.time.Duration = DurationZero,
        backoffCriteria: BackgroundTaskBackoffCriteria? = null,
        tags: Set<String> = emptySet(),
    ): BackgroundTask {
        return BackgroundTask(
            uniqueName = uniqueName,
            workerKey = workerKey,
            cadence = cadence,
            inputData = inputData,
            initialDelay = initialDelay,
            backoffCriteria = backoffCriteria,
            tags = tags,
        )
    }

    private fun stringInput(value: String): BackgroundTaskInputData {
        return BackgroundTaskInputData.of(
            "value" to BackgroundTaskInputValue.StringValue(value),
        )
    }

    private companion object {
        const val TASK_NAME = "library-update"
        const val WORKER_KEY = "library-worker"
        val DurationZero = kotlin.time.Duration.ZERO
    }
}
