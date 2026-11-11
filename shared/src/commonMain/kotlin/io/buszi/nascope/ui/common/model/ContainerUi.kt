package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.data.telemetry.ContainerInfo
import io.buszi.nascope.data.telemetry.ContainerState

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
) {
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
        )
    }
}
