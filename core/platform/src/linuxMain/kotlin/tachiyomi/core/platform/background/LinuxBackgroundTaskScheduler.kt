package tachiyomi.core.platform.background

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import platform.posix.system
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class LinuxBackgroundTaskScheduler(
    workerRegistry: BackgroundWorkerRegistry,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    constraintMonitor: BackgroundTaskConstraintMonitor = LinuxBackgroundTaskConstraintMonitor(),
) : BackgroundTaskScheduler by CoroutineBackgroundTaskScheduler(
    workerRegistry = workerRegistry,
    scope = scope,
    constraintMonitor = constraintMonitor,
)

class LinuxBackgroundTaskConstraintMonitor(
    private val pollInterval: Duration = 30.seconds,
    private val commandRunner: (String) -> Boolean = ::runLinuxCommand,
) : BackgroundTaskConstraintMonitor {
    private val evaluator = DesktopBackgroundTaskConstraintEvaluator(
        platform = DesktopBackgroundPlatform.Linux,
        commandRunner = commandRunner,
    )

    override suspend fun awaitConstraints(constraints: BackgroundTaskConstraints) {
        while (!constraintsAreMet(constraints)) {
            delay(pollInterval)
        }
    }

    internal fun constraintsAreMet(constraints: BackgroundTaskConstraints): Boolean {
        return evaluator.constraintsAreMet(constraints)
    }
}

private fun runLinuxCommand(command: String): Boolean = system(command) == 0
