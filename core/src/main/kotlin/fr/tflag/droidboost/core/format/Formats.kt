package fr.tflag.droidboost.core.format

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/** French-formatted values for the UI. */
object Formats {

    private val symbols = DecimalFormatSymbols(Locale.FRANCE)
    private val oneDecimal = DecimalFormat("0.0", symbols)
    private val twoDecimals = DecimalFormat("0.00", symbols)
    private val integer = DecimalFormat("#,##0", symbols)

    private val BYTE_UNITS = listOf("o", "Ko", "Mo", "Go", "To")

    fun bytes(value: Long): String {
        if (abs(value) < 1024) return "$value o"
        var scaled = value.toDouble()
        var unit = 0
        while (abs(scaled) >= 1024 && unit < BYTE_UNITS.lastIndex) {
            scaled /= 1024
            unit++
        }
        val number = if (abs(scaled) >= 100) integer.format(scaled) else oneDecimal.format(scaled)
        return "$number ${BYTE_UNITS[unit]}"
    }

    fun bytesPerSecond(value: Long): String = "${bytes(value)}/s"

    fun frequencyKhz(khz: Long): String =
        if (khz >= 1_000_000) "${twoDecimals.format(khz / 1_000_000.0)} GHz" else "${integer.format(khz / 1000)} MHz"

    fun percent(fraction: Float): String = "${integer.format((fraction * 100).toDouble())} %"

    fun celsius(value: Float): String = "${oneDecimal.format(value.toDouble())} °C"

    fun milliAmps(value: Double): String = "${integer.format(value)} mA"

    fun watts(value: Double): String = "${twoDecimals.format(value)} W"

    fun volts(milliVolts: Int): String = "${twoDecimals.format(milliVolts / 1000.0)} V"

    fun number(value: Long): String = integer.format(value)

    /** Relative past time, e.g. "il y a 3 j". */
    fun ago(deltaMillis: Long): String {
        val minutes = deltaMillis / 60_000
        return when {
            minutes < 1 -> "à l'instant"
            minutes < 60 -> "il y a $minutes min"
            minutes < 24 * 60 -> "il y a ${minutes / 60} h"
            minutes < 60 * 24 * 60 -> "il y a ${minutes / (24 * 60)} j"
            else -> "il y a ${minutes / (30 * 24 * 60)} mois"
        }
    }

    fun duration(millis: Long): String {
        val totalMinutes = millis / 60_000
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> "$days j $hours h"
            hours > 0 -> "$hours h ${minutes.toString().padStart(2, '0')}"
            totalMinutes > 0 -> "$minutes min"
            else -> "${millis / 1000} s"
        }
    }
}
