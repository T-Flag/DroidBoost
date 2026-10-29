package fr.tflag.droidboost.monitor

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class MonitorConfig(
    val enabled: Boolean = false,
    /** Interval between two rows written to the history. */
    val recordIntervalSec: Int = 60,
    val retentionDays: Int = 30,
    /** 0 disables the alert. */
    val chargeAlertPercent: Int = 0,
    /** 0 disables the alert. */
    val batteryTempAlertC: Int = 42,
    val thermalAlert: Boolean = true,
)

class MonitorSettings(private val store: DataStore<Preferences>) {

    val config: Flow<MonitorConfig> = store.data.map { prefs ->
        val defaults = MonitorConfig()
        MonitorConfig(
            enabled = prefs[ENABLED] ?: defaults.enabled,
            recordIntervalSec = prefs[RECORD_INTERVAL] ?: defaults.recordIntervalSec,
            retentionDays = prefs[RETENTION] ?: defaults.retentionDays,
            chargeAlertPercent = prefs[CHARGE_ALERT] ?: defaults.chargeAlertPercent,
            batteryTempAlertC = prefs[TEMP_ALERT] ?: defaults.batteryTempAlertC,
            thermalAlert = prefs[THERMAL_ALERT] ?: defaults.thermalAlert,
        )
    }

    val enabled: Flow<Boolean> = config.map { it.enabled }

    suspend fun isEnabled(): Boolean = enabled.first()

    suspend fun setEnabled(value: Boolean) = store.edit { it[ENABLED] = value }

    suspend fun setRecordInterval(seconds: Int) = store.edit { it[RECORD_INTERVAL] = seconds }

    suspend fun setRetention(days: Int) = store.edit { it[RETENTION] = days }

    suspend fun setChargeAlert(percent: Int) = store.edit { it[CHARGE_ALERT] = percent }

    suspend fun setBatteryTempAlert(celsius: Int) = store.edit { it[TEMP_ALERT] = celsius }

    suspend fun setThermalAlert(value: Boolean) = store.edit { it[THERMAL_ALERT] = value }

    private companion object {
        val ENABLED = booleanPreferencesKey("monitor_enabled")
        val RECORD_INTERVAL = intPreferencesKey("record_interval_sec")
        val RETENTION = intPreferencesKey("retention_days")
        val CHARGE_ALERT = intPreferencesKey("charge_alert_percent")
        val TEMP_ALERT = intPreferencesKey("battery_temp_alert_c")
        val THERMAL_ALERT = booleanPreferencesKey("thermal_alert")
    }
}
