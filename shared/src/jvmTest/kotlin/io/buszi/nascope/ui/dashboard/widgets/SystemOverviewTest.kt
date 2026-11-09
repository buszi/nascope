package io.buszi.nascope.ui.dashboard.widgets

import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SystemOverviewTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `HostPlatform fromIdentifier resolves platforms correctly`() {
        assertEquals(HostPlatform.Ubuntu, HostPlatform.fromIdentifier("Ubuntu 24.04.1 LTS"))
        assertEquals(HostPlatform.Debian, HostPlatform.fromIdentifier("Debian GNU/Linux 12"))
        assertEquals(HostPlatform.Synology, HostPlatform.fromIdentifier("Synology DSM 7.2"))
        assertEquals(HostPlatform.TrueNas, HostPlatform.fromIdentifier("TrueNAS-SCALE-23.10"))
        assertEquals(HostPlatform.FreeBsd, HostPlatform.fromIdentifier("FreeBSD 13.1-RELEASE"))
        assertEquals(HostPlatform.MacOS, HostPlatform.fromIdentifier("Darwin Kernel Version 24.1.0"))
        assertEquals(HostPlatform.Windows, HostPlatform.fromIdentifier("Windows-10-10.0.20348"))
        assertEquals(HostPlatform.Docker, HostPlatform.fromIdentifier("Linux docker-container"))
        assertEquals(HostPlatform.Linux, HostPlatform.fromIdentifier("Linux 6.8.0-generic"))
        assertEquals(HostPlatform.Generic, HostPlatform.fromIdentifier("UnknownOS 1.0"))
        assertEquals(HostPlatform.Generic, HostPlatform.fromIdentifier(null))
    }

    @Test
    fun `LoadAverages normalizes load ratio against cpu cores`() {
        val load = LoadAverages(
            oneMinute = 2.0f,
            fiveMinute = 3.2f,
            fifteenMinute = 4.0f,
            cpuCores = 4,
        )

        assertEquals(0.5f, load.oneMinuteRatio)
        assertEquals(0.8f, load.fiveMinuteRatio)
        assertEquals(1.0f, load.fifteenMinuteRatio)

        assertEquals(StatusLevel.Operational, load.statusFor(load.oneMinute))
        assertEquals(StatusLevel.Degraded, load.statusFor(load.fiveMinute))
        assertEquals(StatusLevel.Critical, load.statusFor(load.fifteenMinute))
    }

    @Test
    fun `SystemOverviewPreviewParameterProvider provides valid synthetic presets`() {
        val provider = SystemOverviewPreviewParameterProvider()
        val presets = provider.values.toList()

        assertEquals(5, presets.size)
        assertTrue(presets.all { it.hostname.isNotBlank() })
        assertTrue(presets.all { it.uptime.isNotBlank() })
        assertTrue(presets.all { it.osName.isNotBlank() })
        assertTrue(presets.all { it.kernelRelease.isNotBlank() })
        assertTrue(presets.all { it.loadAverages.cpuCores > 0 })
    }

    @Test
    fun `SystemOverviewViewModel subscribes to TelemetryStateHolder and handles retry`() = runTest(testDispatcher) {
        val stateHolder = TelemetryStateHolder()
        val viewModel = SystemOverviewViewModel(stateHolder)

        assertEquals(WidgetState.Ready, viewModel.uiState.value.state)
        assertEquals("alpha-node-01.local", viewModel.uiState.value.data.hostname)

        viewModel.retry()
        assertEquals(WidgetState.Ready, viewModel.uiState.value.state)
    }
}
