package fr.tflag.droidboost.monitor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.net.toUri

/**
 * Helps Shizuku survive reboots without root: with WRITE_SECURE_SETTINGS (granted once by `pm grant`)
 * the app can switch wireless debugging back on, which Shizuku 13.6+ needs to auto-start on a trusted Wi-Fi.
 */
class ShizukuWatchdog(private val context: Context, private val alerts: AlertNotifier) {

    val canControlWirelessDebugging: Boolean
        get() = context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    fun enableWirelessDebugging(): Boolean = canControlWirelessDebugging &&
        runCatching { Settings.Global.putInt(context.contentResolver, ADB_WIFI_ENABLED, 1) }.getOrDefault(false)

    fun onShizukuStopped(keepWirelessDebugging: Boolean) {
        val wifiDebugging = keepWirelessDebugging && enableWirelessDebugging()
        alerts.notify(
            AlertKind.SHIZUKU,
            "Shizuku s'est arrêté",
            if (wifiDebugging) "Débogage sans fil réactivé : ouvrez Shizuku et touchez « Démarrer »."
            else "Les actions avancées sont indisponibles. Relancez Shizuku par le débogage sans fil.",
            action = "Ouvrir Shizuku" to shizukuIntent(),
        )
    }

    private fun shizukuIntent(): Intent =
        context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            ?: Intent(Intent.ACTION_VIEW, "https://github.com/RikkaApps/Shizuku/releases".toUri())

    private companion object {
        const val WRITE_SECURE_SETTINGS = "android.permission.WRITE_SECURE_SETTINGS"
        const val ADB_WIFI_ENABLED = "adb_wifi_enabled"
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }
}
