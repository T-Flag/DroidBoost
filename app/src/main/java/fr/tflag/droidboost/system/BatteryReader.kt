package fr.tflag.droidboost.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import fr.tflag.droidboost.core.model.BatteryMath
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class BatterySnapshot(
    val levelPercent: Int,
    val status: Int,
    val plugged: Int,
    val health: Int,
    val temperatureC: Float,
    val voltageMv: Int,
    /** Magnitude in mA; the direction comes from [isCharging]. */
    val currentMa: Double?,
    val powerW: Double?,
    val chargeCounterMah: Double?,
    val cycleCount: Int?,
    /** Needs BATTERY_STATS (system API), granted once with `pm grant`. */
    val stateOfHealthPercent: Int?,
    val manufacturingDate: LocalDate?,
    val firstUseDate: LocalDate?,
    val technology: String?,
) {
    val isCharging: Boolean
        get() = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            (status == BatteryManager.BATTERY_STATUS_FULL && plugged != 0)

    val statusLabel: String
        get() = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "En charge"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Décharge"
            BatteryManager.BATTERY_STATUS_FULL -> "Pleine"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Branchée, charge en pause"
            else -> "Inconnu"
        }

    val pluggedLabel: String
        get() = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Secteur"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Sans fil"
            BatteryManager.BATTERY_PLUGGED_DOCK -> "Dock"
            else -> "Débranchée"
        }

    val healthLabel: String
        get() = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Bonne"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Surchauffe"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Hors service"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Surtension"
            BatteryManager.BATTERY_HEALTH_COLD -> "Trop froide"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Défaillance"
            else -> "Inconnue"
        }
}

class BatteryReader(private val context: Context) {

    private val manager = context.getSystemService(BatteryManager::class.java)

    fun read(): BatterySnapshot? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED)
            ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).takeIf { it > 0 } ?: 100
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val current = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            .takeIf { it != Int.MIN_VALUE }
            ?.let(BatteryMath::currentMilliAmps)
        val chargeCounter = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            .takeIf { it > 0 }
            ?.let { it / 1000.0 }
        return BatterySnapshot(
            levelPercent = if (level >= 0) level * 100 / scale else 0,
            status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN),
            plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
            health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN),
            temperatureC = BatteryMath.celsius(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)),
            voltageMv = voltage,
            currentMa = current,
            powerW = current?.takeIf { voltage > 0 }?.let { BatteryMath.powerWatts(voltage, it) },
            chargeCounterMah = chargeCounter,
            cycleCount = intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1).takeIf { it >= 0 },
            stateOfHealthPercent = privilegedInt(PROPERTY_STATE_OF_HEALTH)?.takeIf { it in 1..100 },
            manufacturingDate = privilegedDate(PROPERTY_MANUFACTURING_DATE),
            firstUseDate = privilegedDate(PROPERTY_FIRST_USAGE_DATE),
            technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY),
        )
    }

    // The following properties are @SystemApi and throw SecurityException without BATTERY_STATS.
    private fun privilegedInt(id: Int): Int? =
        runCatching { manager.getIntProperty(id) }.getOrNull()?.takeIf { it != Int.MIN_VALUE }

    private fun privilegedDate(id: Int): LocalDate? =
        runCatching { manager.getLongProperty(id) }.getOrNull()
            ?.takeIf { it > 0 && it != Long.MIN_VALUE }
            ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalDate() }

    private companion object {
        // Hidden BatteryManager.BATTERY_PROPERTY_* ids (Android 14+).
        const val PROPERTY_MANUFACTURING_DATE = 7
        const val PROPERTY_FIRST_USAGE_DATE = 8
        const val PROPERTY_STATE_OF_HEALTH = 10
    }
}
