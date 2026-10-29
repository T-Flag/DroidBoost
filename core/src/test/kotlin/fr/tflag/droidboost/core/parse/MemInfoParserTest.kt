package fr.tflag.droidboost.core.parse

import kotlin.test.Test
import kotlin.test.assertEquals

class MemInfoParserTest {

    @Test
    fun `parses kilobyte values`() {
        val info = MemInfoParser.parse(
            """
            MemTotal:       11714460 kB
            MemFree:          402148 kB
            MemAvailable:    5123456 kB
            Buffers:            1024 kB
            Cached:          4096000 kB
            SwapCached:        12000 kB
            Active(anon):    1234567 kB
            SwapTotal:       8388604 kB
            SwapFree:        6291452 kB
            """.trimIndent(),
        )
        assertEquals(11714460L * 1024, info.totalBytes)
        assertEquals(5123456L * 1024, info.availableBytes)
        assertEquals((8388604L - 6291452L) * 1024, info.swapUsedBytes)
    }
}
