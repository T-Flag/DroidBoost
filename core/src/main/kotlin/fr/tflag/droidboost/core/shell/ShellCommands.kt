package fr.tflag.droidboost.core.shell

/**
 * Builds the shell commands run through Shizuku (uid 2000).
 *
 * Every package or permission name is validated before being interpolated, so a malformed
 * identifier can never inject extra shell syntax.
 */
object ShellCommands {

    /** Bytes requested from `pm trim-caches`: far above any real storage, so every cache is eligible. */
    private const val TRIM_ALL = "1000G"

    fun forceStop(packageName: String) = "am force-stop ${pkg(packageName)}"

    fun disable(packageName: String) = "pm disable-user --user 0 ${pkg(packageName)}"

    fun enable(packageName: String) = "pm enable --user 0 ${pkg(packageName)}"

    fun suspend(packageName: String) = "pm suspend --user 0 ${pkg(packageName)}"

    fun unsuspend(packageName: String) = "pm unsuspend --user 0 ${pkg(packageName)}"

    /** Removes the app for user 0 only; with [keepData] the system can restore it later with [installExisting]. */
    fun uninstallForUser(packageName: String, keepData: Boolean = true) =
        "pm uninstall ${if (keepData) "-k " else ""}--user 0 ${pkg(packageName)}"

    fun installExisting(packageName: String) = "cmd package install-existing --user 0 ${pkg(packageName)}"

    fun clearData(packageName: String) = "pm clear --user 0 ${pkg(packageName)}"

    fun trimAllCaches() = "pm trim-caches $TRIM_ALL"

    fun grant(packageName: String, permission: String) = "pm grant ${pkg(packageName)} ${perm(permission)}"

    fun revoke(packageName: String, permission: String) = "pm revoke ${pkg(packageName)} ${perm(permission)}"

    fun getStandbyBucket(packageName: String) = "am get-standby-bucket ${pkg(packageName)}"

    fun setStandbyBucket(packageName: String, bucket: StandbyBucket) =
        "am set-standby-bucket ${pkg(packageName)} ${bucket.shellName}"

    fun setBackgroundRestricted(packageName: String, restricted: Boolean) =
        "appops set ${pkg(packageName)} RUN_ANY_IN_BACKGROUND ${if (restricted) "ignore" else "allow"}"

    fun getBackgroundRestriction(packageName: String) = "appops get ${pkg(packageName)} RUN_ANY_IN_BACKGROUND"

    fun allowUsageStats(packageName: String) = "appops set ${pkg(packageName)} GET_USAGE_STATS allow"

    /** Archives an app (Android 15+): APK and cache removed, data kept, icon restorable from the launcher. */
    fun archive(packageName: String) = "pm archive --user 0 ${pkg(packageName)}"

    /** Asks the installer that archived the app (usually the Play Store) to download it again. */
    fun requestUnarchive(packageName: String) = "pm request-unarchive --user 0 ${pkg(packageName)}"

    /** Lists the per-app folders of a shared-storage area only the shell can read (Android/data, obb, media). */
    fun listAppFolders(area: AppFolderArea) = "ls -1 ${area.path}"

    /** Size in KB of one app folder, e.g. leftovers of an uninstalled app. */
    fun folderSizeKb(area: AppFolderArea, packageName: String) = "du -sk ${area.path}/${pkg(packageName)}"

    /** Deletes the folder of an uninstalled app; the package name is validated so the path cannot escape the area. */
    fun deleteAppFolder(area: AppFolderArea, packageName: String) = "rm -rf ${area.path}/${pkg(packageName)}"

    const val THERMAL_DUMP = "dumpsys thermalservice"
    const val PROC_STAT = "cat /proc/stat"
    const val MEMINFO = "cat /proc/meminfo"

    private fun pkg(name: String): String {
        require(Identifiers.isValidPackage(name)) { "Nom de paquet invalide : $name" }
        return name
    }

    private fun perm(name: String): String {
        require(Identifiers.isValidPermission(name)) { "Nom de permission invalide : $name" }
        return name
    }
}

enum class AppFolderArea(val path: String, val label: String) {
    DATA("/sdcard/Android/data", "Android/data"),
    OBB("/sdcard/Android/obb", "Android/obb"),
    MEDIA("/sdcard/Android/media", "Android/media"),
}

object Identifiers {
    private val PACKAGE = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")
    private val PERMISSION = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)*$")

    fun isValidPackage(name: String) = name.length <= 255 && PACKAGE.matches(name)

    fun isValidPermission(name: String) = name.length <= 255 && PERMISSION.matches(name)
}

enum class StandbyBucket(val shellName: String, val code: Int) {
    ACTIVE("active", 10),
    WORKING_SET("working_set", 20),
    FREQUENT("frequent", 30),
    RARE("rare", 40),
    RESTRICTED("restricted", 45),
    ;

    companion object {
        /** Parses the numeric output of `am get-standby-bucket`. */
        fun fromCode(code: Int): StandbyBucket? = entries.firstOrNull { it.code == code }
    }
}
