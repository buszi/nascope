package io.buszi.nascope.domain.model.telemetry

/** The severity of an alert in the Glances alert log. [Unknown] covers any state this app does not know. */
enum class AlertState { Careful, Warning, Critical, Unknown }

/**
 * One entry of the Glances alert log (`alert` plugin).
 *
 * All times are epoch seconds from the **server's** clock: compare them with the server's time (see
 * `ObserveAlertsUseCase.Data.serverTime`), never with the device's.
 */
data class GlancesAlert(
    /** The metric as Glances names it, e.g. `MEM`, `MEMSWAP`, `CPU_USER`, `LOAD`. Not interpreted here. */
    val type: String,
    val state: AlertState,
    val beginEpochSeconds: Long,
    /** `null` while the alert is still active. */
    val endEpochSeconds: Long?,
    val min: Double?,
    val max: Double?,
    val avg: Double?,
    /** Glances' own description, e.g. "High swap (paging) usage"; `null` when it gives none. */
    val message: String?,
    /** Names of the processes Glances blames, if any. */
    val topProcesses: List<String>,
) {
    val isActive: Boolean get() = endEpochSeconds == null
}
