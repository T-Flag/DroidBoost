package fr.tflag.droidboost.apps

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import fr.tflag.droidboost.system.AppSize
import fr.tflag.droidboost.system.StorageReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class AppEntry(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val isUpdatedSystem: Boolean,
    val enabled: Boolean,
    /** False when removed for user 0 (`pm uninstall -k --user 0`) but still on the system image. */
    val installedForUser: Boolean,
    val suspended: Boolean,
    val stopped: Boolean,
    val versionName: String?,
    val versionCode: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val installer: String?,
    val size: AppSize?,
    val lastUsed: Long?,
    val foregroundMs30d: Long?,
    val hasLauncher: Boolean,
)

data class PermissionEntry(
    val name: String,
    val label: String,
    val granted: Boolean,
    /** Runtime permissions are the only ones `pm grant` / `pm revoke` can toggle. */
    val dangerous: Boolean,
)

class AppRepository(private val context: Context, private val storage: StorageReader) {

    private val pm: PackageManager = context.packageManager
    private val usage = context.getSystemService(UsageStatsManager::class.java)
    private val iconCache = LruCache<String, Bitmap>(ICON_CACHE_SIZE)
    private val _changes = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** Packages modified by an action in this app, so lists can refresh just that entry. */
    val changes: SharedFlow<String> = _changes.asSharedFlow()

    fun notifyChanged(packageName: String) {
        _changes.tryEmit(packageName)
    }

    suspend fun loadAll(): List<AppEntry> = withContext(Dispatchers.IO) {
        val packages = pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(LIST_FLAGS))
        val usageByPackage = usageStats()
        coroutineScope {
            packages.chunked(CHUNK).map { chunk ->
                async { chunk.mapNotNull { toEntry(it, usageByPackage) } }
            }.awaitAll().flatten()
        }
    }

    suspend fun load(packageName: String): AppEntry? = withContext(Dispatchers.IO) {
        val info = runCatching { pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(LIST_FLAGS)) }.getOrNull()
        info?.let { toEntry(it, usageStats()) }
    }

    suspend fun permissions(packageName: String): List<PermissionEntry> = withContext(Dispatchers.IO) {
        val info = runCatching {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong() or LIST_FLAGS))
        }.getOrNull() ?: return@withContext emptyList()
        val names = info.requestedPermissions.orEmpty()
        val flags = info.requestedPermissionsFlags ?: IntArray(names.size)
        names.mapIndexed { i, name ->
            val permInfo = runCatching { pm.getPermissionInfo(name, 0) }.getOrNull()
            PermissionEntry(
                name = name,
                label = permInfo?.loadLabel(pm)?.toString()?.takeIf { it != name } ?: name.substringAfterLast('.'),
                granted = flags.getOrElse(i) { 0 } and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0,
                dangerous = permInfo?.protection == PermissionInfo.PROTECTION_DANGEROUS,
            )
        }.sortedWith(compareByDescending<PermissionEntry> { it.dangerous }.thenBy { it.label.lowercase() })
    }

    fun icon(packageName: String, sizePx: Int): Bitmap? {
        iconCache.get(packageName)?.let { return it }
        val drawable = runCatching { pm.getApplicationIcon(packageName) }.getOrNull() ?: return null
        return drawable.toBitmap(sizePx, sizePx).also { iconCache.put(packageName, it) }
    }

    fun launchIntent(packageName: String) = pm.getLaunchIntentForPackage(packageName)

    private fun toEntry(info: PackageInfo, usageByPackage: Map<String, Pair<Long, Long>>): AppEntry? {
        val app = info.applicationInfo ?: return null
        val flags = app.flags
        val installed = flags and ApplicationInfo.FLAG_INSTALLED != 0
        val (lastUsed, foreground) = usageByPackage[info.packageName] ?: (null to null)
        return AppEntry(
            packageName = info.packageName,
            label = runCatching { app.loadLabel(pm).toString() }.getOrDefault(info.packageName),
            isSystem = flags and ApplicationInfo.FLAG_SYSTEM != 0,
            isUpdatedSystem = flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0,
            enabled = app.enabled,
            installedForUser = installed,
            suspended = flags and ApplicationInfo.FLAG_SUSPENDED != 0,
            stopped = flags and ApplicationInfo.FLAG_STOPPED != 0,
            versionName = info.versionName,
            versionCode = info.longVersionCode,
            targetSdk = app.targetSdkVersion,
            minSdk = app.minSdkVersion,
            firstInstallTime = info.firstInstallTime,
            lastUpdateTime = info.lastUpdateTime,
            installer = if (installed) installerOf(info.packageName) else null,
            size = if (installed) storage.appSize(info.packageName) else null,
            lastUsed = lastUsed?.takeIf { it > 0 },
            foregroundMs30d = foreground,
            hasLauncher = installed && pm.getLaunchIntentForPackage(info.packageName) != null,
        )
    }

    private fun installerOf(packageName: String): String? = runCatching {
        val source = pm.getInstallSourceInfo(packageName)
        source.installingPackageName ?: source.initiatingPackageName
    }.getOrNull()

    /** Last use and foreground time over 30 days; empty without usage access. */
    private fun usageStats(): Map<String, Pair<Long, Long>> = runCatching {
        val end = System.currentTimeMillis()
        val begin = end - TimeUnit.DAYS.toMillis(USAGE_WINDOW_DAYS)
        usage.queryAndAggregateUsageStats(begin, end).mapValues { (_, stats) ->
            maxOf(stats.lastTimeUsed, stats.lastTimeVisible) to stats.totalTimeInForeground
        }
    }.getOrDefault(emptyMap())

    private companion object {
        val LIST_FLAGS = (PackageManager.MATCH_DISABLED_COMPONENTS or PackageManager.MATCH_UNINSTALLED_PACKAGES).toLong()
        const val CHUNK = 48
        const val ICON_CACHE_SIZE = 400
        const val USAGE_WINDOW_DAYS = 30L
    }
}
