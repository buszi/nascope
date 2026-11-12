package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.telemetry.AlertState
import io.buszi.nascope.domain.model.telemetry.GlancesAlert
import io.buszi.nascope.domain.telemetry.ObserveAlertsUseCase
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class AlertUiTest {

    private fun alert(
        type: String = "MEMSWAP",
        state: AlertState = AlertState.Warning,
        begin: Long = 1_000,
        end: Long? = null,
    ) = GlancesAlert(type, state, begin, end, 71.24, 76.46, 73.54, "High swap (paging) usage", listOf("php"))

    @Test
    fun typesGetReadableLabelsAndUnknownOnesStayAsTheyAre() {
        assertEquals("MEMORY", AlertUi.typeLabel("MEM"))
        assertEquals("SWAP", AlertUi.typeLabel("MEMSWAP"))
        assertEquals("CPU USER", AlertUi.typeLabel("CPU_USER"))
        assertEquals("LOAD", AlertUi.typeLabel("LOAD"))
        assertEquals("FS_/data", AlertUi.typeLabel("FS_/data"))
    }

    @Test
    fun statesMapToStatusLevels() {
        fun status(state: AlertState) = AlertUi.from(alert(state = state), 2_000).status
        assertEquals(StatusLevel.Critical, status(AlertState.Critical))
        assertEquals(StatusLevel.Degraded, status(AlertState.Warning))
        assertEquals(StatusLevel.Degraded, status(AlertState.Careful))
        assertEquals(StatusLevel.Degraded, status(AlertState.Unknown))
    }

    @Test
    fun anActiveAlertRunsFromItsBeginToTheServerTime() {
        val ui = AlertUi.from(alert(begin = 1_000), serverTimeEpochSeconds = 1_000 + 3 * 3600 + 5 * 60 + 9)
        assertEquals("3h 05m", ui.duration)
        assertEquals(true, ui.isActive)
    }

    @Test
    fun anActiveAlertHasNoDurationWithoutTheServerTime() {
        assertNull(AlertUi.from(alert(), serverTimeEpochSeconds = null).duration)
    }

    @Test
    fun anEndedAlertLastedFromBeginToEndWhateverTheTime() {
        val ui = AlertUi.from(alert(begin = 1_000, end = 1_000 + 12 * 60), serverTimeEpochSeconds = null)
        assertEquals("12m", ui.duration)
        assertEquals(false, ui.isActive)
    }

    @Test
    fun shortAndNegativeDurationsAreUnderAMinute() {
        assertEquals("<1m", AlertUi.from(alert(begin = 1_000), 1_030).duration)
        assertEquals("<1m", AlertUi.from(alert(begin = 1_000), 900).duration) // clock skew never goes negative
    }

    @Test
    fun figuresAreRoundedToOneDecimalAndOptional() {
        val ui = AlertUi.from(alert(), 2_000)
        assertEquals("71.2", ui.min)
        assertEquals("73.5", ui.avg)
        assertEquals("76.5", ui.max)
        assertNull(AlertUi.from(alert().copy(min = null), 2_000).min)
    }

    @Test
    fun theLogSplitsIntoActiveAndHistoryAndDrivesTheBadge() {
        val data = ObserveAlertsUseCase.Data(
            active = listOf(alert(state = AlertState.Critical), alert(type = "MEM")),
            history = listOf(alert(end = 1_500)),
            serverTime = Instant.fromEpochSeconds(2_000),
        )
        val ui = AlertsUi.from(data)
        assertEquals(2, ui.activeCount)
        assertEquals(1, ui.history.size)
        assertEquals(StatusLevel.Critical, ui.worstStatus)
        assertEquals(AlertBadgeUi(2, StatusLevel.Critical), AlertBadgeUi.from(ui))
    }

    @Test
    fun noActiveAlertsMeansNoBadge() {
        val ui = AlertsUi.from(ObserveAlertsUseCase.Data(emptyList(), listOf(alert(end = 1_500)), null))
        assertNull(ui.worstStatus)
        assertNull(AlertBadgeUi.from(ui))
    }
}
