package fr.tflag.droidboost.ui.thermal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tflag.droidboost.AppContainer
import fr.tflag.droidboost.core.monitor.HistoryWindow
import fr.tflag.droidboost.core.monitor.Metrics
import fr.tflag.droidboost.data.SeriesPoint
import fr.tflag.droidboost.data.ThermalEpisodeEntity
import fr.tflag.droidboost.shell.ShizukuState
import fr.tflag.droidboost.system.ThermalSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

data class ThermalHistory(
    val headroom: List<SeriesPoint> = emptyList(),
    val batteryTemperature: List<SeriesPoint> = emptyList(),
    val cpuLoad: List<SeriesPoint> = emptyList(),
    /** Hottest sensor per type (Surface, CPU, GPU, Modem…), recorded only while Shizuku is active. */
    val halByType: Map<String, List<SeriesPoint>> = emptyMap(),
)

data class ThermalScreenState(
    val window: HistoryWindow = HistoryWindow.DAY,
    val live: ThermalSnapshot? = null,
    val history: ThermalHistory = ThermalHistory(),
    val episodes: List<ThermalEpisodeEntity> = emptyList(),
    val shizuku: ShizukuState = ShizukuState.Unavailable,
    val monitorEnabled: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ThermalViewModel(private val container: AppContainer) : ViewModel() {

    private val window = MutableStateFlow(HistoryWindow.DAY)
    private val reader = container.newThermalReader()

    private val live = flow {
        while (true) {
            emit(reader.read())
            delay(LIVE_REFRESH_MS)
        }
    }.flowOn(Dispatchers.IO)

    private val refreshTicks = flow {
        while (true) {
            emit(Unit)
            delay(HISTORY_REFRESH_MS)
        }
    }

    private val history = combine(window, refreshTicks) { w, _ -> w }.mapLatest { w ->
        val repo = container.historyRepository
        ThermalHistory(
            headroom = repo.series(Metrics.THERMAL_HEADROOM, w),
            batteryTemperature = repo.series(Metrics.BATTERY_TEMPERATURE, w),
            cpuLoad = repo.series(Metrics.CPU_LOAD, w),
            halByType = repo.tags(Metrics.HAL_TEMPERATURE, w).associateWith { repo.series(Metrics.HAL_TEMPERATURE, w, tag = it) },
        )
    }.flowOn(Dispatchers.IO)

    val state: StateFlow<ThermalScreenState> = combine(
        window,
        live,
        history,
        container.historyRepository.thermalEpisodes(),
        combine(container.shell.state, container.monitorSettings.enabled) { shizuku, enabled -> shizuku to enabled },
    ) { w, snapshot, hist, episodes, (shizuku, enabled) ->
        ThermalScreenState(w, snapshot, hist, episodes, shizuku, enabled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThermalScreenState())

    fun setWindow(value: HistoryWindow) {
        window.value = value
    }

    private companion object {
        const val LIVE_REFRESH_MS = 2_000L
        const val HISTORY_REFRESH_MS = 60_000L
    }
}
