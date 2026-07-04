package tachiyomi.core.platform.background

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LinuxBackgroundTaskConstraintMonitorTest {

    @Test
    fun unconstrainedTaskDoesNotProbeOperatingSystem() {
        var commands = 0
        val monitor = LinuxBackgroundTaskConstraintMonitor {
            commands++
            false
        }

        assertTrue(monitor.constraintsAreMet(BackgroundTaskConstraints()))
        assertEquals(0, commands)
    }

    @Test
    fun unavailableNetworkBlocksConstrainedTask() {
        val commands = mutableListOf<String>()
        val monitor = LinuxBackgroundTaskConstraintMonitor { command ->
            commands += command
            false
        }

        val result = monitor.constraintsAreMet(
            BackgroundTaskConstraints(network = BackgroundNetworkConstraint.Connected),
        )

        assertFalse(result)
        assertEquals(1, commands.size)
        assertTrue(commands.single().contains("ip route get"))
    }

    @Test
    fun everyRequestedConstraintMustPass() {
        val commands = mutableListOf<String>()
        val monitor = LinuxBackgroundTaskConstraintMonitor { command ->
            commands += command
            true
        }

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
        assertTrue(commands.any { it.contains("nmcli") })
        assertTrue(commands.any { it.contains("/sys/class/power_supply") })
    }
}
