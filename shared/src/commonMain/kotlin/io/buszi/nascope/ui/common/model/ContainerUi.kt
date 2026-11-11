package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.data.telemetry.ContainerInfo
import io.buszi.nascope.data.telemetry.ContainerState
import io.buszi.nascope.ui.common.components.badges.StatusLevel

/**
 * UI representation of a container lifecycle state.
 */
@Immutable
enum class ContainerStateUi {
    Running,
    Paused,
    Stopped,
    Restarting,
    ;

    /** Health implied by the state: running is nominal, paused or stopped need attention, restarting is critical. */
    val status: StatusLevel
        get() = when (this) {
            Running -> StatusLevel.Operational
            Paused, Stopped -> StatusLevel.Degraded
            Restarting -> StatusLevel.Critical
        }

    companion object {
        fun from(data: ContainerState): ContainerStateUi = when (data) {
            ContainerState.Running -> Running
            ContainerState.Paused -> Paused
            ContainerState.Stopped -> Stopped
            ContainerState.Restarting -> Restarting
        }
    }
}

/**
 * UI representation of a single container instance.
 */
@Immutable
data class ContainerUi(
    val id: String,
    val name: String,
    val image: String,
    val state: ContainerStateUi,
    val cpuPercent: Float,
    val memoryUsageBytes: Long,
    val memoryLimitBytes: Long? = null,
    val uptime: String,
    val recentLogs: List<String> = emptyList(),
) {
    /** Memory usage relative to the configured limit in `[0.0, 1.0]`, or `null` when unlimited. */
    val memoryRatio: Float?
        get() = memoryLimitBytes
            ?.takeIf { it > 0L }
            ?.let { (memoryUsageBytes.toFloat() / it).coerceIn(0f, 1f) }

    companion object {
        fun from(data: ContainerInfo) = ContainerUi(
            id = data.id,
            name = data.name,
            image = data.image,
            state = ContainerStateUi.from(data.state),
            cpuPercent = data.cpuPercent,
            memoryUsageBytes = data.memoryUsageBytes,
            memoryLimitBytes = data.memoryLimitBytes,
            uptime = data.uptime,
            recentLogs = data.recentLogs,
        )
    }
}
