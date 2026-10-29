package fr.tflag.droidboost.core.privacy

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PrivacyTest {

    @Test
    fun `groups granted permissions`() {
        val grouped = PermissionGroup.grouped(
            setOf("android.permission.CAMERA", BACKGROUND_LOCATION, "android.permission.INTERNET"),
        )
        assertEquals(setOf(PermissionGroup.CAMERA, PermissionGroup.LOCATION), grouped.keys)
        assertEquals(setOf(BACKGROUND_LOCATION), grouped[PermissionGroup.LOCATION])
        assertEquals(PermissionGroup.BODY, PermissionGroup.groupOf("android.permission.health.READ_HEART_RATE"))
        assertEquals(null, PermissionGroup.groupOf("android.permission.INTERNET"))
    }

    @Test
    fun `patch age levels`() {
        val today = LocalDate.of(2026, 10, 29)
        assertEquals(CheckLevel.OK, SecurityChecks.patchAge("2026-10-05", today).level)
        assertEquals(CheckLevel.WARNING, SecurityChecks.patchAge("2026-07-05", today).level)
        assertEquals(CheckLevel.INFO, SecurityChecks.patchAge("inconnu", today).level)
    }

    @Test
    fun `verified boot states`() {
        assertEquals(CheckLevel.OK, SecurityChecks.verifiedBoot("green\n").level)
        assertEquals(CheckLevel.WARNING, SecurityChecks.verifiedBoot("orange").level)
        assertEquals(CheckLevel.INFO, SecurityChecks.verifiedBoot(null).level)
    }

    @Test
    fun `parses package and component lists`() {
        assertEquals(
            listOf("com.android.vending", "org.mozilla.firefox"),
            SecurityChecks.parsePackageList("com.android.vending\norg.mozilla.firefox\n\nNo operations.\ncom.android.vending"),
        )
        assertEquals(
            listOf("com.google.android.marvin.talkback", "com.example"),
            SecurityChecks.parseComponentPackages("com.google.android.marvin.talkback/.TalkBackService:com.example/com.example.A11y"),
        )
        assertEquals(emptyList(), SecurityChecks.parseComponentPackages(null))
    }
}
