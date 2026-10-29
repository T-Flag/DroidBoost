package fr.tflag.droidboost.core.model

import kotlin.math.abs

object BatteryMath {

    /**
     * `BATTERY_PROPERTY_CURRENT_NOW` is documented in µA, but some devices report mA.
     * Values this large can only be microamps on a phone.
     */
    private const val MICROAMP_THRESHOLD = 10_000

    /** Magnitude of the current in mA, regardless of the device's unit and sign convention. */
    fun currentMilliAmps(raw: Int): Double =
        if (abs(raw) >= MICROAMP_THRESHOLD) abs(raw) / 1000.0 else abs(raw).toDouble()

    fun powerWatts(voltageMilliVolts: Int, currentMilliAmps: Double): Double =
        voltageMilliVolts / 1000.0 * currentMilliAmps / 1000.0

    /** `EXTRA_TEMPERATURE` is in tenths of a degree Celsius. */
    fun celsius(tenths: Int): Float = tenths / 10f

    /** Share of time spent in deep sleep since boot. */
    fun deepSleepFraction(elapsedRealtimeMs: Long, uptimeMs: Long): Float =
        if (elapsedRealtimeMs <= 0) 0f else ((elapsedRealtimeMs - uptimeMs).toFloat() / elapsedRealtimeMs).coerceIn(0f, 1f)
}
