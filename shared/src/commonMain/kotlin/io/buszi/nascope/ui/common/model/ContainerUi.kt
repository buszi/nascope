package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.model.telemetry.ContainerStatus
import io.buszi.nascope.domain.model.telemetry.ContainerUsage
import io.buszi.nascope.domain.telemetry.ObserveContainersUseCase
import io.buszi.nascope.ui.common.components.badges.StatusLevel

/**
 * UI representation of a container lifecycle state.
 */
@Immutable
enum class ContainerStateUi {
    Running,
    Paused,
    Restarting,
    Exited,
    Created,
    Dead,
    Unknown,
    ;

    /** Health implied by the state: running is nominal, restarting or dead is critical, everything else needs attention. */
    val status: StatusLevel
        get() = when (this) {
            Running -> StatusLevel.Operational
            Restarting, Dead -> StatusLevel.Critical
            Paused, Exited, Created, Unknown -> StatusLevel.Degraded
        }

    companion object {
        fun from(data: ContainerStatus): ContainerStateUi = when (data) {
            ContainerStatus.Running -> Running
            ContainerStatus.Paused -> Paused
            ContainerStatus.Restarting -> Restarting
            ContainerStatus.Exited -> Exited
            ContainerStatus.Created -> Created
            ContainerStatus.Dead -> Dead
            ContainerStatus.Unknown -> Unknown
        }
    }
}

/**
 * UI representation of a single container instance.
 *
 * [cpuPercent], [memoryUsageBytes] and [uptime] are `null` when Glances does not report them (stopped containers,
 * Podman): the UI shows a dash.
 */
@Immutable
data class ContainerUi(
    val id: String,
    val name: String,
    /** Empty when unknown: the image is left out. */
    val image: String,
    val state: ContainerStateUi,
    val cpuPercent: Float?,
    val memoryUsageBytes: Long?,
    val memoryLimitBytes: Long? = null,
    val uptime: String? = null,
) {
    /** Memory usage relative to the configured limit in `[0.0, 1.0]`, or `null` when unlimited or unknown. */
    val memoryRatio: Float?
        get() = if (memoryUsageBytes == null) {
            null
        } else {
            memoryLimitBytes
                ?.takeIf { it > 0L }
                ?.let { (memoryUsageBytes.toFloat() / it).coerceIn(0f, 1f) }
        }

    companion object {
        fun from(data: ContainerUsage) = ContainerUi(
            id = data.id,
            name = data.name,
            image = data.image.orEmpty(),
            state = ContainerStateUi.from(data.status),
            cpuPercent = data.cpuPercent,
            memoryUsageBytes = data.memoryUsageBytes,
            memoryLimitBytes = data.memoryLimitBytes,
            uptime = data.uptimeText,
        )
    }
}

/** The containers of the host and the engine(s) they run on. */
@Immutable
data class ContainersUi(
    /** `Docker`, `Podman` or both; empty when Glances reports no engine: the engine tag is left out. */
    val engine: String = "",
    val containers: List<ContainerUi> = emptyList(),
) {
    companion object {
        fun from(data: ObserveContainersUseCase.Data): ContainersUi = ContainersUi(
            engine = data.containers
                .mapNotNull { it.engine?.takeIf(String::isNotBlank) }
                .distinct()
                .joinToString(" / ") { engine -> engine.replaceFirstChar(Char::uppercase) },
            containers = data.containers.map(ContainerUi::from),
        )
    }
}
