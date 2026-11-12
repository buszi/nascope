package io.buszi.nascope.ui.dashboard.widgets.sensors

import io.buszi.nascope.ui.dashboard.widgets.sensors.SensorsThermalsViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.sensors.SensorsThermalsViewModel.ViewState
import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.telemetry.SensorKind
import io.buszi.nascope.domain.model.telemetry.SensorReading
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveSensorsUseCase
import io.buszi.nascope.domain.telemetry.ObserveSensorsUseCase.Data
import io.buszi.nascope.domain.telemetry.ObserveSensorsUseCase.Sensor
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.model.FanSensorUi
import io.buszi.nascope.ui.common.model.SensorsUi
import io.buszi.nascope.ui.common.model.TemperatureSensorUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class SensorsThermalsViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val observeSensors = mockk<ObserveSensorsUseCase>()
    private val refreshTelemetry = mockk<RefreshTelemetryUseCase>()
    private val states = MutableStateFlow<TelemetryState<Data>>(TelemetryState.Loading)

    private val viewModel by lazy {
        every { observeSensors() } returns states
        SensorsThermalsViewModel(observeSensors = observeSensors, refreshTelemetry = refreshTelemetry)
    }

    private suspend fun TestScope.viewStates(vararg steps: TelemetryState<Data>): List<ViewState> {
        var result: List<ViewState> = emptyList()
        turbineScope {
            val turbine = viewModel.viewState.testIn(this)
            runCurrent()
            steps.forEach {
                states.value = it
                runCurrent()
            }
            result = turbine.cancelAndConsumeItems()
        }
        return result
    }

    @Test
    fun sensorsThermalsViewModelStartsLoadingThenShowsTheNasLikeGroupsWithoutFans() = runTest {
        assertEquals(
            listOf(ViewState(), ViewState(sensors = nasSensors, widgetState = WidgetState.Ready)),
            viewStates(TelemetryState.Ready(nasData, isStale = false)),
        )
    }

    @Test
    fun sensorsThermalsViewModelShowsFansWithTheirRpmAndStalledState() = runTest {
        val last = viewStates(TelemetryState.Ready(fanData, false)).last()

        assertEquals(listOf(FanSensorUi("fan1", 1200, listOf(1100f, 1200f)), FanSensorUi("fan2", 0)), last.sensors.fans)
        assertEquals(1, last.sensors.activeFansCount)
    }

    @Test
    fun sensorsThermalsViewModelShowsAHostWithoutReadingsAsReadyAndEmpty() = runTest {
        val last = viewStates(TelemetryState.Ready(Data(emptyList(), emptyList(), emptyList(), emptyList()), false)).last()

        assertEquals(ViewState(widgetState = WidgetState.Ready), last)
        assertEquals(true, last.sensors.isEmpty)
    }

    @Test
    fun sensorsThermalsViewModelShowsUnavailableWhenThePluginIsDisabledNeverAnError() = runTest {
        val last = viewStates(TelemetryState.Failed(TelemetryFailure.PluginUnavailable)).last()

        assertEquals(ViewState(widgetState = WidgetState.Unavailable), last)
    }

    @Test
    fun sensorsThermalsViewModelMarksStaleKeepsTheContentAndMapsConnectionFailures() = runTest {
        val states = viewStates(
            TelemetryState.Ready(nasData, false),
            TelemetryState.Ready(nasData, true),
            TelemetryState.Failed(TelemetryFailure.Connection(ConnectionFailure.Timeout)),
            TelemetryState.Loading,
        )

        assertEquals(WidgetState.Stale, states[2].widgetState)
        assertEquals(nasSensors, states[2].sensors)
        assertIs<WidgetState.Offline>(states[3].widgetState)
        assertEquals(nasSensors, states[3].sensors)
        assertEquals(ViewState(), states[4])
    }

    @Test
    fun sensorsThermalsViewModelRetryRefreshesTelemetry() = runTest {
        every { refreshTelemetry() } just runs

        viewModel.onViewAction(OnRetry)

        verifySequence {
            observeSensors()
            refreshTelemetry()
        }
    }

    companion object {
        private fun sensor(label: String, value: Float, kind: SensorKind = SensorKind.CpuTemperature, unit: String = "C", history: List<Float> = emptyList()) =
            Sensor(SensorReading(label, kind, value, unit, null, null), history)

        private val nasData = Data(
            cpu = listOf(sensor("Package id 0", 55f, history = listOf(54f, 55f)), sensor("Core 0", 52f)),
            drives = listOf(sensor("Composite", 40f)),
            fans = emptyList(),
            other = listOf(sensor("acpitz 0", 27f)),
        )

        private val nasSensors = SensorsUi(
            cpu = listOf(
                TemperatureSensorUi("Package id 0", 55f, 70f, 85f, listOf(54f, 55f)),
                TemperatureSensorUi("Core 0", 52f, 70f, 85f),
            ),
            drives = listOf(TemperatureSensorUi("Composite", 40f, 70f, 85f)),
            other = listOf(TemperatureSensorUi("acpitz 0", 27f, 70f, 85f)),
        )

        private val fanData = Data(
            cpu = emptyList(),
            drives = emptyList(),
            fans = listOf(
                sensor("fan1", 1200f, SensorKind.FanSpeed, "R", listOf(1100f, 1200f)),
                sensor("fan2", 0f, SensorKind.FanSpeed, "R"),
            ),
            other = emptyList(),
        )
    }
}
