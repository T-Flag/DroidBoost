package fr.tflag.droidboost.network

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import java.time.LocalDate
import java.time.ZoneId

data class AppTraffic(
    val uid: Int,
    val label: String,
    val packageName: String?,
    val mobileBytes: Long,
    val wifiBytes: Long,
) {
    val totalBytes: Long get() = mobileBytes + wifiBytes
}

data class DayTraffic(val day: LocalDate, val mobileBytes: Long, val wifiBytes: Long)

/** Per-app and per-day data usage from the system's network statistics (needs usage access). */
class NetworkUsageReader(private val context: Context) {

    private val stats = context.getSystemService(NetworkStatsManager::class.java)
    private val pm = context.packageManager

    @Suppress("DEPRECATION") // TYPE_MOBILE / TYPE_WIFI remain the only public way to pick a transport here.
    fun perApp(start: Long, end: Long): List<AppTraffic> {
        val byUid = HashMap<Int, LongArray>()
        listOf(ConnectivityManager.TYPE_MOBILE to 0, ConnectivityManager.TYPE_WIFI to 1).forEach { (type, slot) ->
            runCatching {
                stats.querySummary(type, null, start, end).use { summary ->
                    val bucket = NetworkStats.Bucket()
                    while (summary.hasNextBucket()) {
                        summary.getNextBucket(bucket)
                        byUid.getOrPut(bucket.uid) { LongArray(2) }[slot] += bucket.rxBytes + bucket.txBytes
                    }
                }
            }
        }
        return byUid.map { (uid, bytes) ->
            val (label, pkg) = describe(uid)
            AppTraffic(uid, label, pkg, mobileBytes = bytes[0], wifiBytes = bytes[1])
        }.filter { it.totalBytes > 0 }.sortedByDescending { it.totalBytes }
    }

    @Suppress("DEPRECATION")
    fun perDay(days: Int): List<DayTraffic> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        return (days - 1 downTo 0).map { back ->
            val day = today.minusDays(back.toLong())
            val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            DayTraffic(day, deviceBytes(ConnectivityManager.TYPE_MOBILE, start, end), deviceBytes(ConnectivityManager.TYPE_WIFI, start, end))
        }
    }

    private fun deviceBytes(type: Int, start: Long, end: Long): Long = runCatching {
        stats.querySummaryForDevice(type, null, start, end).let { it.rxBytes + it.txBytes }
    }.getOrDefault(0L)

    private fun describe(uid: Int): Pair<String, String?> = when (uid) {
        UID_REMOVED -> "Applis désinstallées" to null
        UID_TETHERING -> "Partage de connexion" to null
        0 -> "Noyau et root" to null
        SYSTEM_UID -> "Système Android" to null
        else -> {
            val packages = pm.getPackagesForUid(uid).orEmpty()
            val pkg = packages.firstOrNull()
            val label = pkg?.let {
                runCatching { pm.getApplicationLabel(pm.getApplicationInfo(it, 0)).toString() }.getOrDefault(it)
            } ?: "UID $uid"
            (if (packages.size > 1) "$label (+${packages.size - 1})" else label) to pkg
        }
    }

    private companion object {
        // NetworkStats.Bucket.UID_REMOVED / UID_TETHERING and Process.SYSTEM_UID.
        const val UID_REMOVED = -4
        const val UID_TETHERING = -5
        const val SYSTEM_UID = 1000
    }
}
