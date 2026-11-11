package io.buszi.nascope.ui.dashboard.widgets.sensors

import app.cash.turbine.turbineScope
import io.buszi.nascope.data.telemetry.FanSensorData
import io.buszi.nascope.data.telemetry.SensorsThermalsData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.data.telemetry.TemperatureSensorData
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.FanSensorUi
import io.buszi.nascope.ui.common.model.TemperatureSensorUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.sensors.SensorsDetailsViewModel.ViewState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SensorsDetailsViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val telemetryStateHolder = mockk<TelemetryStateHolder>()

    private val viewModel by lazy {
        SensorsDetailsViewModel(
            telemetryStateHolder = telemetryStateHolder,
        )
    }

    @Test
    fun sensorsDetailsViewModelEmitsReadyStateWithTelemetryData() = runTest {
        turbineScope {
            every { telemetryStateHolder.sensorsThermals } returns MutableStateFlow(sensorsData)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                listOf(
                    initialViewState,
                    readyViewState,
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                telemetryStateHolder.sensorsThermals
            }
        }
    }

    @Test
    fun sensorsDetailsViewStateComputesAggregates() {
        val state = readyViewState

        assertEquals(listOf("CPU Package", "sda"), state.temperatures.map { it.label })
        assertEquals("CPU Package", state.hottest?.label)
        assertEquals(1, state.atRiskCount)
        assertEquals(1, state.activeFansCount)
        assertEquals(25f, state.averageFanDutyPercent, absoluteTolerance = 0.001f)
        assertEquals(StatusLevel.Degraded, state.overallStatus)
    }

    @Test
    fun sensorsDetailsViewStateHandlesNoSensors() {
        val state = ViewState(widgetState = WidgetState.Ready)

        assertNull(state.hottest)
        assertEquals(0, state.atRiskCount)
        assertEquals(0f, state.averageFanDutyPercent)
        assertEquals(StatusLevel.Operational, state.overallStatus)
    }

    companion object {
        private val sensorsData = SensorsThermalsData(
            cpuTemperature = TemperatureSensorData(
                label = "CPU Package",
                currentTempC = 78f,
                warningThresholdC = 75f,
                criticalThresholdC = 85f,
                history = listOf(70f, 78f),
            ),
            driveTemperatures = listOf(
                TemperatureSensorData(
                    label = "sda",
                    currentTempC = 34f,
                    warningThresholdC = 50f,
                    criticalThresholdC = 60f,
                ),
            ),
            fans = listOf(
                FanSensorData(label = "CPU Fan", rpm = 1100, maxRpm = 2200, rpmHistory = listOf(1000f, 1100f)),
                FanSensorData(label = "Chassis", rpm = 0, maxRpm = 1800),
            ),
        )

        private val initialViewState = ViewState()

        private val readyViewState = ViewState(
            cpuTemperature = sensorsData.cpuTemperature?.let(TemperatureSensorUi::from),
            driveTemperatures = sensorsData.driveTemperatures.map(TemperatureSensorUi::from),
            fans = sensorsData.fans.map(FanSensorUi::from),
            widgetState = WidgetState.Ready,
        )
    }
}
