package fr.tflag.droidboost.system

import android.content.Context
import android.os.Build
import android.os.OutcomeReceiver
import android.os.PowerMonitor
import android.os.PowerMonitorReadings
import android.os.health.SystemHealthManager
import androidx.annotation.RequiresApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class RailPower(val name: String, val isConsumer: Boolean, val watts: Double)

/**
 * Energy per subsystem (CPU clusters, GPU, modem, display…) from the on-device power monitors (ODPM)
 * exposed since Android 15. Readings refresh every ~20 s for normal apps, and much faster with
 * ACCESS_FINE_POWER_MONITORS.
 */
class PowerRailsReader(context: Context) {

    private val health = context.getSystemService(SystemHealthManager::class.java)
    private var monitors: List<PowerMonitor>? = null
    private var previous: Map<String, Pair<Long, Long>> = emptyMap()

    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

    /** Average power per rail since the previous call; empty on the first call or when unsupported. */
    suspend fun read(): List<RailPower> {
        if (!isSupported) return emptyList()
        return readApi35()
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private suspend fun readApi35(): List<RailPower> {
        val available = monitors ?: supportedMonitors().also { monitors = it }
        if (available.isEmpty()) return emptyList()
        val readings = readings(available) ?: return emptyList()
        val current = available.mapNotNull { monitor ->
            val energy = readings.getConsumedEnergy(monitor)
            val timestamp = readings.getTimestampMillis(monitor)
            if (energy < 0 || timestamp <= 0) null else monitor.name to (energy to timestamp)
        }.toMap()
        val before = previous
        previous = current
        return available.mapNotNull { monitor ->
            val (energyNow, timeNow) = current[monitor.name] ?: return@mapNotNull null
            val (energyThen, timeThen) = before[monitor.name] ?: return@mapNotNull null
            val dtMs = timeNow - timeThen
            if (dtMs <= 0) return@mapNotNull null
            // Energy is reported in microjoules (µW·s): µJ / ms = mW.
            RailPower(
                name = monitor.name,
                isConsumer = monitor.type == PowerMonitor.POWER_MONITOR_TYPE_CONSUMER,
                watts = (energyNow - energyThen).toDouble() / dtMs / 1000.0,
            )
        }.sortedByDescending { it.watts }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private suspend fun supportedMonitors(): List<PowerMonitor> = withTimeoutOrNull(TIMEOUT_MS) {
        suspendCancellableCoroutine { continuation ->
            health.getSupportedPowerMonitors(Runnable::run) { list -> if (continuation.isActive) continuation.resume(list) }
        }
    }.orEmpty()

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private suspend fun readings(monitors: List<PowerMonitor>): PowerMonitorReadings? = withTimeoutOrNull(TIMEOUT_MS) {
        suspendCancellableCoroutine { continuation ->
            health.getPowerMonitorReadings(
                monitors,
                Runnable::run,
                object : OutcomeReceiver<PowerMonitorReadings, RuntimeException> {
                    override fun onResult(result: PowerMonitorReadings) {
                        if (continuation.isActive) continuation.resume(result)
                    }

                    override fun onError(error: RuntimeException) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                },
            )
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
