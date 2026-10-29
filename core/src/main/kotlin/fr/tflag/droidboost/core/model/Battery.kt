package fr.tflag.droidboost.core.model

import kotlin.math.abs

object BatteryMath {

    /**
     * Magnitude in mA of `BATTERY_PROPERTY_CURRENT_NOW`, which is in µA as documented and as the Pixel 8 Pro reports it.
     * Guessing the unit from the magnitude misread currents under 10 mA (charge held at 80 %, load matching the
     * charger) as amps, e.g. 4 692 µA shown as 20 W.
     */
    fun currentMilliAmps(raw: Int): Double = abs(raw) / 1000.0

    fun powerWatts(voltageMilliVolts: Int, currentMilliAmps: Double): Double =
        voltageMilliVolts / 1000.0 * currentMilliAmps / 1000.0

    /** `EXTRA_TEMPERATURE` is in tenths of a degree Celsius. */
    fun celsius(tenths: Int): Float = tenths / 10f

    /** Share of time spent in deep sleep since boot. */
    fun deepSleepFraction(elapsedRealtimeMs: Long, uptimeMs: Long): Float =
        if (elapsedRealtimeMs <= 0) 0f else ((elapsedRealtimeMs - uptimeMs).toFloat() / elapsedRealtimeMs).coerceIn(0f, 1f)
}
