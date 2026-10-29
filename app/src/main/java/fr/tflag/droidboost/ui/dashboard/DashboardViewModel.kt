package fr.tflag.droidboost.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tflag.droidboost.AppContainer
import fr.tflag.droidboost.access.DevGrant
import fr.tflag.droidboost.core.model.ThermalDump
import fr.tflag.droidboost.monitor.MonitorService
import fr.tflag.droidboost.shell.ShizukuState
import fr.tflag.droidboost.system.BatterySnapshot
import fr.tflag.droidboost.system.CpuSnapshot
import fr.tflag.droidboost.system.DeviceInfo
import fr.tflag.droidboost.system.MemorySnapshot
import fr.tflag.droidboost.system.NetworkSpeed
import fr.tflag.droidboost.system.StorageTotals
import fr.tflag.droidboost.system.SystemSnapshot
import fr.tflag.droidboost.system.ThermalSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardState(
    val battery: BatterySnapshot? = null,
    val thermal: ThermalSnapshot? = null,
    val cpu: CpuSnapshot? = null,
    val memory: MemorySnapshot? = null,
    val storage: StorageTotals? = null,
    val network: NetworkSpeed? = null,
    val system: SystemSnapshot? = null,
    val shizuku: ShizukuState = ShizukuState.Unavailable,
    val hasBatteryStats: Boolean = false,
    val monitorEnabled: Boolean = false,
)

class DashboardViewModel(private val container: AppContainer) : ViewModel() {

    private val thermalReader = container.newThermalReader()
    private val cpuReader = container.newCpuReader()
    private val memoryReader = container.newMemoryReader()
    private val networkReader = container.newNetworkSpeedReader()
    private var lastHal: ThermalDump? = null

    private val samples = flow {
        var tick = 0
        while (true) {
            emit(sample(tick++))
            delay(REFRESH_MS)
        }
    }.flowOn(Dispatchers.IO)

    val state: StateFlow<DashboardState> = combine(
        samples,
        container.shell.state,
        container.monitorSettings.enabled,
    ) { sample, shizuku, monitor -> sample.copy(shizuku = shizuku, monitorEnabled = monitor) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    private suspend fun sample(tick: Int): DashboardState {
        // The thermal dump is costly: refresh it every other tick and keep the last one meanwhile.
        // It comes from DUMP or Shizuku, so a failed dump (both gone) clears it rather than the Shizuku state.
        val includeHal = tick % HAL_EVERY_TICKS == 0
        val thermal = thermalReader.read(includeHal)
        if (includeHal) lastHal = thermal.hal
        return DashboardState(
            battery = container.battery.read(),
            thermal = thermal.copy(hal = lastHal),
            cpu = cpuReader.read(),
            memory = memoryReader.read(),
            storage = container.storage.totals(),
            network = networkReader.read(),
            system = DeviceInfo.read(),
            hasBatteryStats = container.access.isGranted(DevGrant.BATTERY_STATS),
        )
    }

    fun setMonitorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.monitorSettings.setEnabled(enabled)
            if (enabled) MonitorService.start(container.appContext) else MonitorService.stop(container.appContext)
        }
    }

    private companion object {
        const val REFRESH_MS = 1_000L
        const val HAL_EVERY_TICKS = 2
    }
}
