package fr.tflag.droidboost.core.monitor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ChargeSessionTrackerTest {

    private fun reading(t: Long, plugged: Boolean, level: Int, counter: Double? = null, current: Double? = 2000.0, power: Double? = 8.0, temp: Float = 30f) =
        PowerReading(t, plugged, level, counter, current, power, temp)

    @Test
    fun `tracks a session from plug to unplug`() {
        val tracker = ChargeSessionTracker()
        assertNull(tracker.update(reading(0, plugged = false, level = 20)))
        assertIs<ChargeEvent.Started>(tracker.update(reading(1_000, true, 20, counter = 1000.0)))
        assertIs<ChargeEvent.Updated>(tracker.update(reading(31_000, true, 21, counter = 1050.0, power = 10.0, temp = 34f)))
        tracker.update(reading(61_000, true, 60, counter = 3000.0, power = 6.0))
        val finished = assertIs<ChargeEvent.Finished>(tracker.update(reading(3_600_000, false, 60))).session
        assertEquals(20, finished.startLevel)
        assertEquals(60, finished.endLevel)
        assertEquals(2000.0, finished.addedMah)
        assertEquals(5000.0, finished.estimatedCapacityMah)
        assertEquals(10.0, finished.maxPowerW)
        assertEquals(8.0, finished.averagePowerW)
        assertEquals(34f, finished.maxTemperatureC)
        assertNull(tracker.current)
    }

    @Test
    fun `integrates current when the counter is missing`() {
        val tracker = ChargeSessionTracker()
        tracker.update(reading(0, true, 50))
        // 2000 mA during 2 × 60 s (steps are capped at 60 s, so the 10 min gap counts for 60 s only).
        tracker.update(reading(60_000, true, 51))
        tracker.update(reading(660_000, true, 55))
        val session = tracker.current!!
        assertEquals(2000.0 * 120_000 / 3_600_000, session.integratedMah, 1e-9)
        assertEquals(session.integratedMah, session.addedMah)
        assertNull(session.estimatedCapacityMah)
    }

    @Test
    fun `discards a cable wiggle`() {
        val tracker = ChargeSessionTracker(minDurationMs = 60_000)
        tracker.update(reading(0, true, 50))
        assertIs<ChargeEvent.Discarded>(tracker.update(reading(5_000, false, 50)))
    }
}
