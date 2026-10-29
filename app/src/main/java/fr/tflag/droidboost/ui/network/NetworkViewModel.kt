package fr.tflag.droidboost.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tflag.droidboost.AppContainer
import fr.tflag.droidboost.network.AppTraffic
import fr.tflag.droidboost.network.DayTraffic
import fr.tflag.droidboost.network.NetworkUsageReader
import fr.tflag.droidboost.system.NetworkSpeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

enum class TrafficPeriod(val label: String, val days: Long) {
    TODAY("Aujourd'hui", 0),
    WEEK("7 jours", 7),
    MONTH("30 jours", 30),
}

data class NetworkState(
    val period: TrafficPeriod = TrafficPeriod.TODAY,
    val loading: Boolean = true,
    val apps: List<AppTraffic> = emptyList(),
    val days: List<DayTraffic> = emptyList(),
    val speed: NetworkSpeed? = null,
    val hasUsageAccess: Boolean = false,
) {
    val mobileBytes: Long get() = apps.sumOf { it.mobileBytes }
    val wifiBytes: Long get() = apps.sumOf { it.wifiBytes }
}

class NetworkViewModel(private val container: AppContainer) : ViewModel() {

    private val reader = NetworkUsageReader(container.appContext)
    private val speedReader = container.newNetworkSpeedReader()
    private val content = MutableStateFlow(NetworkState())

    private val speed = flow {
        while (true) {
            emit(speedReader.read())
            delay(1_000)
        }
    }.flowOn(Dispatchers.IO)

    val state: StateFlow<NetworkState> = combine(content, speed) { network, live -> network.copy(speed = live) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NetworkState())

    init {
        load(TrafficPeriod.TODAY)
    }

    fun load(period: TrafficPeriod) {
        content.value = content.value.copy(period = period, loading = true)
        viewModelScope.launch {
            val (apps, days) = withContext(Dispatchers.IO) {
                val zone = ZoneId.systemDefault()
                val start = LocalDate.now(zone).minusDays(period.days).atStartOfDay(zone).toInstant().toEpochMilli()
                reader.perApp(start, System.currentTimeMillis()) to reader.perDay(CHART_DAYS)
            }
            content.value = content.value.copy(
                loading = false,
                apps = apps,
                days = days,
                hasUsageAccess = container.access.hasUsageStats(),
            )
        }
    }

    private companion object {
        const val CHART_DAYS = 30
    }
}
