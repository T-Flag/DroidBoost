package fr.tflag.droidboost.journal

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.tflag.droidboost.core.journal.ExitKind
import fr.tflag.droidboost.core.journal.JournalTypes
import fr.tflag.droidboost.core.journal.PackageChange
import fr.tflag.droidboost.core.journal.PackageDiff
import fr.tflag.droidboost.core.journal.PackageState
import fr.tflag.droidboost.data.HistoryDatabase
import fr.tflag.droidboost.data.JournalEventEntity
import fr.tflag.droidboost.data.PackageSnapshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Writes the unified change journal: package changes (from snapshot diffs), system updates,
 * abnormal process exits, Shizuku state and the actions taken from DroidBoost.
 */
const val SUBTYPE_SYSTEM_UPDATE = "update"

class JournalRecorder(
    private val context: Context,
    private val db: HistoryDatabase,
    private val store: DataStore<Preferences>,
) {
    private val pm: PackageManager = context.packageManager
    private val activityManager = context.getSystemService(ActivityManager::class.java)
    private val mutex = Mutex()
    private val dangerousCache = HashMap<String, Boolean>()

    /** Runs every check; cheap enough to call on app start and periodically from the service. */
    suspend fun refresh() = mutex.withLock {
        withContext(Dispatchers.IO) {
            checkSystemBuild()
            snapshotPackages()
            collectExits()
        }
    }

    suspend fun logAction(packageName: String?, title: String, details: String? = null) {
        insert(JournalEventEntity(ts = now(), type = JournalTypes.ACTION, packageName = packageName, title = title, details = details))
    }

    suspend fun logShizuku(ready: Boolean) {
        insert(
            JournalEventEntity(
                ts = now(),
                type = JournalTypes.SHIZUKU,
                title = if (ready) "Shizuku actif" else "Shizuku arrêté",
                details = if (ready) null else "Les actions avancées et les capteurs sont indisponibles jusqu'à sa relance.",
            ),
        )
    }

    private suspend fun checkSystemBuild() {
        val prefs = store.data.first()
        val fingerprint = Build.FINGERPRINT
        val previous = prefs[LAST_FINGERPRINT]
        if (previous == fingerprint) return
        if (previous != null) {
            insert(
                JournalEventEntity(
                    ts = now(),
                    type = JournalTypes.SYSTEM,
                    title = "Mise à jour du système : Android ${Build.VERSION.RELEASE}",
                    details = "Correctif ${prefs[LAST_PATCH] ?: "?"} → ${Build.VERSION.SECURITY_PATCH} · build ${Build.ID}",
                    subtype = SUBTYPE_SYSTEM_UPDATE,
                ),
            )
        }
        store.edit {
            it[LAST_FINGERPRINT] = fingerprint
            it[LAST_PATCH] = Build.VERSION.SECURITY_PATCH
        }
    }

    private suspend fun snapshotPackages() {
        val current = currentStates()
        val dao = db.packageSnapshots()
        val stored = dao.all()
        if (stored.isEmpty()) {
            insert(
                JournalEventEntity(
                    ts = now(),
                    type = JournalTypes.SYSTEM,
                    title = "Début du journal",
                    details = "${current.size} applis enregistrées comme point de départ",
                ),
            )
        } else {
            val changes = PackageDiff.diff(stored.map { it.toState() }, current)
            if (changes.isNotEmpty()) db.journal().insertAll(changes.map(::toEvent))
        }
        dao.clear()
        dao.insertAll(current.map { it.toEntity() })
    }

    /** Abnormal exits of every app since the last pass; other apps' exits need the DUMP permission. */
    private suspend fun collectExits() {
        val since = store.data.first()[LAST_EXIT_TS] ?: (now() - FIRST_EXIT_LOOKBACK_MS)
        var newest = since
        val events = mutableListOf<JournalEventEntity>()
        installedPackages().forEach { info ->
            val exits = runCatching { activityManager.getHistoricalProcessExitReasons(info.packageName, 0, MAX_EXITS_PER_APP) }
                .getOrNull() ?: return@forEach
            exits.filter { it.timestamp > since }.forEach { exit ->
                newest = maxOf(newest, exit.timestamp)
                val kind = ExitKind.classify(exit.reason, exit.description) ?: return@forEach
                events += JournalEventEntity(
                    ts = exit.timestamp,
                    type = JournalTypes.EXIT,
                    packageName = info.packageName,
                    title = "${kind.label} : ${label(info)}",
                    details = listOfNotNull(
                        exit.processName.takeIf { it != info.packageName }?.let { "processus $it" },
                        exit.description?.take(MAX_DESCRIPTION),
                        exit.pss.takeIf { it > 0 }?.let { "PSS ${it / 1024} Mo" },
                    ).joinToString(" · ").ifEmpty { null },
                    subtype = kind.name,
                )
            }
        }
        if (events.isNotEmpty()) db.journal().insertAll(events)
        store.edit { it[LAST_EXIT_TS] = newest }
    }

    private fun currentStates(): List<PackageState> = installedPackages(withPermissions = true).mapNotNull { info ->
        val app = info.applicationInfo ?: return@mapNotNull null
        val names = info.requestedPermissions.orEmpty()
        val flags = info.requestedPermissionsFlags ?: IntArray(names.size)
        PackageState(
            packageName = info.packageName,
            label = label(info),
            versionName = info.versionName,
            versionCode = info.longVersionCode,
            lastUpdateTime = info.lastUpdateTime,
            enabled = app.enabled,
            installedForUser = app.flags and ApplicationInfo.FLAG_INSTALLED != 0,
            grantedPermissions = names.indices
                .filter { flags.getOrElse(it) { 0 } and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 }
                .map { names[it] }
                .filter(::isDangerous)
                .toSet(),
        )
    }

    private fun installedPackages(withPermissions: Boolean = false): List<PackageInfo> {
        var flags = PackageManager.MATCH_DISABLED_COMPONENTS.toLong() or PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong()
        if (withPermissions) flags = flags or PackageManager.GET_PERMISSIONS.toLong()
        return pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags))
    }

    private fun isDangerous(permission: String): Boolean = dangerousCache.getOrPut(permission) {
        runCatching { pm.getPermissionInfo(permission, 0).protection == PermissionInfo.PROTECTION_DANGEROUS }.getOrDefault(false)
    }

    private fun label(info: PackageInfo): String =
        info.applicationInfo?.let { runCatching { it.loadLabel(pm).toString() }.getOrNull() } ?: info.packageName

    private fun toEvent(change: PackageChange): JournalEventEntity {
        val (type, title, details) = when (change) {
            is PackageChange.Installed -> Triple(JournalTypes.APP, "Installée : ${change.label}", change.versionName?.let { "version $it" })
            is PackageChange.Removed -> Triple(JournalTypes.APP, "Désinstallée : ${change.label}", null)
            is PackageChange.Updated -> Triple(
                JournalTypes.APP,
                "Mise à jour : ${change.label}",
                "${change.fromVersion ?: "?"} → ${change.toVersion ?: "?"}",
            )
            is PackageChange.EnabledChanged ->
                Triple(JournalTypes.APP, (if (change.enabled) "Réactivée : " else "Désactivée : ") + change.label, null)
            is PackageChange.InstalledForUserChanged -> Triple(
                JournalTypes.APP,
                (if (change.installed) "Réinstallée : " else "Retirée pour l'utilisateur : ") + change.label,
                null,
            )
            is PackageChange.PermissionGranted ->
                Triple(JournalTypes.PERMISSION, "Permission accordée : ${change.label}", PackageDiff.shortPermission(change.permission))
            is PackageChange.PermissionRevoked ->
                Triple(JournalTypes.PERMISSION, "Permission retirée : ${change.label}", PackageDiff.shortPermission(change.permission))
        }
        return JournalEventEntity(ts = now(), type = type, packageName = change.packageName, title = title, details = details)
    }

    private suspend fun insert(event: JournalEventEntity) = db.journal().insertAll(listOf(event))

    private fun now() = System.currentTimeMillis()

    private fun PackageSnapshotEntity.toState() = PackageState(
        packageName = packageName,
        label = label,
        versionName = versionName,
        versionCode = versionCode,
        lastUpdateTime = lastUpdateTime,
        enabled = enabled,
        installedForUser = installedForUser,
        grantedPermissions = grantedPermissions.split('\n').filter { it.isNotBlank() }.toSet(),
    )

    private fun PackageState.toEntity() = PackageSnapshotEntity(
        packageName = packageName,
        label = label,
        versionName = versionName,
        versionCode = versionCode,
        lastUpdateTime = lastUpdateTime,
        enabled = enabled,
        installedForUser = installedForUser,
        grantedPermissions = grantedPermissions.sorted().joinToString("\n"),
    )

    private companion object {
        val LAST_FINGERPRINT = stringPreferencesKey("journal_last_fingerprint")
        val LAST_PATCH = stringPreferencesKey("journal_last_patch")
        val LAST_EXIT_TS = longPreferencesKey("journal_last_exit_ts")
        const val MAX_EXITS_PER_APP = 16
        const val MAX_DESCRIPTION = 160
        const val FIRST_EXIT_LOOKBACK_MS = 7 * 24 * 3_600_000L
    }
}
