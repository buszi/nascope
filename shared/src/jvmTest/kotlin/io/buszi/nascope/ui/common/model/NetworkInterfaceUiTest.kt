package io.buszi.nascope.ui.common.model

import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkInterfaceUiTest {

    @Test
    fun peaksAreHistoryMaximaConvertedToBytesPerSecond() {
        val iface = iface(rxHistory = listOf(1f, 21f, 3f), txHistory = listOf(0.5f, 4.5f))

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
    fun linkUtilizationIsClampedAndNullWithoutSpeed() {
        assertEquals(1f, iface(rxBytesPerSec = 500_000_000L, linkSpeedMbps = 100).linkUtilization)
        assertNull(iface(rxBytesPerSec = 1_000L, linkSpeedMbps = 0).linkUtilization)
    }

    @Test
    fun totalErrorsSumsBothDirections() {
        assertEquals(15L, iface(rxErrors = 12L, txErrors = 3L).totalErrors)
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

    private fun iface(
        isUp: Boolean = true,
        rxBytesPerSec: Long = 0L,
        txBytesPerSec: Long = 0L,
        rxHistory: List<Float> = emptyList(),
        txHistory: List<Float> = emptyList(),
        linkSpeedMbps: Int = 0,
        rxErrors: Long = 0L,
        txErrors: Long = 0L,
    ) = NetworkInterfaceUi(
        name = "eth0",
        isUp = isUp,
        ipAddress = "10.0.0.1/24",
        rxBytesPerSec = rxBytesPerSec,
        txBytesPerSec = txBytesPerSec,
        totalRxBytes = 0L,
        totalTxBytes = 0L,
        rxHistory = rxHistory,
        txHistory = txHistory,
        linkSpeedMbps = linkSpeedMbps,
        rxErrors = rxErrors,
        txErrors = txErrors,
    )
}
