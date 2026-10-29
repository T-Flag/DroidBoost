package fr.tflag.droidboost.system

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Process
import android.os.storage.StorageManager

data class StorageTotals(val totalBytes: Long, val freeBytes: Long) {
    val usedBytes: Long get() = totalBytes - freeBytes
    val usedFraction: Float get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
}

data class MediaBreakdown(
    val imageBytes: Long,
    val videoBytes: Long,
    val audioBytes: Long,
    val externalTotalBytes: Long,
)

data class AppSize(val appBytes: Long, val dataBytes: Long, val cacheBytes: Long) {
    /** `dataBytes` already includes the cache. */
    val totalBytes: Long get() = appBytes + dataBytes
}

class StorageReader(context: Context) {

    private val stats = context.getSystemService(StorageStatsManager::class.java)
    private val user = Process.myUserHandle()

    fun totals(): StorageTotals? = runCatching {
        StorageTotals(
            totalBytes = stats.getTotalBytes(StorageManager.UUID_DEFAULT),
            freeBytes = stats.getFreeBytes(StorageManager.UUID_DEFAULT),
        )
    }.getOrNull()

    /** Needs usage access. */
    fun media(): MediaBreakdown? = runCatching {
        val external = stats.queryExternalStatsForUser(StorageManager.UUID_DEFAULT, user)
        MediaBreakdown(
            imageBytes = external.imageBytes,
            videoBytes = external.videoBytes,
            audioBytes = external.audioBytes,
            externalTotalBytes = external.totalBytes,
        )
    }.getOrNull()

    /** Needs usage access; null for packages not installed for this user. */
    fun appSize(packageName: String): AppSize? = runCatching {
        val app = stats.queryStatsForPackage(StorageManager.UUID_DEFAULT, packageName, user)
        AppSize(appBytes = app.appBytes, dataBytes = app.dataBytes, cacheBytes = app.cacheBytes)
    }.getOrNull()
}
