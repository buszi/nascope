package io.buszi.nascope.domain.telemetry.impl

import app.cash.turbine.test
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.telemetry.CpuUsage
import io.buszi.nascope.domain.model.telemetry.ProgramUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveProgramsUseCase.Data
import io.buszi.nascope.domain.telemetry.Polled
import io.buszi.nascope.domain.telemetry.TelemetryPoller
import io.buszi.nascope.test.TestCoroutineRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveProgramsUseCaseTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val programs = MutableStateFlow<Polled<List<ProgramUsage>>>(Polled.Loading)
    private val cpu = MutableStateFlow<Polled<CpuUsage>>(Polled.Loading)
    private val poller = mockk<TelemetryPoller> {
        every { this@mockk.programs } returns this@ObserveProgramsUseCaseTest.programs
        every { this@mockk.cpu } returns this@ObserveProgramsUseCaseTest.cpu
    }
    private val useCase = ObserveProgramsUseCaseImpl(poller)
    private val php = ProgramUsage("php-fpm", 12, 30f, 4f, 1_000, 24, "www", "S")

    @Test
    fun followsThePrimaryPluginAndCarriesTheLogicalCores() = runTest {
        useCase().test {
            assertEquals(TelemetryState.Loading, awaitItem())
            programs.value = Polled.Value(listOf(php), isStale = false)
            assertEquals(TelemetryState.Ready(Data(listOf(php), null), false), awaitItem())
            cpu.value = Polled.Value(CpuUsage(10f, null, null, null, null, 4), isStale = false)
            assertEquals(TelemetryState.Ready(Data(listOf(php), 4), false), awaitItem())
            programs.value = Polled.Value(listOf(php), isStale = true)
            assertEquals(TelemetryState.Ready(Data(listOf(php), 4), true), awaitItem())
            programs.value = Polled.Failed(TelemetryFailure.PluginUnavailable)
            assertEquals(TelemetryState.Failed(TelemetryFailure.PluginUnavailable), awaitItem())
            cpu.value = Polled.Failed(TelemetryFailure.Connection(ConnectionFailure.ConnectionRefused))
            assertEquals(TelemetryState.Failed(TelemetryFailure.PluginUnavailable), awaitItem())
        }
    }
}
