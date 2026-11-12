package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.model.telemetry.FileSystemUsage
import io.buszi.nascope.ui.common.components.badges.StatusLevel

/**
 * UI representation of a mounted filesystem.
 *
 * [deviceName], [fsType] and [mountOptions] are empty when the host does not report them (Glances 3 has no
 * mount options); the UI leaves such fields out.
 */
@Immutable
data class StorageMountUi(
    val mountPoint: String,
    val deviceName: String,
    val fsType: String,
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val mountOptions: String = "",
) {
    /** Docker bind-mounts these files of the container into it, so they only echo the host's filesystem. */
    val isContainerInjected: Boolean
        get() = mountPoint in ContainerInjectedMountPoints

    /** What to call the mount: its path, or its device when the path is a container-injected file. */
    val displayName: String
        get() = if (isContainerInjected && deviceName.isNotEmpty()) deviceName else mountPoint

    /** Fraction of the capacity that is in use, in `[0.0, 1.0]`. */
    val usageRatio: Float
        get() = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    /** Status derived from [usageRatio] only: Glances reports no pool health. */
    val status: StatusLevel
        get() = when {
            usageRatio >= CriticalThreshold -> StatusLevel.Critical
            usageRatio >= WarningThreshold -> StatusLevel.Degraded
            else -> StatusLevel.Operational
        }

    companion object {
        private val ContainerInjectedMountPoints = setOf("/etc/hosts", "/etc/hostname", "/etc/resolv.conf")

        const val WarningThreshold: Float = 0.80f
        const val CriticalThreshold: Float = 0.90f

        fun from(data: FileSystemUsage) = StorageMountUi(
            mountPoint = data.mountPoint,
            deviceName = data.deviceName.orEmpty(),
            fsType = data.fsType.orEmpty(),
            totalBytes = data.totalBytes,
            usedBytes = data.usedBytes,
            freeBytes = data.freeBytes,
            mountOptions = data.options.orEmpty(),
        )
    }
}

/** Worst [StorageMountUi.status] across the mounts, [StatusLevel.Operational] when there are none. */
val List<StorageMountUi>.overallStatus: StatusLevel
    get() = when {
        any { it.status == StatusLevel.Critical } -> StatusLevel.Critical
        any { it.status == StatusLevel.Degraded } -> StatusLevel.Degraded
        else -> StatusLevel.Operational
    }

/**
 * One mount per backing device: bind mounts such as `/etc/hosts` and `/etc/hostname` in a container all report
 * the same device, so summing every mount would count its capacity several times. Mounts without a device name
 * are kept as they are.
 */
val List<StorageMountUi>.distinctDevices: List<StorageMountUi>
    get() = distinctBy { it.deviceName.ifEmpty { it.mountPoint } }

/**
 * Leaves out the container-injected mounts (`/etc/hosts` and friends) that a real mount of the same device already
 * covers. When a device only has such mounts, as on a Glances container without a data volume, one of them stays,
 * so the host's storage is still listed (under its device name, see [StorageMountUi.displayName]).
 */
fun List<StorageMountUi>.withoutContainerInjectedDuplicates(): List<StorageMountUi> {
    val realDevices = filterNot { it.isContainerInjected }.map { it.deviceName }.toSet()
    val keptInjected = filter { it.isContainerInjected && it.deviceName !in realDevices }
        .distinctBy { it.deviceName.ifEmpty { it.mountPoint } }
    return filter { !it.isContainerInjected || it in keptInjected }
}
