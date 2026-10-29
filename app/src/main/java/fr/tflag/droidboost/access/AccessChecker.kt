package fr.tflag.droidboost.access

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.os.Process
import android.provider.Settings
import fr.tflag.droidboost.core.shell.ShellCommands

/**
 * A permission marked `development` in AOSP: granted once with `pm grant`, it survives reboots.
 * [restartsApp]: the permission maps to Linux groups (`platform.xml`), so Android kills the app to apply it.
 */
enum class DevGrant(val permission: String, val title: String, val unlocks: String, val restartsApp: Boolean = false) {
    PACKAGE_USAGE_STATS(
        "android.permission.PACKAGE_USAGE_STATS",
        "Données d'usage",
        "Tailles par appli, temps d'écran, consommation de données",
    ),
    BATTERY_STATS(
        "android.permission.BATTERY_STATS",
        "Statistiques batterie",
        "Santé de la batterie, dates de fabrication et de première utilisation",
    ),
    DUMP(
        "android.permission.DUMP",
        "Diagnostics système",
        "Températures des capteurs sans Shizuku, plantages et ANR de toutes les applis, conso par appli",
    ),
    WRITE_SECURE_SETTINGS(
        "android.permission.WRITE_SECURE_SETTINGS",
        "Réglages sécurisés",
        "Réactivation du débogage sans fil pour relancer Shizuku",
    ),
    READ_LOGS(
        "android.permission.READ_LOGS",
        "Journaux système",
        "Lecture du logcat (au premier plan, après consentement)",
        restartsApp = true,
    ),
    ACCESS_FINE_POWER_MONITORS(
        "android.permission.ACCESS_FINE_POWER_MONITORS",
        "Mesure fine de l'énergie",
        "Consommation par composant (CPU, GPU, modem, écran) rafraîchie en continu",
    ),
    ;

    fun adbCommand(packageName: String) = "adb shell ${ShellCommands.grant(packageName, permission)}"
}

/** Current state of every access the app can use, checked without any privilege. */
class AccessChecker(private val context: Context) {

    private val appOps = context.getSystemService(AppOpsManager::class.java)

    fun hasUsageStats(): Boolean {
        val mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED ||
            (mode == AppOpsManager.MODE_DEFAULT && isGranted(DevGrant.PACKAGE_USAGE_STATS.permission))
    }

    fun hasAllFilesAccess(): Boolean = Environment.isExternalStorageManager()

    fun hasNotifications(): Boolean = isGranted(Manifest.permission.POST_NOTIFICATIONS)

    fun hasOverlay(): Boolean = Settings.canDrawOverlays(context)

    fun isGranted(grant: DevGrant): Boolean =
        if (grant == DevGrant.PACKAGE_USAGE_STATS) hasUsageStats() else isGranted(grant.permission)

    private fun isGranted(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    /** Every ADB command needed to grant all [DevGrant]s at once, ready to paste. */
    fun adbScript(): String = buildList {
        DevGrant.entries.forEach { add(it.adbCommand(context.packageName)) }
        add("adb shell ${ShellCommands.allowUsageStats(context.packageName)}")
    }.joinToString("\n")
}
