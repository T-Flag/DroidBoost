package fr.tflag.droidboost.core.journal

/** What the journal remembers about an installed package between two snapshots. */
data class PackageState(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val lastUpdateTime: Long,
    val enabled: Boolean,
    val installedForUser: Boolean,
    /** Granted runtime permissions. */
    val grantedPermissions: Set<String>,
)

sealed interface PackageChange {
    val packageName: String
    val label: String

    data class Installed(override val packageName: String, override val label: String, val versionName: String?) : PackageChange
    data class Removed(override val packageName: String, override val label: String) : PackageChange
    data class Updated(
        override val packageName: String,
        override val label: String,
        val fromVersion: String?,
        val toVersion: String?,
    ) : PackageChange
    data class EnabledChanged(override val packageName: String, override val label: String, val enabled: Boolean) : PackageChange
    data class InstalledForUserChanged(override val packageName: String, override val label: String, val installed: Boolean) : PackageChange
    data class PermissionGranted(override val packageName: String, override val label: String, val permission: String) : PackageChange
    data class PermissionRevoked(override val packageName: String, override val label: String, val permission: String) : PackageChange
}

object PackageDiff {

    fun diff(before: Collection<PackageState>, after: Collection<PackageState>): List<PackageChange> {
        val old = before.associateBy { it.packageName }
        val new = after.associateBy { it.packageName }
        val changes = mutableListOf<PackageChange>()
        new.values.sortedBy { it.label.lowercase() }.forEach { now ->
            val was = old[now.packageName]
            if (was == null) {
                changes += PackageChange.Installed(now.packageName, now.label, now.versionName)
                return@forEach
            }
            // Restoring an archived app rewrites its APK: that is no update unless the version changed.
            val reinstalled = now.installedForUser && !was.installedForUser
            if (now.versionCode != was.versionCode || (now.lastUpdateTime != was.lastUpdateTime && !reinstalled)) {
                changes += PackageChange.Updated(now.packageName, now.label, was.versionName, now.versionName)
            }
            if (now.installedForUser != was.installedForUser) {
                changes += PackageChange.InstalledForUserChanged(now.packageName, now.label, now.installedForUser)
            }
            if (now.enabled != was.enabled) {
                changes += PackageChange.EnabledChanged(now.packageName, now.label, now.enabled)
            }
            (now.grantedPermissions - was.grantedPermissions).sorted().forEach {
                changes += PackageChange.PermissionGranted(now.packageName, now.label, it)
            }
            (was.grantedPermissions - now.grantedPermissions).sorted().forEach {
                changes += PackageChange.PermissionRevoked(now.packageName, now.label, it)
            }
        }
        old.values.filter { it.packageName !in new }.sortedBy { it.label.lowercase() }.forEach {
            changes += PackageChange.Removed(it.packageName, it.label)
        }
        return changes
    }

    /** Short permission name for display: `android.permission.CAMERA` → `CAMERA`. */
    fun shortPermission(permission: String): String = permission.substringAfterLast('.')
}
