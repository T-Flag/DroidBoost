package fr.tflag.droidboost.ui.thermal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.tflag.droidboost.core.format.Formats
import fr.tflag.droidboost.core.model.ThermalStatus
import fr.tflag.droidboost.data.ThermalEpisodeEntity
import fr.tflag.droidboost.shell.ShizukuState
import fr.tflag.droidboost.ui.components.LevelBar
import fr.tflag.droidboost.ui.components.LineChart
import fr.tflag.droidboost.ui.components.MetricRow
import fr.tflag.droidboost.ui.components.MissingAccessHint
import fr.tflag.droidboost.ui.components.SectionCard
import fr.tflag.droidboost.ui.components.StatusPill
import fr.tflag.droidboost.ui.components.WindowSelector
import fr.tflag.droidboost.ui.theme.critical
import fr.tflag.droidboost.ui.theme.ok
import fr.tflag.droidboost.ui.theme.warning
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val EPISODE_DATE = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.FRANCE)

/** Sensor types worth a chart of their own, in display order. */
private const val MAX_LIVE_SENSORS = 8

private val CHARTED_TYPES = listOf("Surface", "CPU", "GPU", "Modem", "SoC", "TPU", "NPU")

@Composable
fun ThermalScreen(viewModel: ThermalViewModel, contentPadding: PaddingValues, onOpenAccess: () -> Unit, onOpenDashboard: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.live?.let { live ->
            item {
                val statusColor = when {
                    live.status.level >= ThermalStatus.SEVERE.level -> colors.critical
                    live.status.level >= ThermalStatus.LIGHT.level -> colors.warning
                    else -> colors.ok
                }
                SectionCard("Maintenant", Icons.Outlined.Thermostat, trailing = { StatusPill(live.status.label, statusColor) }) {
                    live.headroom?.let {
                        MetricRow("Marge avant bridage sévère", Formats.percent(1f - it.coerceIn(0f, 1f)))
                        LevelBar(it)
                    }
                    if (live.headroomThresholds.isNotEmpty()) {
                        Text("Seuils de la marge (Android 15+)", style = MaterialTheme.typography.labelLarge)
                        live.headroomThresholds.toSortedMap(compareBy { it.level }).forEach { (status, value) ->
                            MetricRow(status.label, Formats.percent(value))
                        }
                    }
                    if (live.hal == null) {
                        MissingAccessHint("Températures par capteur : permission DUMP ou Shizuku requise.", "Configurer", onOpenAccess)
                    } else {
                        live.hal.temperatures.filter { fr.tflag.droidboost.core.model.TemperatureTypes.isTemperature(it.type) }
                            .sortedByDescending { it.celsius }
                            .take(MAX_LIVE_SENSORS)
                            .forEach { MetricRow(fr.tflag.droidboost.core.model.TemperatureTypes.displayName(it), Formats.celsius(it.celsius)) }
                    }
                }
            }
        }
        item {
            SectionCard("Historique", Icons.Outlined.ShowChart) {
                if (!state.monitorEnabled) {
                    MissingAccessHint("L'historique s'enregistre quand la notification permanente est active.", "Activer", onOpenDashboard)
                }
                WindowSelector(state.window, viewModel::setWindow)
                Text("Marge thermique (100 % = bridage sévère)", style = MaterialTheme.typography.labelLarge)
                LineChart(state.history.headroom, state.window, { Formats.percent(it.toFloat()) }, fixedMin = 0.0, reference = 1.0)
                CHARTED_TYPES.filter { it in state.history.halByType }.forEach { type ->
                    Text("Capteur $type (le plus chaud)", style = MaterialTheme.typography.labelLarge)
                    LineChart(
                        state.history.halByType.getValue(type),
                        state.window,
                        { Formats.celsius(it.toFloat()) },
                        color = colors.error,
                        reference = if (type == "Surface") 37.0 else null,
                    )
                }
                Text("Batterie", style = MaterialTheme.typography.labelLarge)
                LineChart(state.history.batteryTemperature, state.window, { Formats.celsius(it.toFloat()) }, color = colors.tertiary)
                if (state.history.cpuLoad.isNotEmpty()) {
                    Text("Charge CPU", style = MaterialTheme.typography.labelLarge)
                    LineChart(state.history.cpuLoad, state.window, { Formats.percent(it.toFloat()) }, fixedMin = 0.0, fixedMax = 1.0)
                }
            }
        }
        item {
            SectionCard("Épisodes de bridage", Icons.Outlined.LocalFireDepartment) {
                Text(
                    if (state.episodes.isEmpty()) "Aucun épisode enregistré." else "Périodes où Android a ralenti le processeur pour le refroidir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        items(state.episodes, key = { it.id }) { EpisodeRow(it) }
    }
}

@Composable
private fun EpisodeRow(episode: ThermalEpisodeEntity) {
    val context = LocalContext.current
    val start = Instant.ofEpochMilli(episode.startTs).atZone(ZoneId.systemDefault())
    val app = episode.foregroundPackage?.let { pkg ->
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
    }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(EPISODE_DATE.format(start).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
            StatusPill(ThermalStatus.of(episode.maxStatus).label, if (episode.maxStatus >= ThermalStatus.SEVERE.level) MaterialTheme.colorScheme.critical else MaterialTheme.colorScheme.warning)
        }
        val details = listOfNotNull(
            Formats.duration(episode.lastTs - episode.startTs) + if (episode.endTs == null) " (en cours)" else "",
            app?.let { "appli : $it" },
            if (episode.charging) "en charge" else null,
            episode.maxHeadroom?.let { "marge max ${Formats.percent(it)}" },
        )
        Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}
