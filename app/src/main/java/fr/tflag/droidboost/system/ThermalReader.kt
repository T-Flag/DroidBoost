package fr.tflag.droidboost.system

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import fr.tflag.droidboost.core.model.ThermalDump
import fr.tflag.droidboost.core.model.ThermalStatus
import fr.tflag.droidboost.core.parse.ThermalDumpParser
import fr.tflag.droidboost.core.shell.ShellCommands
import fr.tflag.droidboost.shell.ShizukuShell

data class ThermalSnapshot(
    val status: ThermalStatus,
    /** 1.0 means the device reached SEVERE throttling; null when unsupported or rate limited. */
    val headroom: Float?,
    /** Headroom value at which each status starts (Android 15+). */
    val headroomThresholds: Map<ThermalStatus, Float>,
    /** HAL sensors, only through Shizuku. */
    val hal: ThermalDump?,
)

class ThermalReader(context: Context, private val shell: ShizukuShell) {

    private val power = context.getSystemService(PowerManager::class.java)
    private var lastHeadroomAt = 0L
    private var lastHeadroom: Float? = null

    suspend fun read(includeHal: Boolean = true): ThermalSnapshot = ThermalSnapshot(
        status = ThermalStatus.of(power.currentThermalStatus),
        headroom = headroom(),
        headroomThresholds = thresholds(),
        hal = if (includeHal) shell.output(ShellCommands.THERMAL_DUMP)?.let(ThermalDumpParser::parse) else null,
    )

    /** AOSP returns NaN when called again within 500 ms, so cache the value for a second. */
    @Synchronized
    private fun headroom(): Float? {
        val now = SystemClock.elapsedRealtime()
        if (now - lastHeadroomAt >= HEADROOM_MIN_INTERVAL_MS) {
            lastHeadroom = runCatching { power.getThermalHeadroom(0) }.getOrNull()?.takeUnless { it.isNaN() }
            lastHeadroomAt = now
        }
        return lastHeadroom
    }

    private fun thresholds(): Map<ThermalStatus, Float> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return emptyMap()
        return runCatching { power.thermalHeadroomThresholds }.getOrNull()
            ?.mapKeys { ThermalStatus.of(it.key) }
            .orEmpty()
    }

    private companion object {
        const val HEADROOM_MIN_INTERVAL_MS = 1_000L
    }
}
