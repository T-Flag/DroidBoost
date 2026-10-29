package fr.tflag.droidboost.privacy

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import fr.tflag.droidboost.core.privacy.BACKGROUND_LOCATION
import fr.tflag.droidboost.core.privacy.CheckLevel
import fr.tflag.droidboost.core.privacy.PermissionGroup
import fr.tflag.droidboost.core.privacy.SecurityCheck
import fr.tflag.droidboost.core.privacy.SecurityChecks
import fr.tflag.droidboost.shell.ShizukuShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class AppPermissionUse(val packageName: String, val label: String, val permissions: Set<String>, val isSystem: Boolean)

data class SpecialAccess(val title: String, val description: String, val apps: List<String>?)

data class PrivacyReport(
    val groups: Map<PermissionGroup, List<AppPermissionUse>>,
    val backgroundLocation: List<AppPermissionUse>,
    val specialAccesses: List<SpecialAccess>,
)

/** Read-only audit of permissions, special accesses and the device's security posture. */
class PrivacyInspector(private val context: Context, private val shell: ShizukuShell) {

    private val pm: PackageManager = context.packageManager

    suspend fun privacy(includeSystem: Boolean): PrivacyReport = withContext(Dispatchers.IO) {
        val uses = installedWithPermissions().mapNotNull { info ->
            val app = info.applicationInfo ?: return@mapNotNull null
            if (app.flags and ApplicationInfo.FLAG_INSTALLED == 0) return@mapNotNull null
            val isSystem = app.flags and ApplicationInfo.FLAG_SYSTEM != 0 && app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0
            if (isSystem && !includeSystem) return@mapNotNull null
            val names = info.requestedPermissions.orEmpty()
            val flags = info.requestedPermissionsFlags ?: IntArray(names.size)
            val granted = names.indices.filter { flags.getOrElse(it) { 0 } and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 }.map { names[it] }.toSet()
            AppPermissionUse(info.packageName, label(app), granted, isSystem)
        }
        val groups = PermissionGroup.entries.associateWith { group ->
            uses.filter { use -> use.permissions.any { PermissionGroup.groupOf(it) == group } }.sortedBy { it.label.lowercase() }
        }
        PrivacyReport(
            groups = groups,
            backgroundLocation = uses.filter { BACKGROUND_LOCATION in it.permissions }.sortedBy { it.label.lowercase() },
            specialAccesses = specialAccesses(),
        )
    }

    suspend fun security(): List<SecurityCheck> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        buildList {
            add(SecurityChecks.patchAge(Build.VERSION.SECURITY_PATCH, LocalDate.now()))
            moduleVersion()?.let { add(SecurityCheck("Mise à jour système Google Play", it, CheckLevel.INFO)) }
            val secure = context.getSystemService(KeyguardManager::class.java).isDeviceSecure
            add(
                SecurityCheck(
                    "Verrouillage de l'écran",
                    if (secure) "Activé" else "Aucun",
                    if (secure) CheckLevel.OK else CheckLevel.WARNING,
                    if (secure) null else "Sans verrouillage, les données ne sont pas protégées en cas de vol.",
                ),
            )
            add(SecurityChecks.verifiedBoot(shell.output("getprop ro.boot.verifiedbootstate")))
            val devOptions = Settings.Global.getInt(resolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
            add(SecurityCheck("Options pour les développeurs", if (devOptions) "Activées" else "Désactivées", CheckLevel.INFO))
            val adb = Settings.Global.getInt(resolver, Settings.Global.ADB_ENABLED, 0) == 1
            add(
                SecurityCheck(
                    "Débogage USB",
                    if (adb) "Activé" else "Désactivé",
                    CheckLevel.INFO,
                    if (adb) "Nécessaire pour Shizuku ; n'acceptez jamais d'empreinte d'ordinateur inconnue." else null,
                ),
            )
            runCatching { Settings.Global.getInt(resolver, ADB_WIFI_ENABLED, 0) == 1 }.getOrNull()?.let { wifi ->
                add(SecurityCheck("Débogage sans fil", if (wifi) "Activé" else "Désactivé", CheckLevel.INFO))
            }
            val connectivity = context.getSystemService(ConnectivityManager::class.java)
            val network = connectivity.activeNetwork
            val vpn = connectivity.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            add(SecurityCheck("VPN", if (vpn) "Actif" else "Inactif", CheckLevel.INFO))
            connectivity.getLinkProperties(network)?.let { link ->
                add(
                    SecurityCheck(
                        "DNS privé",
                        when {
                            link.privateDnsServerName != null -> "Actif : ${link.privateDnsServerName}"
                            link.isPrivateDnsActive -> "Actif (automatique)"
                            else -> "Inactif"
                        },
                        if (link.isPrivateDnsActive) CheckLevel.OK else CheckLevel.INFO,
                    ),
                )
            }
            val sideloaded = sideloadedApps()
            add(
                SecurityCheck(
                    "Applis installées hors Play Store",
                    sideloaded.size.toString(),
                    CheckLevel.INFO,
                    sideloaded.take(MAX_LISTED).joinToString().takeIf { it.isNotEmpty() },
                ),
            )
        }
    }

    private suspend fun specialAccesses(): List<SpecialAccess> {
        val resolver = context.contentResolver
        val accessibility = SecurityChecks.parseComponentPackages(
            Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
        )
        val notificationListeners = shell.output("settings get secure enabled_notification_listeners")
            ?.let(SecurityChecks::parseComponentPackages)
        val admins = context.getSystemService(DevicePolicyManager::class.java).activeAdmins.orEmpty().map { it.packageName }.distinct()
        return listOf(
            SpecialAccess("Accessibilité", "Peut lire l'écran et agir à votre place", accessibility.map(::labelOf)),
            SpecialAccess("Lecture des notifications", "Voit le contenu de toutes vos notifications", notificationListeners?.map(::labelOf)),
            SpecialAccess("Administrateurs de l'appareil", "Peut verrouiller ou effacer le téléphone", admins.map(::labelOf)),
            SpecialAccess("Installation d'applis", "Peut installer d'autres applis", queryOp("REQUEST_INSTALL_PACKAGES")),
            SpecialAccess("Superposition", "Peut s'afficher par-dessus les autres applis", queryOp("SYSTEM_ALERT_WINDOW")),
            SpecialAccess("Accès à tous les fichiers", "Lit et modifie tout le stockage partagé", queryOp("MANAGE_EXTERNAL_STORAGE")),
            SpecialAccess("Données d'usage", "Sait quelles applis vous utilisez et quand", queryOp("GET_USAGE_STATS")),
            SpecialAccess("Modification des réglages système", "Peut changer luminosité, sonnerie…", queryOp("WRITE_SETTINGS")),
        )
    }

    /** Packages allowed for an app-op; needs Shizuku (`appops query-op`). */
    private suspend fun queryOp(op: String): List<String>? =
        shell.output("appops query-op $op allow")?.let(SecurityChecks::parsePackageList)?.map(::labelOf)

    private fun sideloadedApps(): List<String> = installedWithPermissions(withPermissions = false).mapNotNull { info ->
        val app = info.applicationInfo ?: return@mapNotNull null
        if (app.flags and ApplicationInfo.FLAG_SYSTEM != 0) return@mapNotNull null
        val installer = runCatching { pm.getInstallSourceInfo(info.packageName).installingPackageName }.getOrNull()
        if (installer == PLAY_STORE) null else label(app)
    }.sortedBy { it.lowercase() }

    private fun moduleVersion(): String? = runCatching {
        pm.getPackageInfo(MODULE_METADATA, PackageManager.PackageInfoFlags.of(0)).versionName
    }.getOrNull()

    private fun installedWithPermissions(withPermissions: Boolean = true): List<PackageInfo> {
        val flags = if (withPermissions) PackageManager.GET_PERMISSIONS.toLong() else 0L
        return pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags))
    }

    private fun labelOf(packageName: String): String =
        runCatching { label(pm.getApplicationInfo(packageName, 0)) }.getOrDefault(packageName)

    private fun label(app: ApplicationInfo): String = runCatching { app.loadLabel(pm).toString() }.getOrDefault(app.packageName)

    private companion object {
        const val ADB_WIFI_ENABLED = "adb_wifi_enabled"
        const val PLAY_STORE = "com.android.vending"
        const val MODULE_METADATA = "com.google.android.modulemetadata"
        const val MAX_LISTED = 12
    }
}
