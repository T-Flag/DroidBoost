package fr.tflag.droidboost.core.format

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatsTest {

    @Test
    fun `formats bytes in french units`() {
        assertEquals("512 o", Formats.bytes(512))
        assertEquals("1,5 Ko", Formats.bytes(1536))
        assertEquals("11,2 Go", Formats.bytes(12_000_000_000))
        assertEquals("238 Go", Formats.bytes(256_000_000_000))
    }

    @Test
    fun `formats frequencies`() {
        assertEquals("2,91 GHz", Formats.frequencyKhz(2_910_000))
        assertEquals("820 MHz", Formats.frequencyKhz(820_000))
    }

    @Test
    fun `formats durations`() {
        assertEquals("45 s", Formats.duration(45_000))
        assertEquals("12 min", Formats.duration(12 * 60_000L))
        assertEquals("3 h 05", Formats.duration((3 * 60 + 5) * 60_000L))
        assertEquals("2 j 4 h", Formats.duration((52 * 60) * 60_000L))
    }

    @Test
    fun `formats relative times`() {
        assertEquals("à l'instant", Formats.ago(20_000))
        assertEquals("il y a 5 min", Formats.ago(5 * 60_000L))
        assertEquals("il y a 3 h", Formats.ago(3 * 3_600_000L))
        assertEquals("il y a 4 j", Formats.ago(4 * 86_400_000L))
        assertEquals("il y a 2 mois", Formats.ago(65 * 86_400_000L))
    }
}
