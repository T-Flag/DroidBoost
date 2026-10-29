package fr.tflag.droidboost.core.parse

import fr.tflag.droidboost.core.model.CpuTimes
import kotlin.test.Test
import kotlin.test.assertEquals

class ProcStatParserTest {

    private val first = """
        cpu  100 0 100 700 100 0 0 0 0 0
        cpu0 50 0 50 300 0 0 0 0 0 0
        cpu1 50 0 50 400 100 0 0 0 0 0
        intr 12345
        ctxt 678
    """.trimIndent()

    private val second = """
        cpu  200 0 200 1300 100 0 0 0 0 0
        cpu0 100 0 100 400 0 0 0 0 0 0
        cpu1 100 0 100 900 100 0 0 0 0 0
    """.trimIndent()

    @Test
    fun `parses aggregate and per core lines`() {
        val parsed = ProcStatParser.parse(first)
        assertEquals(setOf(CpuTimes.TOTAL, 0, 1), parsed.keys)
        assertEquals(CpuTimes(idle = 800, total = 1000), parsed[CpuTimes.TOTAL])
    }

    @Test
    fun `computes busy fraction between snapshots`() {
        val load = ProcStatParser.load(ProcStatParser.parse(first), ProcStatParser.parse(second))
        assertEquals(0.25f, load[CpuTimes.TOTAL])
        assertEquals(0.5f, load[0])
        assertEquals(1f / 6, load.getValue(1), 1e-4f)
    }

    @Test
    fun `skips cores missing from one snapshot`() {
        val withoutCore1 = second.lines().filterNot { it.startsWith("cpu1") }.joinToString("\n")
        val load = ProcStatParser.load(ProcStatParser.parse(first), ProcStatParser.parse(withoutCore1))
        assertEquals(setOf(CpuTimes.TOTAL, 0), load.keys)
    }
}
