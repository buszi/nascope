package io.buszi.nascope.ui.dashboard.widgets

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.components.badges.StatusLevel

/**
 * Synthetic telemetry data preview provider for system overview widgets.
 *
 * Provides realistic homelab and enterprise server presets for testing layout
 * across varying host distributions, kernel versions, architectures, and load conditions.
 */
class SystemOverviewPreviewParameterProvider : PreviewParameterProvider<SystemOverviewData> {

    override val values: Sequence<SystemOverviewData> = sequenceOf(
        UbuntuServer,
        SynologyNas,
        TrueNasCore,
        MacMiniServer,
        WindowsServer,
    )

    companion object {
        val UbuntuServer = SystemOverviewData(
            hostname = "alpha-node-01.local",
            platform = HostPlatform.Ubuntu,
            osName = "Ubuntu 24.04.1 LTS",
            kernelRelease = "Linux 6.8.0-45-generic",
            uptime = "14d 06h 32m",
            loadAverages = LoadAverages(
                oneMinute = 0.42f,
                fiveMinute = 0.58f,
                fifteenMinute = 0.71f,
                cpuCores = 8,
            ),
            architecture = "x86_64",
            status = StatusLevel.Operational,
        )

        val SynologyNas = SystemOverviewData(
            hostname = "ds920-storage.lan",
            platform = HostPlatform.Synology,
            osName = "DSM 7.2.1-69057 Update 4",
            kernelRelease = "Linux 4.4.302+",
            uptime = "128d 14h 02m",
            loadAverages = LoadAverages(
                oneMinute = 3.85f,
                fiveMinute = 3.20f,
                fifteenMinute = 2.45f,
                cpuCores = 4,
            ),
            architecture = "x86_64",
            status = StatusLevel.Degraded,
        )

        val TrueNasCore = SystemOverviewData(
            hostname = "truenas-zpool.home",
            platform = HostPlatform.TrueNas,
            osName = "TrueNAS CORE 13.0-U6.1",
            kernelRelease = "FreeBSD 13.1-RELEASE-p9",
            uptime = "45d 21h 15m",
            loadAverages = LoadAverages(
                oneMinute = 1.12f,
                fiveMinute = 1.05f,
                fifteenMinute = 0.98f,
                cpuCores = 6,
            ),
            architecture = "x86_64",
            status = StatusLevel.Operational,
        )

        val MacMiniServer = SystemOverviewData(
            hostname = "macmini-m2-ci.local",
            platform = HostPlatform.MacOS,
            osName = "macOS 15.1 Sequoia",
            kernelRelease = "Darwin 24.1.0",
            uptime = "3d 08h 47m",
            loadAverages = LoadAverages(
                oneMinute = 1.84f,
                fiveMinute = 1.62f,
                fifteenMinute = 1.45f,
                cpuCores = 8,
            ),
            architecture = "arm64",
            status = StatusLevel.Operational,
        )

        val WindowsServer = SystemOverviewData(
            hostname = "win-ad-dc01.corp",
            platform = HostPlatform.Windows,
            osName = "Windows Server 2022 Datacenter",
            kernelRelease = "NT 10.0.20348",
            uptime = "82d 03h 19m",
            loadAverages = LoadAverages(
                oneMinute = 7.92f,
                fiveMinute = 6.84f,
                fifteenMinute = 5.41f,
                cpuCores = 8,
            ),
            architecture = "x86_64",
            status = StatusLevel.Critical,
        )
    }
}
