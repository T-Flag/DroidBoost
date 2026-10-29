package fr.tflag.droidboost.core.monitor

/** One battery reading taken by the monitoring loop. */
data class PowerReading(
    val timestamp: Long,
    val plugged: Boolean,
    val levelPercent: Int,
    val chargeCounterMah: Double?,
    /** Magnitude in mA. */
    val currentMa: Double?,
    val powerW: Double?,
    val temperatureC: Float,
)

data class ChargeSession(
    val startTimestamp: Long,
    val lastTimestamp: Long,
    val startLevel: Int,
    val endLevel: Int,
    val startCounterMah: Double?,
    val endCounterMah: Double?,
    /** Charge integrated from the current, used when the charge counter is missing. */
    val integratedMah: Double,
    val maxPowerW: Double,
    val averagePowerW: Double?,
    val maxTemperatureC: Float,
) {
    val durationMs: Long get() = lastTimestamp - startTimestamp
    val levelGained: Int get() = endLevel - startLevel

    /** Charge put into the battery, from the fuel gauge when available. */
    val addedMah: Double?
        get() {
            if (startCounterMah != null && endCounterMah != null) return (endCounterMah - startCounterMah).takeIf { it > 0 }
            return integratedMah.takeIf { it > 0 }
        }

    /** Full capacity implied by this session; only meaningful over a large enough level gain. */
    val estimatedCapacityMah: Double?
        get() = addedMah?.takeIf { levelGained >= MIN_LEVEL_FOR_ESTIMATE }?.let { it * 100 / levelGained }

    companion object {
        const val MIN_LEVEL_FOR_ESTIMATE = 15
    }
}

sealed interface ChargeEvent {
    data class Started(val session: ChargeSession) : ChargeEvent
    data class Updated(val session: ChargeSession) : ChargeEvent
    data class Finished(val session: ChargeSession) : ChargeEvent

    /** Unplugged too quickly to be a real charge (cable wiggle). */
    data class Discarded(val session: ChargeSession) : ChargeEvent
}

/** Turns a stream of [PowerReading]s into charge sessions (plug → unplug). */
class ChargeSessionTracker(private val minDurationMs: Long = 60_000) {

    var current: ChargeSession? = null
        private set

    private var energyWattMs = 0.0
    private var poweredMs = 0L

    fun update(reading: PowerReading): ChargeEvent? {
        val session = current
        return when {
            reading.plugged && session == null -> {
                energyWattMs = 0.0
                poweredMs = 0
                val started = ChargeSession(
                    startTimestamp = reading.timestamp,
                    lastTimestamp = reading.timestamp,
                    startLevel = reading.levelPercent,
                    endLevel = reading.levelPercent,
                    startCounterMah = reading.chargeCounterMah,
                    endCounterMah = reading.chargeCounterMah,
                    integratedMah = 0.0,
                    maxPowerW = reading.powerW ?: 0.0,
                    averagePowerW = null,
                    maxTemperatureC = reading.temperatureC,
                )
                current = started
                ChargeEvent.Started(started)
            }
            reading.plugged && session != null -> {
                val updated = integrate(session, reading)
                current = updated
                ChargeEvent.Updated(updated)
            }
            !reading.plugged && session != null -> {
                current = null
                val finished = session.copy(lastTimestamp = reading.timestamp)
                if (finished.durationMs < minDurationMs) ChargeEvent.Discarded(finished) else ChargeEvent.Finished(finished)
            }
            else -> null
        }
    }

    private fun integrate(session: ChargeSession, reading: PowerReading): ChargeSession {
        // Long gaps (service stopped, device asleep) are not integrated: the rate is unknown there.
        val dt = (reading.timestamp - session.lastTimestamp).coerceIn(0, MAX_INTEGRATION_STEP_MS)
        val addedMah = (reading.currentMa ?: 0.0) * dt / MS_PER_HOUR
        reading.powerW?.let {
            energyWattMs += it * dt
            poweredMs += dt
        }
        return session.copy(
            lastTimestamp = reading.timestamp,
            endLevel = reading.levelPercent,
            endCounterMah = reading.chargeCounterMah ?: session.endCounterMah,
            integratedMah = session.integratedMah + addedMah,
            maxPowerW = maxOf(session.maxPowerW, reading.powerW ?: 0.0),
            averagePowerW = if (poweredMs > 0) energyWattMs / poweredMs else null,
            maxTemperatureC = maxOf(session.maxTemperatureC, reading.temperatureC),
        )
    }

    private companion object {
        const val MAX_INTEGRATION_STEP_MS = 60_000L
        const val MS_PER_HOUR = 3_600_000.0
    }
}
