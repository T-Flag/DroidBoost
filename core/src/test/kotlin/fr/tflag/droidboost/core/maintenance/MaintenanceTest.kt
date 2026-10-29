package fr.tflag.droidboost.core.maintenance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MaintenanceTest {

    @Test
    fun `catalog protects critical packages`() {
        assertTrue(DebloatCatalog.isProtected("com.google.android.apps.wellbeing"))
        assertTrue(DebloatCatalog.isProtected("com.google.android.overlay.gmsconfig.photos"))
        assertFalse(DebloatCatalog.isProtected("com.google.android.apps.tips"))
        assertEquals(DebloatRisk.SAFE, DebloatCatalog.find("com.google.android.apps.tips")?.risk)
        // Every package appears once.
        assertEquals(DebloatCatalog.entries.size, DebloatCatalog.entries.map { it.packageName }.toSet().size)
    }

    @Test
    fun `finds duplicates by size then hash`() {
        val mb = 1024L * 1024
        val files = listOf(
            FileEntry("/a/photo.jpg", 3 * mb, 1),
            FileEntry("/b/photo (1).jpg", 3 * mb, 2),
            FileEntry("/c/other.jpg", 3 * mb, 3),
            FileEntry("/d/video.mp4", 10 * mb, 4),
            FileEntry("/e/tiny.txt", 10, 5),
            FileEntry("/f/tiny copy.txt", 10, 6),
        )
        val hashes = mapOf("/a/photo.jpg" to "h1", "/b/photo (1).jpg" to "h1", "/c/other.jpg" to "h2")
        val hashed = mutableListOf<String>()
        val groups = DuplicateFinder.find(files) { hashed += it.path; hashes[it.path] }
        assertEquals(1, groups.size)
        assertEquals(listOf("/a/photo.jpg", "/b/photo (1).jpg"), groups[0].files.map { it.path })
        assertEquals(3 * mb, groups[0].wastedBytes)
        // The unique 10 MB video and the tiny files are never hashed.
        assertEquals(setOf("/a/photo.jpg", "/b/photo (1).jpg", "/c/other.jpg"), hashed.toSet())
    }

    @Test
    fun `parses estimated power use`() {
        val dump = """
            Statistics since last charge:
              System starts: 0, currently on battery: true
              Estimated power use (mAh):
                Capacity: 5050, Computed drain: 812, actual drain: 790-830
                Global
                  screen: 210 apps: 150 duration: 5h 2m
                UID u0a221: 123 fg: 100 bg: 23 ( cpu=80.1 wifi=12.0 )
                UID 1000: 45.2 ( cpu=40.0 )
                UID u0a17: 0.51 ( cpu=0.51 )
                UID u0a221: 7 ( wakelock=7 )

              All kernel wake locks:
                Uid u0a99: 999 ( not part of the section )
        """.trimIndent()
        val power = BatteryStatsParser.parse(dump)
        assertEquals(listOf(10221, 1000, 10017), power.map { it.uid })
        assertEquals(130.0, power[0].mah)
        assertEquals(45.2, power[1].mah)
    }

    @Test
    fun `converts uid labels`() {
        assertEquals(10123, BatteryStatsParser.toUid("u0a123"))
        assertEquals(1010123, BatteryStatsParser.toUid("u10a123"))
        assertEquals(1000, BatteryStatsParser.toUid("1000"))
        assertEquals(null, BatteryStatsParser.toUid("abc"))
    }
}
