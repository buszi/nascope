package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.domain.model.telemetry.ProcessUsage

/**
 * UI representation of a process scheduling state, parsed from the single-letter status code.
 */
@Immutable
enum class ProcessStateUi(val code: Char) {
    Running('R'),
    Sleeping('S'),
    Idle('I'),
    DiskSleep('D'),
    Stopped('T'),
    Zombie('Z'),
    Unknown('?'),
    ;

    companion object {
        fun fromCode(code: String): ProcessStateUi {
            val letter = code.firstOrNull() ?: return Unknown
            return entries.firstOrNull { it.code.equals(letter, ignoreCase = true) } ?: Unknown
        }
    }
}

/**
 * UI representation of a single host process.
 */
@Immutable
data class ProcessUi(
    val pid: Int,
    val name: String,
    /** A dash when the host does not report the user. */
    val user: String,
    val cpuPercent: Float,
    val memoryPercent: Float,
    /** `null` when the host does not report it: the RSS is left out. */
    val memoryRssBytes: Long? = null,
    /** psutil status letter; empty when unknown. */
    val status: String = "",
    /** Logical cores of the host, to turn the per-core [cpuPercent] into a share of the machine. */
    val logicalCores: Int = 1,
) {
    /**
     * [cpuPercent] as a share of the whole machine in `[0, 100]`. Glances reports it per core (a process on two
     * cores of four shows 200%), so a busy multi-threaded process is not mistaken for a saturated host.
     */
    val cpuMachinePercent: Float
        get() = (cpuPercent / logicalCores.coerceAtLeast(1)).coerceIn(0f, 100f)

    /** Scheduling state parsed from [status]. */
    val state: ProcessStateUi
        get() = ProcessStateUi.fromCode(status)

    /** Load level of this process: critical from 80% and degraded from 50% of the machine's CPU or memory. */
    val loadStatus: StatusLevel
        get() = maxOf(
            loadStatusOf(cpuMachinePercent),
            loadStatusOf(memoryPercent),
        )

    private fun loadStatusOf(percent: Float): StatusLevel = when {
        percent >= CriticalLoadPercent -> StatusLevel.Critical
        percent >= WarningLoadPercent -> StatusLevel.Degraded
        else -> StatusLevel.Operational
    }

    companion object {
        const val WarningLoadPercent: Float = 50f
        const val CriticalLoadPercent: Float = 80f

        const val MissingUser: String = "—"

        fun from(data: ProcessUsage) = from(data, logicalCores = null)

        fun from(data: ProcessUsage, logicalCores: Int?) = ProcessUi(
            pid = data.pid,
            name = data.name,
            user = data.user ?: MissingUser,
            cpuPercent = data.cpuPercent,
            memoryPercent = data.memoryPercent,
            memoryRssBytes = data.memoryRssBytes,
            status = data.statusCode.orEmpty(),
            logicalCores = logicalCores ?: 1,
        )
    }
}
