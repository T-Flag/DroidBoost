package fr.tflag.droidboost.monitor

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MonitorSettings(private val store: DataStore<Preferences>) {

    val enabled: Flow<Boolean> = store.data.map { it[ENABLED] ?: false }

    suspend fun isEnabled(): Boolean = enabled.first()

    suspend fun setEnabled(value: Boolean) {
        store.edit { it[ENABLED] = value }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("monitor_enabled")
    }
}
