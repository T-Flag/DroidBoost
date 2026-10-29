package fr.tflag.droidboost.core.monitor

object BatteryHealth {

    /** Design capacities (mAh) of the devices this app targets, keyed by `Build.DEVICE`. */
    private val DESIGN_CAPACITY = mapOf(
        "husky" to 5050, // Pixel 8 Pro
    )

    fun designCapacityMah(device: String): Int? = DESIGN_CAPACITY[device]

    /**
     * Full capacity as the fuel gauge currently models it: the charge counter scaled to 100 %.
     * Below 10 % the rounding of the level makes the ratio too noisy.
     */
    fun gaugeFullCapacityMah(chargeCounterMah: Double, levelPercent: Int): Double? =
        if (levelPercent >= 10) chargeCounterMah * 100 / levelPercent else null

    fun healthFraction(estimatedMah: Double, designMah: Int): Float = (estimatedMah / designMah).toFloat()

    /** Median of session estimates: robust to a few sessions skewed by heavy use while charging. */
    fun medianEstimate(estimates: List<Double>): Double? {
        if (estimates.isEmpty()) return null
        val sorted = estimates.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }

    /** Average discharge rate in %/h over consecutive unplugged level samples. */
    fun dischargeRatePerHour(samples: List<Pair<Long, Double>>, plugged: List<Pair<Long, Double>>): Double? {
        val pluggedTimes = plugged.filter { it.second > 0.5 }.map { it.first }.toSet()
        var dropped = 0.0
        var hours = 0.0
        samples.zipWithNext().forEach { (a, b) ->
            if (a.first in pluggedTimes || b.first in pluggedTimes) return@forEach
            val dtHours = (b.first - a.first) / 3_600_000.0
            val delta = a.second - b.second
            // Gaps over 2 h or a rising level mean the series was interrupted or charging.
            if (dtHours <= 0 || dtHours > 2 || delta < 0) return@forEach
            dropped += delta
            hours += dtHours
        }
        return if (hours >= 0.5) dropped / hours else null
    }
}
