package io.buszi.nascope.ui.dashboard.widgets.system

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.util.Message
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewViewModel.ViewState

/**
 * Preview parameter provider supplying realistic synthetic host telemetry datasets.
 *
 * Demonstrates a variety of real-world scenarios:
 * - Healthy Linux (Ubuntu) multi-core host with low load.
 * - Heavily-loaded enterprise Debian server operating near core capacity.
 * - Synology DiskStation running DSM.
 * - TrueNAS storage appliance running FreeBSD.
 * - Edge container host running Alpine.
 * - Loading, Error, and Subsystem Offline lifecycle states.
 */
class SystemOverviewPreviewParameterProvider : PreviewParameterProvider<ViewState> {

    override val values: Sequence<ViewState> = sequenceOf(
        UbuntuServer,
        HighLoadDebianServer,
        SynologyNas,
        TrueNasAppliance,
        LoadingState,
        ErrorState,
        OfflineState,
    )

    companion object {
        val UbuntuServer = ViewState(
            hostname = "alpha-node-01.local",
            osName = "Ubuntu 24.04.1 LTS",
            kernelRelease = "Linux 6.8.0-45-generic",
            uptime = "14d 06h 32m",
            load1m = 0.42f,
            load5m = 0.58f,
            load15m = 0.71f,
            cpuCores = 8,
            platform = HostPlatform.Ubuntu,
            widgetState = WidgetState.Ready,
        )

        val HighLoadDebianServer = ViewState(
            hostname = "db-cluster-primary.corp",
            osName = "Debian GNU/Linux 12 (bookworm)",
            kernelRelease = "Linux 6.1.0-21-amd64",
            uptime = "142d 18h 05m",
            load1m = 14.82f,
            load5m = 12.40f,
            load15m = 9.15f,
            cpuCores = 16,
            platform = HostPlatform.Debian,
            widgetState = WidgetState.Ready,
        )

        val SynologyNas = ViewState(
            hostname = "DiskStation-DS920",
            osName = "Synology DSM 7.2.1-69057",
            kernelRelease = "Linux 4.4.302+",
            uptime = "68d 11h 42m",
            load1m = 1.85f,
            load5m = 2.10f,
            load15m = 1.95f,
            cpuCores = 4,
            platform = HostPlatform.Synology,
            widgetState = WidgetState.Ready,
        )

        val TrueNasAppliance = ViewState(
            hostname = "truenas-core.san",
            osName = "TrueNAS-13.0-U6.1",
            kernelRelease = "FreeBSD 13.1-RELEASE-p9",
            uptime = "210d 04h 19m",
            load1m = 0.88f,
            load5m = 0.94f,
            load15m = 1.02f,
            cpuCores = 12,
            platform = HostPlatform.TrueNas,
            widgetState = WidgetState.Ready,
        )

        val LoadingState = ViewState(
            widgetState = WidgetState.Loading,
        )

        val ErrorState = ViewState(
            widgetState = WidgetState.Error(
                message = Message.plain("Connection refused to Glances daemon at 192.168.1.120:61208"),
                onRetry = {},
            ),
        )

        val OfflineState = ViewState(
            widgetState = WidgetState.Offline(
                reason = Message.plain("Host daemon suspended or unreachable"),
                onReconnect = {},
            ),
        )
    }
}
