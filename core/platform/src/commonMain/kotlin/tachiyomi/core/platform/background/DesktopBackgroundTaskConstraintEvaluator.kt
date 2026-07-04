package tachiyomi.core.platform.background

internal class DesktopBackgroundTaskConstraintEvaluator(
    private val platform: DesktopBackgroundPlatform,
    private val commandRunner: (String) -> Boolean,
) {
    fun constraintsAreMet(constraints: BackgroundTaskConstraints): Boolean {
        if (constraints.network != BackgroundNetworkConstraint.NotRequired && !probe(DesktopBackgroundProbe.NetworkConnected)) {
            return false
        }
        if (constraints.requiresWifi && !probe(DesktopBackgroundProbe.WifiConnected)) {
            return false
        }
        if (constraints.network == BackgroundNetworkConstraint.Unmetered && !probe(DesktopBackgroundProbe.UnmeteredNetwork)) {
            return false
        }
        if (constraints.requiresCharging && !probe(DesktopBackgroundProbe.Charging)) {
            return false
        }
        if (constraints.requiresBatteryNotLow && !probe(DesktopBackgroundProbe.BatteryNotLow)) {
            return false
        }
        return true
    }

    private fun probe(probe: DesktopBackgroundProbe): Boolean {
        val command = platform.commandFor(probe) ?: return true
        return commandRunner(command)
    }
}

internal enum class DesktopBackgroundPlatform {
    Windows,
    MacOS,
    Linux,
    Other,
}

private enum class DesktopBackgroundProbe {
    NetworkConnected,
    WifiConnected,
    UnmeteredNetwork,
    Charging,
    BatteryNotLow,
}

private fun DesktopBackgroundPlatform.commandFor(probe: DesktopBackgroundProbe): String? {
    return when (this) {
        DesktopBackgroundPlatform.Windows -> when (probe) {
            DesktopBackgroundProbe.NetworkConnected ->
                powershell(
                    """
                    ${'$'}p = Get-NetConnectionProfile -ErrorAction SilentlyContinue
                    if (${'$'}p | Where-Object { ${'$'}_.IPv4Connectivity -eq 'Internet' -or ${'$'}_.IPv6Connectivity -eq 'Internet' }) { exit 0 } else { exit 1 }
                    """,
                )
            DesktopBackgroundProbe.WifiConnected ->
                powershell(
                    """
                    if (Get-NetAdapter -Physical -ErrorAction SilentlyContinue | Where-Object { ${'$'}_.Status -eq 'Up' -and ${'$'}_.NdisPhysicalMedium -eq 9 }) { exit 0 } else { exit 1 }
                    """,
                )
            DesktopBackgroundProbe.UnmeteredNetwork ->
                powershell(
                    """
                    Add-Type -AssemblyName System.Runtime.WindowsRuntime
                    [Windows.Networking.Connectivity.NetworkInformation,Windows.Networking.Connectivity,ContentType=WindowsRuntime] | Out-Null
                    ${'$'}p = [Windows.Networking.Connectivity.NetworkInformation]::GetInternetConnectionProfile()
                    if (${'$'}p -and ${'$'}p.GetConnectionCost().NetworkCostType -in 'Unrestricted','Unknown') { exit 0 } else { exit 1 }
                    """,
                )
            DesktopBackgroundProbe.Charging ->
                powershell(
                    """
                    ${'$'}b = Get-CimInstance Win32_Battery -ErrorAction SilentlyContinue
                    if (!${'$'}b -or ${'$'}b.BatteryStatus -in 2,6,7,8,9) { exit 0 } else { exit 1 }
                    """,
                )
            DesktopBackgroundProbe.BatteryNotLow ->
                powershell(
                    """
                    ${'$'}b = Get-CimInstance Win32_Battery -ErrorAction SilentlyContinue
                    if (!${'$'}b -or ${'$'}b.EstimatedChargeRemaining -ge 15) { exit 0 } else { exit 1 }
                    """,
                )
        }
        DesktopBackgroundPlatform.MacOS -> when (probe) {
            DesktopBackgroundProbe.NetworkConnected ->
                "route -n get 1.1.1.1 >/dev/null 2>&1"
            DesktopBackgroundProbe.WifiConnected ->
                "networksetup -listallhardwareports | awk '/Wi-Fi|AirPort/{getline; print ${'$'}2}' | " +
                    "xargs -I{} networksetup -getairportnetwork {} | grep -qv 'not associated'"
            DesktopBackgroundProbe.UnmeteredNetwork ->
                null
            DesktopBackgroundProbe.Charging ->
                "pmset -g batt | grep -Eq 'AC Power|charged|finishing charge'"
            DesktopBackgroundProbe.BatteryNotLow ->
                "pmset -g batt | grep -Eo '[0-9]+%' | tr -d '%' | awk '{if (${'$'}1 < 15) exit 1}'"
        }
        DesktopBackgroundPlatform.Linux -> when (probe) {
            DesktopBackgroundProbe.NetworkConnected ->
                "ip route get 1.1.1.1 >/dev/null 2>&1"
            DesktopBackgroundProbe.WifiConnected ->
                "iwgetid -r >/dev/null 2>&1"
            DesktopBackgroundProbe.UnmeteredNetwork ->
                "nmcli -t -f GENERAL.METERED device show 2>/dev/null | grep -Eq ':(no|unknown)$'"
            DesktopBackgroundProbe.Charging ->
                "set -- /sys/class/power_supply/BAT*/status; [ ! -e \"${'$'}1\" ] || grep -Eq 'Charging|Full' \"${'$'}@\""
            DesktopBackgroundProbe.BatteryNotLow ->
                "set -- /sys/class/power_supply/BAT*/capacity; [ ! -e \"${'$'}1\" ] || awk '{if (${'$'}1 < 15) exit 1}' \"${'$'}@\""
        }
        DesktopBackgroundPlatform.Other -> null
    }
}

private fun powershell(script: String): String {
    val normalizedScript = script
        .trimIndent()
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .joinToString(separator = " ")
        .replace("|", "^|")

    return "powershell.exe -NoProfile -NonInteractive -Command \"$normalizedScript\""
}
