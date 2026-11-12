package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.telemetry.ObserveSystemOverviewUseCase
import io.buszi.nascope.ui.dashboard.widgets.system.HostPlatform
import kotlin.time.Duration

/** UI representation of the host identity, uptime and load shown by the system overview brick. */
@Immutable
data class SystemOverviewUi(
    val hostname: String = "",
    /** Distribution when Glances reports one, else the OS name. */
    val osName: String = "",
    /** OS, kernel release and architecture on one line; parts the host does not provide are left out. */
    val kernelRelease: String = "",
    /** `null` when the uptime plugin has no data: the tag is not shown. */
    val uptime: String? = null,
    /** `null` when the load plugin has no data: the load section is not shown. */
    val load: LoadAverageUi? = null,
    val platform: HostPlatform = HostPlatform.Generic,
) {
    companion object {
        fun from(data: ObserveSystemOverviewUseCase.Data): SystemOverviewUi {
            val system = data.system
            val kernel = listOfNotNull(system.osName, system.osVersion).joinToString(" ")
            return SystemOverviewUi(
                hostname = system.hostname,
                osName = system.distribution ?: system.osName,
                kernelRelease = listOfNotNull(kernel, system.architecture).joinToString(" · "),
                uptime = data.uptime?.let(::formatUptime),
                load = data.load?.let { LoadAverageUi(it.min1, it.min5, it.min15, it.logicalCores ?: 0) },
                platform = HostPlatform.fromIdentifier(system.distribution ?: system.osName),
            )
        }

        /** `14d 06h 32m`; days and hours are left out while they are zero. */
        fun formatUptime(uptime: Duration): String = uptime.toComponents { days, hours, minutes, _, _ ->
            val mm = minutes.toString().padStart(2, '0')
            val hh = hours.toString().padStart(2, '0')
            when {
                days > 0 -> "${days}d ${hh}h ${mm}m"
                hours > 0 -> "${hours}h ${mm}m"
                else -> "${minutes}m"
            }
        }
    }
}

/** Load averages with the core count they are normalized against (0 when the host does not report it). */
@Immutable
data class LoadAverageUi(
    val load1m: Float,
    val load5m: Float,
    val load15m: Float,
    val cpuCores: Int,
)
