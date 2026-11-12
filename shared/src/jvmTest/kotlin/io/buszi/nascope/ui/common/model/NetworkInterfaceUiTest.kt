package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.telemetry.NetworkInterfaceUsage
import io.buszi.nascope.domain.telemetry.ObserveNetworkUseCase
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkInterfaceUiTest {

    @Test
    fun peaksAreHistoryMaximaInBytesPerSecond() {
        val iface = iface(rxHistory = listOf(1f, 21_000_000f, 3f), txHistory = listOf(500_000f, 4_500_000f))

        assertEquals(21_000_000L, iface.peakRxBytesPerSec)
        assertEquals(4_500_000L, iface.peakTxBytesPerSec)
    }

    @Test
    fun peaksAreZeroWithoutHistory() {
        assertEquals(0L, iface().peakRxBytesPerSec)
        assertEquals(0L, iface().peakTxBytesPerSec)
    }

    @Test
    fun linkUtilizationUsesBusierDirection() {
        // 12.5 MB/s = 100 Mbps on a 1000 Mbps link
        val iface = iface(rxBytesPerSec = 12_500_000L, txBytesPerSec = 1_000_000L, linkSpeedMbps = 1000)

        assertEquals(0.1f, iface.linkUtilization)
    }

    @Test
    fun linkUtilizationIsClampedAndNullWithoutAKnownSpeed() {
        assertEquals(1f, iface(rxBytesPerSec = 500_000_000L, linkSpeedMbps = 100).linkUtilization)
        assertNull(iface(rxBytesPerSec = 1_000L, linkSpeedMbps = null).linkUtilization)
        assertNull(iface(rxBytesPerSec = 1_000L, linkSpeedMbps = 0).linkUtilization)
    }

    @Test
    fun overallStatusIsCriticalOnlyWhenNothingIsUp() {
        assertEquals(StatusLevel.Critical, NetworkInterfaceUi.overallStatus(emptyList()))
        assertEquals(StatusLevel.Critical, NetworkInterfaceUi.overallStatus(listOf(iface(isUp = false))))
        assertEquals(
            StatusLevel.Operational,
            NetworkInterfaceUi.overallStatus(listOf(iface(isUp = false), iface(isUp = true))),
        )
    }

    @Test
    fun fromTreatsAnUnknownLinkStateAsUpAndConvertsTheSpeedToMbps() {
        val ui = NetworkInterfaceUi.from(
            ObserveNetworkUseCase.Interface(
                usage = NetworkInterfaceUsage("eth0", null, null, 1_000_000_000L, 10, 20, 30, 40),
                rxHistory = listOf(1f),
                txHistory = listOf(2f),
            ),
        )

        assertEquals(
            NetworkInterfaceUi("eth0", true, 10, 20, 30, 40, listOf(1f), listOf(2f), linkSpeedMbps = 1000),
            ui,
        )
    }

    @Test
    fun fromAllLeavesOutTheLoopbackInterface() {
        val data = ObserveNetworkUseCase.Data(
            listOf("lo", "eth0", "docker0").map {
                ObserveNetworkUseCase.Interface(
                    NetworkInterfaceUsage(it, null, true, null, 0, 0, 0, 0),
                    emptyList(),
                    emptyList(),
                )
            },
        )

        assertEquals(listOf("eth0", "docker0"), NetworkInterfaceUi.fromAll(data).map { it.name })
    }

    private fun iface(
        isUp: Boolean = true,
        rxBytesPerSec: Long = 0L,
        txBytesPerSec: Long = 0L,
        rxHistory: List<Float> = emptyList(),
        txHistory: List<Float> = emptyList(),
        linkSpeedMbps: Int? = null,
    ) = NetworkInterfaceUi(
        name = "eth0",
        isUp = isUp,
        rxBytesPerSec = rxBytesPerSec,
        txBytesPerSec = txBytesPerSec,
        totalRxBytes = 0L,
        totalTxBytes = 0L,
        rxHistory = rxHistory,
        txHistory = txHistory,
        linkSpeedMbps = linkSpeedMbps,
    )
}
