package fr.tflag.droidboost.ui.journal

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tflag.droidboost.AppContainer
import fr.tflag.droidboost.access.DevGrant
import fr.tflag.droidboost.core.journal.ComparedMetric
import fr.tflag.droidboost.core.journal.JournalTypes
import fr.tflag.droidboost.core.monitor.BatteryHealth
import fr.tflag.droidboost.core.monitor.Metrics
import fr.tflag.droidboost.data.JournalEventEntity
import fr.tflag.droidboost.data.PackageCount
import fr.tflag.droidboost.journal.Exporter
import fr.tflag.droidboost.journal.SUBTYPE_SYSTEM_UPDATE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class JournalFilter(val label: String, val types: Set<String>?) {
    ALL("Tout", null),
    APPS("Applis", setOf(JournalTypes.APP)),
    PERMISSIONS("Permissions", setOf(JournalTypes.PERMISSION)),
    STABILITY("Stabilité", setOf(JournalTypes.EXIT)),
    SYSTEM("Système", setOf(JournalTypes.SYSTEM, JournalTypes.SHIZUKU, JournalTypes.ANOMALY)),
    ACTIONS("Actions", setOf(JournalTypes.ACTION)),
}

data class Comparison(val title: String, val metrics: List<ComparedMetric>)

data class JournalState(
    val filter: JournalFilter = JournalFilter.ALL,
    val events: List<JournalEventEntity> = emptyList(),
    val comparison: Comparison? = null,
    val unstableApps: List<Pair<String, Int>> = emptyList(),
    val hasDump: Boolean = false,
    val exporting: Boolean = false,
)

class JournalViewModel(private val container: AppContainer) : ViewModel() {

    private val filter = MutableStateFlow(JournalFilter.ALL)
    private val extras = MutableStateFlow(Triple<Comparison?, List<Pair<String, Int>>, Boolean>(null, emptyList(), false))
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    val state: StateFlow<JournalState> = combine(filter, container.history.journal().recent(EVENT_LIMIT), extras) { f, events, (comparison, unstable, exporting) ->
        JournalState(
            filter = f,
            events = f.types?.let { types -> events.filter { it.type in types } } ?: events,
            comparison = comparison,
            unstableApps = unstable,
            hasDump = container.access.isGranted(DevGrant.DUMP),
            exporting = exporting,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalState())

    init {
        refresh()
    }

    fun setFilter(value: JournalFilter) {
        filter.value = value
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { container.journal.refresh() }
            val comparison = withContext(Dispatchers.IO) { comparison() }
            val unstable = withContext(Dispatchers.IO) {
                container.history.journal().topPackages(JournalTypes.EXIT, System.currentTimeMillis() - WEEK_MS, 5).map(::labelled)
            }
            extras.value = extras.value.copy(first = comparison, second = unstable)
        }
    }

    fun export(target: Uri) {
        viewModelScope.launch {
            extras.value = extras.value.copy(third = true)
            val result = runCatching { Exporter(container.appContext, container.history).exportZip(target) }
            extras.value = extras.value.copy(third = false)
            _messages.send(result.fold({ "Export terminé : $it lignes" }, { "Export impossible : ${it.message}" }))
        }
    }

    /**
     * Compares the week after the latest system update with the week before it;
     * without any update, compares the last 7 days with the 7 previous ones.
     */
    private suspend fun comparison(): Comparison? {
        val db = container.history
        val now = System.currentTimeMillis()
        val update = db.journal().latest(JournalTypes.SYSTEM, SUBTYPE_SYSTEM_UPDATE)
        val pivot = update?.ts ?: (now - WEEK_MS)
        val beforeStart = pivot - WEEK_MS
        val afterEnd = minOf(now, pivot + WEEK_MS)
        if (afterEnd - pivot < MIN_AFTER_MS) return null
        val samples = db.samples()

        suspend fun average(metric: String, start: Long, end: Long, tag: String = "") = samples.average(metric, tag, start, end)

        suspend fun discharge(start: Long, end: Long): Double? {
            val bucket = 10 * 60_000L
            val level = samples.seriesBetween(Metrics.BATTERY_LEVEL, "", start, end, bucket).map { it.bucket to it.value }
            val plugged = samples.seriesBetween(Metrics.BATTERY_PLUGGED, "", start, end, bucket).map { it.bucket to it.value }
            return BatteryHealth.dischargeRatePerHour(level, plugged)
        }

        fun perDay(count: Int, start: Long, end: Long): Double = count / ((end - start).toDouble() / DAY_MS)

        val metrics = listOf(
            ComparedMetric("Décharge débranché (%/h)", discharge(beforeStart, pivot), discharge(pivot, afterEnd), higherIsWorse = true),
            ComparedMetric(
                "Température batterie (°C)",
                average(Metrics.BATTERY_TEMPERATURE, beforeStart, pivot),
                average(Metrics.BATTERY_TEMPERATURE, pivot, afterEnd),
                higherIsWorse = true,
            ),
            ComparedMetric(
                "Marge thermique utilisée (%)",
                average(Metrics.THERMAL_HEADROOM, beforeStart, pivot)?.times(100),
                average(Metrics.THERMAL_HEADROOM, pivot, afterEnd)?.times(100),
                higherIsWorse = true,
            ),
            ComparedMetric(
                EPISODES_LABEL,
                perDay(db.thermalEpisodes().countBetween(beforeStart, pivot), beforeStart, pivot),
                perDay(db.thermalEpisodes().countBetween(pivot, afterEnd), pivot, afterEnd),
                higherIsWorse = true,
            ),
            ComparedMetric(
                EXITS_LABEL,
                perDay(db.journal().countBetween(JournalTypes.EXIT, beforeStart, pivot), beforeStart, pivot),
                perDay(db.journal().countBetween(JournalTypes.EXIT, pivot, afterEnd), pivot, afterEnd),
                higherIsWorse = true,
            ),
            ComparedMetric(
                "Charge CPU moyenne (%)",
                average(Metrics.CPU_LOAD, beforeStart, pivot)?.times(100),
                average(Metrics.CPU_LOAD, pivot, afterEnd)?.times(100),
                higherIsWorse = true,
            ),
        )
        // Without recorded history on both sides, the comparison would only show empty values.
        val sampled = metrics.filter { it.label != EPISODES_LABEL && it.label != EXITS_LABEL }
        if (sampled.none { it.before != null && it.after != null }) return null
        val title = if (update != null) "Avant / après : ${update.title.removePrefix("Mise à jour du système : ")}" else "7 derniers jours / 7 jours précédents"
        return Comparison(title, metrics)
    }

    private fun labelled(count: PackageCount): Pair<String, Int> {
        val pm = container.appContext.packageManager
        val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(count.packageName, 0)).toString() }
            .getOrDefault(count.packageName)
        return label to count.count
    }

    private companion object {
        const val EVENT_LIMIT = 500
        const val EPISODES_LABEL = "Épisodes de bridage par jour"
        const val EXITS_LABEL = "Plantages, ANR et arrêts mémoire par jour"
        const val DAY_MS = 24 * 3_600_000L
        const val WEEK_MS = 7 * DAY_MS
        const val MIN_AFTER_MS = 6 * 3_600_000L
    }
}
