package io.buszi.nascope.ui.dashboard.widgets.network

import kotlin.test.Test
import kotlin.test.assertEquals

class BitrateFormatterTest {

    @Test
    fun formatsZeroBitrate() {
        assertEquals("0 B/s", BitrateFormatter.formatBitrate(0L))
        assertEquals(Pair("0", "B/s"), BitrateFormatter.formatBitrateParts(0L))
    }

    @Test
    fun formatsBytesPerSec() {
        assertEquals("450 B/s", BitrateFormatter.formatBitrate(450L))
        assertEquals(Pair("450", "B/s"), BitrateFormatter.formatBitrateParts(450L))
    }

    @Test
    fun formatsKiloBytesPerSec() {
        assertEquals("520.0 KB/s", BitrateFormatter.formatBitrate(520_000L))
        assertEquals(Pair("520.0", "KB/s"), BitrateFormatter.formatBitrateParts(520_000L))
    }

    @Test
    fun formatsMegaBytesPerSec() {
        assertEquals("14.8 MB/s", BitrateFormatter.formatBitrate(14_850_000L))
        assertEquals(Pair("14.8", "MB/s"), BitrateFormatter.formatBitrateParts(14_850_000L))
        assertEquals("3.2 MB/s", BitrateFormatter.formatBitrate(3_210_000L))
        assertEquals(Pair("3.2", "MB/s"), BitrateFormatter.formatBitrateParts(3_210_000L))
    }

    @Test
    fun formatsGigaBytesPerSec() {
        assertEquals("1.25 GB/s", BitrateFormatter.formatBitrate(1_250_000_000L))
        assertEquals(Pair("1.25", "GB/s"), BitrateFormatter.formatBitrateParts(1_250_000_000L))
    }

    @Test
    fun formatsCumulativeDataBytes() {
        assertEquals("0 B", BitrateFormatter.formatDataBytes(0L))
        assertEquals("512 B", BitrateFormatter.formatDataBytes(512L))
        assertEquals("450.0 KB", BitrateFormatter.formatDataBytes(450_000L))
        assertEquals("124.0 MB", BitrateFormatter.formatDataBytes(124_000_000L))
        assertEquals("842.3 GB", BitrateFormatter.formatDataBytes(842_300_000_000L))
        assertEquals("1.20 TB", BitrateFormatter.formatDataBytes(1_200_000_000_000L))
    }
}
