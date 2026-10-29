package fr.tflag.droidboost.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tflag.droidboost.AppContainer
import fr.tflag.droidboost.data.HistoryStats
import fr.tflag.droidboost.monitor.MonitorConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(val config: MonitorConfig = MonitorConfig(), val stats: HistoryStats? = null)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val stats = MutableStateFlow<HistoryStats?>(null)
    private val settings = container.monitorSettings

    val state: StateFlow<SettingsState> = combine(settings.config, stats) { config, s -> SettingsState(config, s) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    init {
        viewModelScope.launch { stats.value = container.historyRepository.stats() }
    }

    fun setRecordInterval(seconds: Int) = launch { settings.setRecordInterval(seconds) }

    fun setRetention(days: Int) = launch { settings.setRetention(days) }

    fun setChargeAlert(percent: Int) = launch { settings.setChargeAlert(percent) }

    fun setBatteryTempAlert(celsius: Int) = launch { settings.setBatteryTempAlert(celsius) }

    fun setThermalAlert(enabled: Boolean) = launch { settings.setThermalAlert(enabled) }

    fun setAnomalyAlerts(enabled: Boolean) = launch { settings.setAnomalyAlerts(enabled) }

    fun setOverlay(enabled: Boolean) = launch { settings.setOverlay(enabled) }

    fun setKeepWirelessDebugging(enabled: Boolean) = launch { settings.setKeepWirelessDebugging(enabled) }

    val canDrawOverlays: Boolean get() = container.access.hasOverlay()

    val canWriteSecureSettings: Boolean get() = container.access.isGranted(fr.tflag.droidboost.access.DevGrant.WRITE_SECURE_SETTINGS)

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
