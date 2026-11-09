package io.buszi.nascope.ui.dashboard.widgets

import androidx.compose.runtime.Immutable
import io.buszi.nascope.ui.common.components.badges.StatusLevel

/**
 * Telemetry model summarizing core host details and system load.
 *
 * @param hostname Node network hostname or fully qualified domain name (e.g. `"alpha-node-01.local"`).
 * @param platform Operating system / distribution classification.
 * @param osName Full operating system name and release (e.g. `"Ubuntu 24.04.1 LTS"`).
 * @param kernelRelease Kernel version release (e.g. `"Linux 6.8.0-45-generic"`).
 * @param uptime Human-readable active uptime duration string (e.g. `"14d 06h 32m"`).
 * @param loadAverages 1m, 5m, and 15m UNIX load averages.
 * @param architecture Optional processor architecture string (e.g. `"x86_64"`, `"aarch64"`).
 * @param status Operational status of the host daemon. Defaults to [StatusLevel.Operational].
 */
@Immutable
data class SystemOverviewData(
    val hostname: String,
    val platform: HostPlatform,
    val osName: String,
    val kernelRelease: String,
    val uptime: String,
    val loadAverages: LoadAverages,
    val architecture: String? = "x86_64",
    val status: StatusLevel = StatusLevel.Operational,
)
