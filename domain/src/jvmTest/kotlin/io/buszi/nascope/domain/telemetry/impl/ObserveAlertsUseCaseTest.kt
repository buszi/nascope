package io.buszi.nascope.domain.telemetry.impl

import app.cash.turbine.test
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.telemetry.AlertState
import io.buszi.nascope.domain.model.telemetry.GlancesAlert
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveAlertsUseCase
import io.buszi.nascope.domain.telemetry.Polled
import io.buszi.nascope.domain.telemetry.TelemetryPoller
import io.buszi.nascope.test.TestCoroutineRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

class ObserveAlertsUseCaseTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val alerts = MutableStateFlow<Polled<List<GlancesAlert>>>(Polled.Loading)
    private val serverTime = MutableStateFlow<Polled<Instant>>(Polled.Loading)
    private val poller = mockk<TelemetryPoller> {
        every { this@mockk.alerts } returns this@ObserveAlertsUseCaseTest.alerts
        every { this@mockk.serverTime } returns this@ObserveAlertsUseCaseTest.serverTime
    }
    private val useCase = ObserveAlertsUseCaseImpl(poller)

    private fun alert(type: String, state: AlertState, begin: Long, end: Long? = null) =
        GlancesAlert(type, state, begin, end, null, null, null, null, emptyList())

    private fun data(state: TelemetryState<ObserveAlertsUseCase.Data>) =
        assertIs<TelemetryState.Ready<ObserveAlertsUseCase.Data>>(state).data

    @Test
    fun splitsActiveFromHistoryAndSortsBoth() = runTest {
        val careful = alert("LOAD", AlertState.Careful, 100)
        val warnOld = alert("MEM", AlertState.Warning, 50)
        val warnNew = alert("MEMSWAP", AlertState.Warning, 90)
        val critical = alert("CPU", AlertState.Critical, 10)
        val unknown = alert("X", AlertState.Unknown, 200)
        val endedEarly = alert("A", AlertState.Warning, 1, end = 20)
        val endedLate = alert("B", AlertState.Critical, 2, end = 40)
        alerts.value = Polled.Value(
            listOf(endedEarly, careful, warnOld, endedLate, unknown, warnNew, critical),
            isStale = false,
        )
        useCase().test {
            val d = data(awaitItem())
            assertEquals(listOf(critical, warnNew, warnOld, careful, unknown), d.active)
            assertEquals(listOf(endedLate, endedEarly), d.history)
            assertEquals(null, d.serverTime)
        }
    }

    @Test
    fun capsTheHistoryKeepingTheMostRecent() = runTest {
        val ended = (1..ObserveAlertsUseCase.HISTORY_LIMIT + 10).map { alert("T$it", AlertState.Warning, it.toLong(), end = it + 1000L) }
        alerts.value = Polled.Value(ended, isStale = false)
        useCase().test {
            val d = data(awaitItem())
            assertEquals(ObserveAlertsUseCase.HISTORY_LIMIT, d.history.size)
            assertEquals("T${ObserveAlertsUseCase.HISTORY_LIMIT + 10}", d.history.first().type)
        }
    }

    @Test
    fun noAlertsIsReadyAndEmpty() = runTest {
        alerts.value = Polled.Value(emptyList(), isStale = false)
        useCase().test {
            val d = data(awaitItem())
            assertEquals(emptyList(), d.active + d.history)
        }
    }

    @Test
    fun theServerTimeComesFromTheNowPluginAndIsOnlyASecondary() = runTest {
        alerts.value = Polled.Value(emptyList(), isStale = false)
        useCase().test {
            assertEquals(null, data(awaitItem()).serverTime)
            serverTime.value = Polled.Value(Instant.fromEpochSeconds(1000), isStale = false)
            assertEquals(Instant.fromEpochSeconds(1000), data(awaitItem()).serverTime)
            serverTime.value = Polled.Failed(offline)
            assertEquals(null, data(awaitItem()).serverTime)
        }
    }

    @Test
    fun followsThePrimaryPluginThroughLoadingStaleAndFailed() = runTest {
        useCase().test {
            assertEquals(TelemetryState.Loading, awaitItem())
            alerts.value = Polled.Value(emptyList(), isStale = true)
            assertEquals(true, assertIs<TelemetryState.Ready<*>>(awaitItem()).isStale)
            alerts.value = Polled.Failed(TelemetryFailure.PluginUnavailable)
            assertEquals(TelemetryState.Failed(TelemetryFailure.PluginUnavailable), awaitItem())
            runCurrent()
        }
    }

    private val offline = TelemetryFailure.Connection(ConnectionFailure.ConnectionRefused)
}
