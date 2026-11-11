package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.data.telemetry.ProcessInfo

/**
 * UI representation of a process scheduling state, parsed from the single-letter status code.
 */
@Immutable
enum class ProcessStateUi(val code: Char) {
    Running('R'),
    Sleeping('S'),
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
    val user: String,
    val cpuPercent: Float,
    val memoryPercent: Float,
    val memoryRssBytes: Long = 0L,
    val status: String = "R",
) {
    /** Scheduling state parsed from [status]. */
    val state: ProcessStateUi
        get() = ProcessStateUi.fromCode(status)

    /** Load level of this process: critical from 80% and degraded from 50% CPU or memory. */
    val loadStatus: StatusLevel
        get() = maxOf(
            loadStatusOf(cpuPercent),
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

        fun from(data: ProcessInfo) = ProcessUi(
            pid = data.pid,
            name = data.name,
            user = data.user,
            cpuPercent = data.cpuPercent,
            memoryPercent = data.memoryPercent,
            memoryRssBytes = data.memoryRssBytes,
            status = data.status,
        )
    }
}
