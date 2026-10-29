package fr.tflag.droidboost.ui.battery

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tflag.droidboost.AppContainer
import fr.tflag.droidboost.access.DevGrant
import fr.tflag.droidboost.core.monitor.BatteryHealth
import fr.tflag.droidboost.core.monitor.HistoryWindow
import fr.tflag.droidboost.core.monitor.Metrics
import fr.tflag.droidboost.data.ChargeSessionEntity
import fr.tflag.droidboost.data.SeriesPoint
import fr.tflag.droidboost.maintenance.AppBatteryUse
import fr.tflag.droidboost.system.BatterySnapshot
import fr.tflag.droidboost.system.PowerRailsReader
import fr.tflag.droidboost.system.RailPower
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
import kotlinx.coroutines.launch

data class BatteryHistory(
    val level: List<SeriesPoint> = emptyList(),
    val power: List<SeriesPoint> = emptyList(),
    val temperature: List<SeriesPoint> = emptyList(),
    val dischargePerHour: Double? = null,
)

data class BatteryScreenState(
    val window: HistoryWindow = HistoryWindow.DAY,
    val battery: BatterySnapshot? = null,
    val history: BatteryHistory = BatteryHistory(),
    val sessions: List<ChargeSessionEntity> = emptyList(),
    val designCapacityMah: Int? = null,
    val gaugeCapacityMah: Double? = null,
    val sessionCapacityMah: Double? = null,
    val hasBatteryStats: Boolean = false,
    val monitorEnabled: Boolean = false,
    /** Null until loaded, or when neither DUMP nor Shizuku is available. */
    val appUsage: List<AppBatteryUse>? = null,
    val appUsageLoading: Boolean = true,
    val rails: List<RailPower> = emptyList(),
    val railsSupported: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class BatteryViewModel(private val container: AppContainer) : ViewModel() {

    private val window = MutableStateFlow(HistoryWindow.DAY)
    private val design = BatteryHealth.designCapacityMah(Build.DEVICE)

    private val live = flow {
        while (true) {
            emit(container.battery.read())
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
        val level = repo.series(Metrics.BATTERY_LEVEL, w)
        BatteryHistory(
            level = level,
            power = repo.series(Metrics.BATTERY_POWER_W, w),
            temperature = repo.series(Metrics.BATTERY_TEMPERATURE, w),
            dischargePerHour = BatteryHealth.dischargeRatePerHour(
                level.map { it.bucket to it.value },
                repo.series(Metrics.BATTERY_PLUGGED, w).map { it.bucket to it.value },
            ),
        )
    }.flowOn(Dispatchers.IO)

    private val sessionCapacity = flow {
        emit(BatteryHealth.medianEstimate(container.historyRepository.capacityEstimates()))
    }.flowOn(Dispatchers.IO)

    private val railsReader = PowerRailsReader(container.appContext)
    private val appUsage = MutableStateFlow<Pair<List<AppBatteryUse>?, Boolean>>(null to true)

    private val rails = flow {
        emit(railsReader.read())
        while (true) {
            delay(RAILS_REFRESH_MS)
            emit(railsReader.read())
        }
    }.flowOn(Dispatchers.IO)

    private val base = combine(
        window,
        live,
        history,
        container.historyRepository.chargeSessions(),
        combine(sessionCapacity, container.monitorSettings.enabled) { capacity, enabled -> capacity to enabled },
    ) { w, battery, hist, sessions, (sessionCapacity, monitorEnabled) ->
        BatteryScreenState(
            window = w,
            battery = battery,
            history = hist,
            sessions = sessions,
            designCapacityMah = design,
            gaugeCapacityMah = battery?.chargeCounterMah?.let { BatteryHealth.gaugeFullCapacityMah(it, battery.levelPercent) },
            sessionCapacityMah = sessionCapacity,
            hasBatteryStats = container.access.isGranted(DevGrant.BATTERY_STATS),
            monitorEnabled = monitorEnabled,
        )
    }

    val state: StateFlow<BatteryScreenState> = combine(base, appUsage, rails) { screen, (usage, loading), railPower ->
        screen.copy(appUsage = usage, appUsageLoading = loading, rails = railPower, railsSupported = railsReader.isSupported)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BatteryScreenState(designCapacityMah = design))

    init {
        refreshAppUsage()
    }

    fun refreshAppUsage() {
        appUsage.value = appUsage.value.first to true
        viewModelScope.launch { appUsage.value = container.batteryUsage.sinceLastCharge() to false }
    }

    fun setWindow(value: HistoryWindow) {
        window.value = value
    }

    private companion object {
        const val LIVE_REFRESH_MS = 2_000L
        const val HISTORY_REFRESH_MS = 60_000L
        const val RAILS_REFRESH_MS = 20_000L
    }
}
