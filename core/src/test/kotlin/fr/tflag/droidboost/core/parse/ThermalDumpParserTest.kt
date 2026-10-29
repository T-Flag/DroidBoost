package fr.tflag.droidboost.core.parse

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ThermalDumpParserTest {

    private val dump = """
        IsStatusOverride: false
        ThermalEventListeners:
        	callbacks: 1
        	killed: false
        Thermal Status: 1
        Cached temperatures:
        	Temperature{mValue=31.5, mType=3, mName=VIRTUAL-SKIN, mStatus=0}
        HAL Ready: true
        HAL connection:
        	ThermalHAL AIDL 1  connected: yes
        Current temperatures from HAL:
        	Temperature{mValue=30.37, mType=2, mName=battery, mStatus=0}
        	Temperature{mValue=48.2, mType=0, mName=BIG, mStatus=1}
        	Temperature{mValue=NaN, mType=1, mName=G3D, mStatus=0}
        	Temperature{mValue=38.9, mType=3, mName=VIRTUAL-SKIN, mStatus=1}
        Current cooling devices from HAL:
        	CoolingDevice{mValue=0, mType=2, mName=fan}
        Temperature static thresholds from HAL:
        	TemperatureThreshold{mType=3, mName=VIRTUAL-SKIN, mHotThrottlingThresholds=[NaN, 37.0, 39.0, 43.0, 45.0, 47.0, 55.0], mColdThrottlingThresholds=[NaN, NaN, NaN, NaN, NaN, NaN, NaN]}
    """.trimIndent()

    @Test
    fun `reads hal temperatures and skips NaN`() {
        val parsed = ThermalDumpParser.parse(dump)
        assertEquals(1, parsed.status)
        assertEquals(listOf("battery", "BIG", "VIRTUAL-SKIN"), parsed.temperatures.map { it.name })
        assertEquals(48.2f, parsed.temperatures[1].celsius)
        assertEquals(1, parsed.temperatures[1].status)
    }

    @Test
    fun `reads throttling thresholds`() {
        val parsed = ThermalDumpParser.parse(dump)
        val skin = parsed.temperatures.first { it.name == "VIRTUAL-SKIN" }
        assertEquals(37.0f, parsed.thresholdFor(skin)?.firstThrottle)
        assertNull(parsed.thresholdFor(parsed.temperatures.first { it.name == "battery" }))
    }

    @Test
    fun `falls back to cached temperatures`() {
        val parsed = ThermalDumpParser.parse(
            """
            Thermal Status: 0
            Cached temperatures:
            	Temperature{mValue=31.5, mType=3, mName=VIRTUAL-SKIN, mStatus=0}
            HAL Ready: false
            """.trimIndent(),
        )
        assertEquals(listOf(31.5f), parsed.temperatures.map { it.celsius })
    }

    @Test
    fun `empty dump`() {
        val parsed = ThermalDumpParser.parse("")
        assertNull(parsed.status)
        assertEquals(emptyList(), parsed.temperatures)
    }
}
