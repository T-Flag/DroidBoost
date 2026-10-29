package fr.tflag.droidboost.core.monitor

import fr.tflag.droidboost.core.model.ThermalStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MonitorLogicTest {

    @Test
    fun `thermal episode spans throttling statuses`() {
        val tracker = ThermalEpisodeTracker()
        assertNull(tracker.update(0, ThermalStatus.NONE, 0.3f, "app.a", charging = false))
        assertIs<ThermalEvent.Started>(tracker.update(10, ThermalStatus.LIGHT, 0.7f, null, charging = false))
        tracker.update(20, ThermalStatus.SEVERE, 1.05f, "com.game", charging = true)
        val finished = assertIs<ThermalEvent.Finished>(tracker.update(30, ThermalStatus.NONE, 0.5f, null, false)).episode
        assertEquals(20, finished.durationMs)
        assertEquals(ThermalStatus.SEVERE, finished.maxStatus)
        assertEquals(1.05f, finished.maxHeadroom)
        assertEquals("com.game", finished.foregroundPackage)
        assertTrue(finished.charging)
    }

    @Test
    fun `charge level alert fires once per session`() {
        val alert = ChargeLevelAlert(80)
        assertFalse(alert.check(plugged = true, levelPercent = 79))
        assertTrue(alert.check(plugged = true, levelPercent = 80))
        assertFalse(alert.check(plugged = true, levelPercent = 81))
        assertFalse(alert.check(plugged = false, levelPercent = 81))
        assertTrue(alert.check(plugged = true, levelPercent = 81))
    }

    @Test
    fun `threshold alert uses hysteresis`() {
        val alert = ThresholdAlert(threshold = 42f, hysteresis = 2f)
        assertTrue(alert.check(42.5f))
        assertFalse(alert.check(43f))
        assertFalse(alert.check(41f))
        assertFalse(alert.check(42.2f))
        assertFalse(alert.check(39.9f))
        assertTrue(alert.check(42f))
    }

    @Test
    fun `battery health helpers`() {
        assertEquals(5000.0, BatteryHealth.gaugeFullCapacityMah(2500.0, 50))
        assertNull(BatteryHealth.gaugeFullCapacityMah(200.0, 4))
        assertEquals(5050, BatteryHealth.designCapacityMah("husky"))
        assertEquals(4800.0, BatteryHealth.medianEstimate(listOf(4900.0, 4700.0, 4800.0)))
        assertEquals(4750.0, BatteryHealth.medianEstimate(listOf(4900.0, 4700.0, 4800.0, 4600.0)))
    }

    @Test
    fun `discharge rate ignores charging and gaps`() {
        val h = 3_600_000L
        val levels = listOf(0L to 80.0, h to 76.0, 2 * h to 72.0, 3 * h to 90.0, 10 * h to 70.0)
        val plugged = listOf(0L to 0.0, h to 0.0, 2 * h to 0.0, 3 * h to 1.0, 10 * h to 0.0)
        assertEquals(4.0, BatteryHealth.dischargeRatePerHour(levels, plugged))
    }
}
