package fr.tflag.droidboost.core.monitor

/** Fires once when the battery reaches [thresholdPercent] while charging; re-arms after unplugging. */
class ChargeLevelAlert(private val thresholdPercent: Int) {
    private var firedThisSession = false

    fun check(plugged: Boolean, levelPercent: Int): Boolean {
        if (!plugged) {
            firedThisSession = false
            return false
        }
        if (levelPercent >= thresholdPercent && !firedThisSession) {
            firedThisSession = true
            return true
        }
        return false
    }
}

/**
 * Fires once when [value] reaches [threshold], then waits until it drops below `threshold - hysteresis`
 * before it can fire again, so a value hovering around the threshold does not spam notifications.
 */
class ThresholdAlert(private val threshold: Float, private val hysteresis: Float) {
    private var armed = true

    fun check(value: Float): Boolean {
        if (armed && value >= threshold) {
            armed = false
            return true
        }
        if (!armed && value <= threshold - hysteresis) armed = true
        return false
    }
}
