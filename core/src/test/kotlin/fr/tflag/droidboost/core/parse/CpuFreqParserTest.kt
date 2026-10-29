package fr.tflag.droidboost.core.parse

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class CpuFreqParserTest {

    @Test
    fun `parses shell output with offline and unreadable cores`() {
        val cores = CpuFreqParser.parse(
            """
            cpu0 1 1197000 324000 1704000
            cpu10 1 - - -
            cpu8 0 - 500000 2910000
            garbage line
            """.trimIndent(),
        )
        assertEquals(listOf(0, 8, 10), cores.map { it.index })
        assertEquals(1_197_000L, cores[0].currentKhz)
        assertFalse(cores[1].online)
        assertNull(cores[1].currentKhz)
        assertEquals(2_910_000L, cores[1].maxKhz)
    }

    @Test
    fun `parses possible cpu ranges`() {
        assertEquals((0..8).toList(), CpuFreqParser.parseRange("0-8\n"))
        assertEquals(listOf(0, 1, 2, 3, 6), CpuFreqParser.parseRange("0-3,6"))
        assertEquals(emptyList(), CpuFreqParser.parseRange(""))
    }

    @Test
    fun `command is a single shell line`() {
        assertFalse(CpuFreqParser.COMMAND.contains('\n'))
    }
}
