package fr.tflag.droidboost.core.journal

/** A metric averaged over two periods, e.g. before and after a system update. */
data class ComparedMetric(val label: String, val before: Double?, val after: Double?, val higherIsWorse: Boolean) {
    val delta: Double? get() = if (before != null && after != null) after - before else null

    /** Relative change, null when undefined (missing side or zero baseline). */
    val relativeChange: Double?
        get() = delta?.takeIf { before != null && kotlin.math.abs(before) > 1e-9 }?.let { it / kotlin.math.abs(before!!) }

    /** True when the change is large enough (≥ 10 %) and in the bad direction. */
    val isRegression: Boolean
        get() {
            val change = relativeChange ?: return false
            return if (higherIsWorse) change >= REGRESSION_THRESHOLD else change <= -REGRESSION_THRESHOLD
        }

    private companion object {
        const val REGRESSION_THRESHOLD = 0.10
    }
}

object PeriodComparison {
    fun average(values: List<Double>): Double? = values.takeIf { it.isNotEmpty() }?.average()
}
