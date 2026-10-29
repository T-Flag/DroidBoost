package fr.tflag.droidboost.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ModelTest {

    @Test
    fun `groups tensor g3 cores into three clusters`() {
        val cores = (0..8).map { i ->
            val max = when (i) {
                in 0..3 -> 1_704_000L
                in 4..7 -> 2_367_000L
                else -> 2_910_000L
            }
            CpuCore(index = i, currentKhz = null, minKhz = null, maxKhz = max, online = true)
        }
        val clusters = CpuTopology.clusters(cores)
        assertEquals(listOf("Économie", "Équilibré", "Performance"), clusters.map { it.name })
        assertEquals(listOf(4, 4, 1), clusters.map { it.cores.size })
        assertEquals(2_910_000L, clusters.last().maxKhz)
    }

    @Test
    fun `normalizes current units`() {
        assertEquals(450.0, BatteryMath.currentMilliAmps(-450_000))
        assertEquals(450.0, BatteryMath.currentMilliAmps(450))
        assertEquals(3.85 * 1.5, BatteryMath.powerWatts(3850, 1500.0), 1e-9)
    }

    @Test
    fun `computes deep sleep share`() {
        assertEquals(0.75f, BatteryMath.deepSleepFraction(elapsedRealtimeMs = 4000, uptimeMs = 1000))
        assertEquals(0f, BatteryMath.deepSleepFraction(0, 0))
    }
}
