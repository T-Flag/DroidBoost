package fr.tflag.droidboost.apps

import fr.tflag.droidboost.core.shell.CommandOutcome
import fr.tflag.droidboost.core.shell.ShellCommands
import fr.tflag.droidboost.core.shell.StandbyBucket
import fr.tflag.droidboost.shell.ShizukuShell

/** Privileged actions on other apps, all through the Shizuku shell. */
class AppActions(private val shell: ShizukuShell) {

    suspend fun forceStop(pkg: String) = shell.run(ShellCommands.forceStop(pkg))

    suspend fun setEnabled(pkg: String, enabled: Boolean) =
        shell.run(if (enabled) ShellCommands.enable(pkg) else ShellCommands.disable(pkg))

    suspend fun setSuspended(pkg: String, suspended: Boolean) =
        shell.run(if (suspended) ShellCommands.suspend(pkg) else ShellCommands.unsuspend(pkg))

    /**
     * Removes the app for user 0, keeping its APK on the system image so it can be restored.
     * Android 17 may answer [CommandOutcome.RootRequired] for system apps: callers then offer to disable instead.
     */
    suspend fun uninstallForUser(pkg: String) = shell.run(ShellCommands.uninstallForUser(pkg))

    suspend fun reinstall(pkg: String) = shell.run(ShellCommands.installExisting(pkg))

    /** For an archived app: `install-existing` cannot help since its APK is gone. */
    suspend fun unarchive(pkg: String) = shell.run(ShellCommands.requestUnarchive(pkg))

    suspend fun clearData(pkg: String) = shell.run(ShellCommands.clearData(pkg))

    suspend fun setPermission(pkg: String, permission: String, granted: Boolean) =
        shell.run(if (granted) ShellCommands.grant(pkg, permission) else ShellCommands.revoke(pkg, permission))

    suspend fun setStandbyBucket(pkg: String, bucket: StandbyBucket) = shell.run(ShellCommands.setStandbyBucket(pkg, bucket))

    suspend fun standbyBucket(pkg: String): StandbyBucket? =
        shell.output(ShellCommands.getStandbyBucket(pkg))?.trim()?.toIntOrNull()?.let(StandbyBucket::fromCode)

    suspend fun setBackgroundRestricted(pkg: String, restricted: Boolean) =
        shell.run(ShellCommands.setBackgroundRestricted(pkg, restricted))

    /** Null when unknown (no Shizuku); output looks like `RUN_ANY_IN_BACKGROUND: ignore`. */
    suspend fun isBackgroundRestricted(pkg: String): Boolean? =
        shell.output(ShellCommands.getBackgroundRestriction(pkg))?.let { "ignore" in it || "deny" in it }

    suspend fun trimAllCaches(): CommandOutcome = shell.run(ShellCommands.trimAllCaches(), timeoutMs = TRIM_TIMEOUT_MS)

    private companion object {
        const val TRIM_TIMEOUT_MS = 120_000L
    }
}
