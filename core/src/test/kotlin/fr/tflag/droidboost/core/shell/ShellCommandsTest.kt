package fr.tflag.droidboost.core.shell

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShellCommandsTest {

    @Test
    fun `builds user-0 commands`() {
        assertEquals("pm disable-user --user 0 com.google.android.apps.tips", ShellCommands.disable("com.google.android.apps.tips"))
        assertEquals("pm uninstall -k --user 0 com.example.app", ShellCommands.uninstallForUser("com.example.app"))
        assertEquals("pm uninstall --user 0 com.example.app", ShellCommands.uninstallForUser("com.example.app", keepData = false))
        assertEquals("cmd package install-existing --user 0 com.example.app", ShellCommands.installExisting("com.example.app"))
        assertEquals(
            "am set-standby-bucket com.example.app restricted",
            ShellCommands.setStandbyBucket("com.example.app", StandbyBucket.RESTRICTED),
        )
        assertEquals(
            "pm grant fr.tflag.droidboost android.permission.DUMP",
            ShellCommands.grant("fr.tflag.droidboost", "android.permission.DUMP"),
        )
    }

    @Test
    fun `rejects identifiers that could inject shell syntax`() {
        assertFailsWith<IllegalArgumentException> { ShellCommands.forceStop("com.example; reboot") }
        assertFailsWith<IllegalArgumentException> { ShellCommands.disable("com.example.app && rm -rf /") }
        assertFailsWith<IllegalArgumentException> { ShellCommands.clearData("\$(id)") }
        assertFailsWith<IllegalArgumentException> { ShellCommands.grant("com.example.app", "android.permission.X;id") }
        assertFailsWith<IllegalArgumentException> { ShellCommands.disable("nodots") }
    }

    @Test
    fun `app folder commands stay inside their area`() {
        assertEquals("rm -rf /sdcard/Android/data/com.old.app", ShellCommands.deleteAppFolder(AppFolderArea.DATA, "com.old.app"))
        assertEquals("du -sk /sdcard/Android/obb/com.old.app", ShellCommands.folderSizeKb(AppFolderArea.OBB, "com.old.app"))
        assertFailsWith<IllegalArgumentException> { ShellCommands.deleteAppFolder(AppFolderArea.DATA, "..") }
        assertFailsWith<IllegalArgumentException> { ShellCommands.deleteAppFolder(AppFolderArea.DATA, "com.a/../../..") }
        assertFailsWith<IllegalArgumentException> { ShellCommands.deleteAppFolder(AppFolderArea.DATA, "") }
    }

    @Test
    fun `validates package names`() {
        assertTrue(Identifiers.isValidPackage("com.google.android.gms"))
        assertTrue(Identifiers.isValidPackage("org.lineageos.updater_2"))
        assertFalse(Identifiers.isValidPackage("1com.example"))
        assertFalse(Identifiers.isValidPackage("com..example"))
        assertFalse(Identifiers.isValidPackage("com.example "))
    }

    @Test
    fun `maps standby bucket codes`() {
        assertEquals(StandbyBucket.RARE, StandbyBucket.fromCode(40))
        assertEquals(null, StandbyBucket.fromCode(99))
    }
}
