package fr.tflag.droidboost.system

import android.net.TrafficStats
import android.os.Build
import android.os.SystemClock
import fr.tflag.droidboost.core.model.BatteryMath

data class SystemSnapshot(
    val model: String,
    val androidRelease: String,
    val sdkInt: Int,
    val securityPatch: String,
    val buildId: String,
    val kernel: String?,
    val elapsedRealtimeMs: Long,
    val deepSleepFraction: Float,
)

object DeviceInfo {
    fun read(): SystemSnapshot {
        val elapsed = SystemClock.elapsedRealtime()
        return SystemSnapshot(
            model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidRelease = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH,
            buildId = Build.ID,
            kernel = System.getProperty("os.version"),
            elapsedRealtimeMs = elapsed,
            deepSleepFraction = BatteryMath.deepSleepFraction(elapsed, SystemClock.uptimeMillis()),
        )
    }
}

data class NetworkSpeed(val rxBytesPerSecond: Long, val txBytesPerSecond: Long)

/** Device-wide throughput from global counters (per-app counters only cover the caller since Android 7). */
class NetworkSpeedReader {
    private var lastRx = -1L
    private var lastTx = -1L
    private var lastAt = 0L

    @Synchronized
    fun read(): NetworkSpeed? {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val now = SystemClock.elapsedRealtime()
        if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) return null
        val speed = if (lastAt > 0 && now > lastAt) {
            val seconds = (now - lastAt) / 1000.0
            NetworkSpeed(
                rxBytesPerSecond = ((rx - lastRx).coerceAtLeast(0) / seconds).toLong(),
                txBytesPerSecond = ((tx - lastTx).coerceAtLeast(0) / seconds).toLong(),
            )
        } else {
            null
        }
        lastRx = rx
        lastTx = tx
        lastAt = now
        return speed
    }
}
