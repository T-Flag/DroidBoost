package fr.tflag.droidboost.core.journal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JournalTest {

    private fun state(
        pkg: String,
        version: Long = 1,
        enabled: Boolean = true,
        installed: Boolean = true,
        perms: Set<String> = emptySet(),
    ) = PackageState(pkg, pkg.substringAfterLast('.'), "v$version", version, version * 1000, enabled, installed, perms)

    @Test
    fun `detects every kind of change`() {
        val before = listOf(
            state("com.a.updated", version = 1),
            state("com.b.removed"),
            state("com.c.perms", perms = setOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO")),
            state("com.d.toggled"),
            state("com.e.debloated"),
        )
        val after = listOf(
            state("com.a.updated", version = 2),
            state("com.c.perms", perms = setOf("android.permission.CAMERA", "android.permission.ACCESS_FINE_LOCATION")),
            state("com.d.toggled", enabled = false),
            state("com.e.debloated", installed = false),
            state("com.f.new"),
        )
        val changes = PackageDiff.diff(before, after)
        assertEquals(
            listOf(
                PackageChange.Updated("com.a.updated", "updated", "v1", "v2"),
                PackageChange.InstalledForUserChanged("com.e.debloated", "debloated", false),
                PackageChange.Installed("com.f.new", "new", "v1"),
                PackageChange.PermissionGranted("com.c.perms", "perms", "android.permission.ACCESS_FINE_LOCATION"),
                PackageChange.PermissionRevoked("com.c.perms", "perms", "android.permission.RECORD_AUDIO"),
                PackageChange.EnabledChanged("com.d.toggled", "toggled", false),
                PackageChange.Removed("com.b.removed", "removed"),
            ).sortedBy { it.toString() },
            changes.sortedBy { it.toString() },
        )
    }

    @Test
    fun `no change between identical snapshots`() {
        val snapshot = listOf(state("com.a"), state("com.b", perms = setOf("x.Y")))
        assertEquals(emptyList(), PackageDiff.diff(snapshot, snapshot))
    }

    @Test
    fun `classifies exit reasons`() {
        assertEquals(ExitKind.LOW_MEMORY, ExitKind.classify(3, null))
        assertEquals(ExitKind.ANR, ExitKind.classify(6, "Input dispatching timed out"))
        assertEquals(ExitKind.MEMORY_LIMIT, ExitKind.classify(13, "MemoryLimiter:AnonSwap"))
        assertNull(ExitKind.classify(13, "something else"))
        assertNull(ExitKind.classify(10, null))
    }

    @Test
    fun `flags regressions in the bad direction only`() {
        val drain = ComparedMetric("Décharge", before = 4.0, after = 5.0, higherIsWorse = true)
        assertEquals(0.25, drain.relativeChange)
        assertTrue(drain.isRegression)
        assertFalse(ComparedMetric("Décharge", 5.0, 4.0, higherIsWorse = true).isRegression)
        assertFalse(ComparedMetric("Décharge", 4.0, 4.2, higherIsWorse = true).isRegression)
        assertFalse(ComparedMetric("Décharge", null, 4.0, higherIsWorse = true).isRegression)
        assertNull(ComparedMetric("Crashs", 0.0, 3.0, higherIsWorse = true).relativeChange)
    }
}
