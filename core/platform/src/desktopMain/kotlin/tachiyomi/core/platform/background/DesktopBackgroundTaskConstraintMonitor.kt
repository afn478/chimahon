package tachiyomi.core.platform.background

import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DesktopBackgroundTaskConstraintMonitor internal constructor(
    private val pollInterval: Duration,
    platform: DesktopBackgroundPlatform,
    commandRunner: (String) -> Boolean,
) : BackgroundTaskConstraintMonitor {
    private val evaluator = DesktopBackgroundTaskConstraintEvaluator(platform, commandRunner)

    constructor(
        pollInterval: Duration = 30.seconds,
    ) : this(
        pollInterval = pollInterval,
        platform = currentDesktopBackgroundPlatform(),
        commandRunner = ::runDesktopCommand,
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

private fun currentDesktopBackgroundPlatform(): DesktopBackgroundPlatform {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    return when {
        os.contains("windows") -> DesktopBackgroundPlatform.Windows
        os.contains("mac") || os.contains("darwin") -> DesktopBackgroundPlatform.MacOS
        os.contains("linux") -> DesktopBackgroundPlatform.Linux
        else -> DesktopBackgroundPlatform.Other
    }
}

private fun runDesktopCommand(command: String): Boolean {
    return runCatching {
        val shell = if (currentDesktopBackgroundPlatform() == DesktopBackgroundPlatform.Windows) {
            listOf("cmd.exe", "/c", command)
        } else {
            listOf("sh", "-c", command)
        }
        val process = ProcessBuilder(shell)
            .redirectErrorStream(true)
            .start()
        if (process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.exitValue() == 0
        } else {
            process.destroyForcibly()
            false
        }
    }.getOrDefault(false)
}

private const val COMMAND_TIMEOUT_SECONDS = 5L
