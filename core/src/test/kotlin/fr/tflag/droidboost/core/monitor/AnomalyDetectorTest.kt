package fr.tflag.droidboost.core.monitor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AnomalyDetectorTest {

    private val usual = List(24) { 2.0 }

    @Test
    fun `drain alert needs a baseline and a clear excess`() {
        assertNotNull(AnomalyDetector.drain(7.5, usual))
        assertNull(AnomalyDetector.drain(4.5, usual), "below baseline + 3 %/h")
        assertNull(AnomalyDetector.drain(7.5, List(5) { 2.0 }), "baseline too short")
        assertNull(AnomalyDetector.drain(null, usual))
        // A heavy user (6 %/h) only triggers at twice that.
        assertNull(AnomalyDetector.drain(10.0, List(24) { 6.0 }))
        assertNotNull(AnomalyDetector.drain(12.5, List(24) { 6.0 }))
    }

    @Test
    fun `storage drop`() {
        val gib = 1024.0 * 1024 * 1024
        assertNotNull(AnomalyDetector.storageDrop(50 * gib, 47 * gib))
        assertNull(AnomalyDetector.storageDrop(50 * gib, 49 * gib))
        assertNull(AnomalyDetector.storageDrop(null, 1.0))
    }

    @Test
    fun `crash bursts and growth`() {
        val bursts = AnomalyDetector.crashBurst(mapOf("com.a" to 4, "com.b" to 1)) { it.uppercase() }
        assertEquals(listOf("crash:com.a"), bursts.map { it.key })
        assertEquals("COM.A plante en boucle", bursts.single().title)
        val gib = 1024.0 * 1024 * 1024
        assertEquals(listOf("growth:com.c"), AnomalyDetector.appGrowth(mapOf("com.c" to 1.5 * gib, "com.d" to 0.2 * gib)) { it }.map { it.key })
    }

    @Test
    fun `hourly rates skip plugged hours`() {
        val h = 3_600_000L
        val levels = listOf(
            0L to 90.0, h / 2 to 89.0, h - 1 to 88.0,
            h to 88.0, h + h / 2 to 95.0,
            2 * h to 80.0, 2 * h + h - 1 to 76.0,
        )
        val plugged = listOf(h to 1.0)
        val rates = AnomalyDetector.hourlyRates(levels, plugged)
        assertEquals(2, rates.size)
        assertEquals(2.0, rates[0], 0.01)
        assertEquals(4.0, rates[1], 0.01)
    }
}
