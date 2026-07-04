package tachiyomi.core.platform.background

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class DesktopBackgroundTaskConstraintMonitorTest {

    @Test
    fun unconstrainedTaskDoesNotProbeOperatingSystem() {
        val commands = mutableListOf<String>()
        val monitor = DesktopBackgroundTaskConstraintMonitor(
            pollInterval = 30.seconds,
            platform = DesktopBackgroundPlatform.Linux,
            commandRunner = { command ->
                commands += command
                false
            },
        )

        assertTrue(monitor.constraintsAreMet(BackgroundTaskConstraints()))
        assertTrue(commands.isEmpty())
    }

    @Test
    fun unavailableLinuxNetworkBlocksConstrainedTask() {
        val commands = mutableListOf<String>()
        val monitor = DesktopBackgroundTaskConstraintMonitor(
            pollInterval = 30.seconds,
            platform = DesktopBackgroundPlatform.Linux,
            commandRunner = { command ->
                commands += command
                false
            },
        )

        val result = monitor.constraintsAreMet(
            BackgroundTaskConstraints(network = BackgroundNetworkConstraint.Connected),
        )

        assertFalse(result)
        assertEquals(1, commands.size)
        assertTrue(commands.single().contains("ip route get"))
    }

    @Test
    fun macOsUnmeteredNetworkFallsBackToConnectivityProbe() {
        val commands = mutableListOf<String>()
        val monitor = DesktopBackgroundTaskConstraintMonitor(
            pollInterval = 30.seconds,
            platform = DesktopBackgroundPlatform.MacOS,
            commandRunner = { command ->
                commands += command
                true
            },
        )

        val result = monitor.constraintsAreMet(
            BackgroundTaskConstraints(network = BackgroundNetworkConstraint.Unmetered),
        )

        assertTrue(result)
        assertEquals(1, commands.size)
        assertTrue(commands.single().contains("route -n get"))
    }

    @Test
    fun everyRequestedWindowsConstraintMustPass() {
        val commands = mutableListOf<String>()
        val monitor = DesktopBackgroundTaskConstraintMonitor(
            pollInterval = 30.seconds,
            platform = DesktopBackgroundPlatform.Windows,
            commandRunner = { command ->
                commands += command
                true
            },
        )

        val result = monitor.constraintsAreMet(
            BackgroundTaskConstraints(
                network = BackgroundNetworkConstraint.Unmetered,
                requiresWifi = true,
                requiresCharging = true,
                requiresBatteryNotLow = true,
            ),
        )

        assertTrue(result)
        assertEquals(5, commands.size)
        assertTrue(commands.all { it.startsWith("powershell.exe -NoProfile -NonInteractive -Command") })
        assertTrue(commands.any { it.contains("NetworkCostType") })
    }

    @Test
    fun unsupportedDesktopPlatformTreatsConstraintsAsBestEffort() {
        var commands = 0
        val monitor = DesktopBackgroundTaskConstraintMonitor(
            pollInterval = 30.seconds,
            platform = DesktopBackgroundPlatform.Other,
            commandRunner = {
                commands++
                false
            },
        )

        val result = monitor.constraintsAreMet(
            BackgroundTaskConstraints(
                network = BackgroundNetworkConstraint.Unmetered,
                requiresWifi = true,
                requiresCharging = true,
                requiresBatteryNotLow = true,
            ),
        )

        assertTrue(result)
        assertEquals(0, commands)
    }
}
