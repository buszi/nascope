package io.buszi.nascope.ui.dashboard.widgets.network

import kotlin.math.abs
import kotlin.math.round

/**
 * Utility for formatting network throughput bitrates and cumulative data transfer volumes.
 */
object BitrateFormatter {

    private const val OneKilo: Double = 1_000.0
    private const val OneMega: Double = 1_000_000.0
    private const val OneGiga: Double = 1_000_000_000.0
    private const val OneTera: Double = 1_000_000_000_000.0

    /**
     * Splits bitrate into a numerical value string and unit label for metric displays.
     * e.g., (14_850_000L) -> Pair("14.8", "MB/s")
     */
    fun formatBitrateParts(bytesPerSec: Long): Pair<String, String> {
        val absBytes = abs(bytesPerSec).toDouble()
        return when {
            absBytes >= OneGiga -> {
                val value = formatDecimal(bytesPerSec / OneGiga, decimals = 2)
                Pair(value, "GB/s")
            }
            absBytes >= OneMega -> {
                val value = formatDecimal(bytesPerSec / OneMega, decimals = 1)
                Pair(value, "MB/s")
            }
            absBytes >= OneKilo -> {
                val value = formatDecimal(bytesPerSec / OneKilo, decimals = 1)
                Pair(value, "KB/s")
            }
            else -> {
                Pair(bytesPerSec.toString(), "B/s")
            }
        }
    }

    /**
     * Formats bitrate as a combined single string, e.g. "14.8 MB/s".
     */
    fun formatBitrate(bytesPerSec: Long): String {
        val (value, unit) = formatBitrateParts(bytesPerSec)
        return "$value $unit"
    }

    /**
     * Formats cumulative transfer bytes, e.g. "842.3 GB", "1.20 TB".
     */
    fun formatDataBytes(bytes: Long): String {
        val absBytes = abs(bytes).toDouble()
        return when {
            absBytes >= OneTera -> "${formatDecimal(bytes / OneTera, decimals = 2)} TB"
            absBytes >= OneGiga -> "${formatDecimal(bytes / OneGiga, decimals = 1)} GB"
            absBytes >= OneMega -> "${formatDecimal(bytes / OneMega, decimals = 1)} MB"
            absBytes >= OneKilo -> "${formatDecimal(bytes / OneKilo, decimals = 1)} KB"
            else -> "$bytes B"
        }
    }

    private fun formatDecimal(value: Double, decimals: Int): String {
        val factor = when (decimals) {
            1 -> 10.0
            2 -> 100.0
            3 -> 1000.0
            else -> 10.0
        }
        val rounded = round(value * factor).toLong()
        val whole = rounded / factor.toLong()
        val fraction = abs(rounded % factor.toLong())
        val fractionStr = fraction.toString().padStart(decimals, '0')
        return "$whole.$fractionStr"
    }
}
