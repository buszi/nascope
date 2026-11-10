package io.buszi.nascope.ui.dashboard.widgets.sensors

import app.cash.turbine.turbineScope
import io.buszi.nascope.data.telemetry.FanSensorData
import io.buszi.nascope.data.telemetry.SensorsThermalsData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.data.telemetry.TemperatureSensorData
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.sensors.SensorsThermalsViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.sensors.SensorsThermalsViewModel.ViewState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SensorsThermalsViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val telemetryStateHolder = mockk<TelemetryStateHolder>()

    private val viewModel by lazy {
        SensorsThermalsViewModel(
            telemetryStateHolder = telemetryStateHolder,
        )
    }

    @Test
    fun sensorsViewModelEmitsReadyStateWithNominalTelemetry() = runTest {
        turbineScope {
            val sensorsFlow = MutableStateFlow(testSensorsData)
            every { telemetryStateHolder.sensorsThermals } returns sensorsFlow

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
    fun warningThresholdTriggersDegradedOverallStatus() = runTest {
        turbineScope {
            val elevatedData = testSensorsData.copy(
                cpuTemperature = testCpuSensor.copy(currentTempC = 78.0f),
            )
            val sensorsFlow = MutableStateFlow(elevatedData)
            every { telemetryStateHolder.sensorsThermals } returns sensorsFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            val states = viewStateTurbine.cancelAndConsumeItems()
            assertEquals(StatusLevel.Degraded, states.last().overallStatus)
        }
    }

    @Test
    fun criticalThresholdTriggersCriticalOverallStatus() = runTest {
        turbineScope {
            val criticalData = testSensorsData.copy(
                cpuTemperature = testCpuSensor.copy(currentTempC = 91.0f),
            )
            val sensorsFlow = MutableStateFlow(criticalData)
            every { telemetryStateHolder.sensorsThermals } returns sensorsFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            val states = viewStateTurbine.cancelAndConsumeItems()
            assertEquals(StatusLevel.Critical, states.last().overallStatus)
        }
    }

    @Test
    fun retryInvokesStateHolderRefresh() = runTest {
        turbineScope {
            val sensorsFlow = MutableStateFlow(testSensorsData)
            every { telemetryStateHolder.sensorsThermals } returns sensorsFlow
            every { telemetryStateHolder.refresh() } just runs

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            viewModel.onViewAction(OnRetry)
            runCurrent()

            assertEquals(
                listOf(
                    initialViewState,
                    readyViewState,
                    readyViewState.copy(widgetState = WidgetState.Loading),
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                telemetryStateHolder.sensorsThermals
                telemetryStateHolder.refresh()
            }
        }
    }

    private companion object {
        val testCpuSensor = TemperatureSensorData(
            label = "CPU Package",
            currentTempC = 58.0f,
            warningThresholdC = 75.0f,
            criticalThresholdC = 85.0f,
        )

        val testDriveSensor = TemperatureSensorData(
            label = "nvme0n1",
            currentTempC = 41.0f,
            warningThresholdC = 65.0f,
            criticalThresholdC = 75.0f,
        )

        val testFan = FanSensorData(
            label = "CPU Fan",
            rpm = 1380,
            maxRpm = 2200,
        )

        val testSensorsData = SensorsThermalsData(
            cpuTemperature = testCpuSensor,
            driveTemperatures = listOf(testDriveSensor),
            fans = listOf(testFan),
        )

        val initialViewState = ViewState()

        val readyViewState = ViewState(
            overallStatus = StatusLevel.Operational,
            cpuTemperature = testCpuSensor,
            driveTemperatures = listOf(testDriveSensor),
            fans = listOf(testFan),
            activeFansCount = 1,
            maxTemperatureC = 58.0f,
            widgetState = WidgetState.Ready,
        )
    }
}
