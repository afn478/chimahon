package tachiyomi.core.platform.background

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface BackgroundTaskScheduler {
    fun schedule(task: BackgroundTask)

    fun schedule(chain: BackgroundTaskChain) {
        throw UnsupportedOperationException("Background task chains are not supported")
    }

    fun cancel(uniqueName: String)

    fun isRunning(uniqueName: String): Boolean = false

    fun isScheduled(uniqueName: String): Boolean = isRunning(uniqueName)

    fun isRunningWithTag(tag: String): Boolean = false

    fun runningTasksWithTag(tag: String): List<BackgroundTaskInfo> = emptyList()

    fun cancelTask(task: BackgroundTaskInfo) {}

    fun isRunningFlow(uniqueName: String): Flow<Boolean> = flowOf(isRunning(uniqueName))

    fun prune() {}
}

data class BackgroundTask(
    val uniqueName: String,
    val workerKey: String,
    val cadence: BackgroundTaskCadence,
    val constraints: BackgroundTaskConstraints = BackgroundTaskConstraints(),
    val policy: ExistingBackgroundTaskPolicy = ExistingBackgroundTaskPolicy.Keep,
    val inputData: BackgroundTaskInputData = BackgroundTaskInputData.Empty,
    val initialDelay: Duration = Duration.ZERO,
    val backoffCriteria: BackgroundTaskBackoffCriteria? = null,
    val expeditedPolicy: BackgroundTaskOutOfQuotaPolicy? = null,
    val tags: Set<String> = emptySet(),
)

data class BackgroundTaskInfo(
    val id: String,
    val tags: Set<String> = emptySet(),
    val uniqueName: String = id,
    val state: BackgroundTaskState = BackgroundTaskState.Running,
)

data class BackgroundTaskChain(
    val uniqueName: String,
    val tasks: List<BackgroundTask>,
    val policy: ExistingBackgroundTaskPolicy = ExistingBackgroundTaskPolicy.Keep,
) {
    init {
        require(tasks.isNotEmpty()) { "Background task chain must contain at least one task" }
        require(tasks.all { it.cadence == BackgroundTaskCadence.OneTime }) {
            "Background task chains only support one-time tasks"
        }
    }
}

data class BackgroundTaskInputData(
    val values: Map<String, BackgroundTaskInputValue> = emptyMap(),
) {
    companion object {
        val Empty = BackgroundTaskInputData()

        fun of(vararg values: Pair<String, BackgroundTaskInputValue>): BackgroundTaskInputData {
            return BackgroundTaskInputData(values.toMap())
        }
    }
}

sealed interface BackgroundTaskInputValue {
    data class BooleanValue(val value: Boolean) : BackgroundTaskInputValue
    data class IntValue(val value: Int) : BackgroundTaskInputValue
    data class LongValue(val value: Long) : BackgroundTaskInputValue
    data class FloatValue(val value: Float) : BackgroundTaskInputValue
    data class DoubleValue(val value: Double) : BackgroundTaskInputValue
    data class StringValue(val value: String) : BackgroundTaskInputValue

    class BooleanArrayValue(val value: BooleanArray) : BackgroundTaskInputValue {
        override fun equals(other: Any?): Boolean {
            return other is BooleanArrayValue && value.contentEquals(other.value)
        }

        override fun hashCode(): Int = value.contentHashCode()
    }

    class IntArrayValue(val value: IntArray) : BackgroundTaskInputValue {
        override fun equals(other: Any?): Boolean {
            return other is IntArrayValue && value.contentEquals(other.value)
        }

        override fun hashCode(): Int = value.contentHashCode()
    }

    class LongArrayValue(val value: LongArray) : BackgroundTaskInputValue {
        override fun equals(other: Any?): Boolean {
            return other is LongArrayValue && value.contentEquals(other.value)
        }

        override fun hashCode(): Int = value.contentHashCode()
    }

    class FloatArrayValue(val value: FloatArray) : BackgroundTaskInputValue {
        override fun equals(other: Any?): Boolean {
            return other is FloatArrayValue && value.contentEquals(other.value)
        }

        override fun hashCode(): Int = value.contentHashCode()
    }

    class DoubleArrayValue(val value: DoubleArray) : BackgroundTaskInputValue {
        override fun equals(other: Any?): Boolean {
            return other is DoubleArrayValue && value.contentEquals(other.value)
        }

        override fun hashCode(): Int = value.contentHashCode()
    }

    class StringArrayValue(val value: Array<String?>) : BackgroundTaskInputValue {
        override fun equals(other: Any?): Boolean {
            return other is StringArrayValue && value.contentEquals(other.value)
        }

        override fun hashCode(): Int = value.contentHashCode()
    }
}

sealed interface BackgroundTaskCadence {
    data object OneTime : BackgroundTaskCadence

    data class Periodic(
        val repeatInterval: Duration,
        val flexInterval: Duration? = null,
    ) : BackgroundTaskCadence
}

data class BackgroundTaskConstraints(
    val network: BackgroundNetworkConstraint = BackgroundNetworkConstraint.NotRequired,
    val requiresWifi: Boolean = false,
    val requiresCharging: Boolean = false,
    val requiresBatteryNotLow: Boolean = false,
)

data class BackgroundTaskBackoffCriteria(
    val policy: BackgroundTaskBackoffPolicy,
    val delay: Duration,
)

enum class BackgroundNetworkConstraint {
    NotRequired,
    Connected,
    Unmetered,
}

enum class BackgroundTaskBackoffPolicy {
    Linear,
    Exponential,
}

enum class BackgroundTaskOutOfQuotaPolicy {
    RunAsNonExpedited,
    Drop,
}

enum class ExistingBackgroundTaskPolicy {
    Keep,
    Replace,
    Update,
}

enum class BackgroundTaskState {
    Enqueued,
    Running,
    Succeeded,
    Failed,
    Cancelled,
}

fun interface BackgroundWorker {
    suspend fun run(inputData: BackgroundTaskInputData): BackgroundTaskResult
}

fun interface BackgroundWorkerRegistry {
    fun worker(workerKey: String): BackgroundWorker?
}

sealed interface BackgroundTaskResult {
    data object Success : BackgroundTaskResult
    data object Retry : BackgroundTaskResult
    data object Failure : BackgroundTaskResult
}

fun interface BackgroundTaskConstraintMonitor {
    suspend fun awaitConstraints(constraints: BackgroundTaskConstraints)

    companion object {
        val Unconstrained = BackgroundTaskConstraintMonitor {}
    }
}

class MissingBackgroundWorkerException(workerKey: String) :
    IllegalArgumentException("No background worker registered for key: $workerKey")

internal fun BackgroundTask.retryDelay(attempt: Int): Duration {
    val criteria = backoffCriteria ?: return DEFAULT_BACKGROUND_TASK_RETRY_DELAY
    return when (criteria.policy) {
        BackgroundTaskBackoffPolicy.Linear -> criteria.delay * (attempt + 1)
        BackgroundTaskBackoffPolicy.Exponential -> {
            val boundedAttempt = attempt.coerceIn(0, MAX_BACKGROUND_TASK_EXPONENTIAL_ATTEMPT)
            val multiplier = 2.0.pow(boundedAttempt)
            (criteria.delay.inWholeMilliseconds * multiplier).toLong().milliseconds
        }
    }
}

private val DEFAULT_BACKGROUND_TASK_RETRY_DELAY = 30_000.milliseconds
private const val MAX_BACKGROUND_TASK_EXPONENTIAL_ATTEMPT = 30
