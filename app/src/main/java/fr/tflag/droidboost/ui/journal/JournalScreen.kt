package fr.tflag.droidboost.ui.journal

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.tflag.droidboost.core.journal.ComparedMetric
import fr.tflag.droidboost.core.journal.JournalTypes
import fr.tflag.droidboost.data.JournalEventEntity
import fr.tflag.droidboost.ui.components.MetricRow
import fr.tflag.droidboost.ui.components.MissingAccessHint
import fr.tflag.droidboost.ui.components.SectionCard
import fr.tflag.droidboost.ui.theme.critical
import fr.tflag.droidboost.ui.theme.ok
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE)
private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.FRANCE)
private val NUMBER = DecimalFormat("0.##", DecimalFormatSymbols(Locale.FRANCE))
private val EXPORT_NAME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")

private sealed interface JournalRow {
    data class Day(val date: LocalDate) : JournalRow
    data class Event(val event: JournalEventEntity) : JournalRow
}

@Composable
fun JournalScreen(viewModel: JournalViewModel, contentPadding: PaddingValues, snackbar: SnackbarHostState, onOpenAccess: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::export)
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }
    val zone = ZoneId.systemDefault()
    val rows = buildList {
        var day: LocalDate? = null
        state.events.forEach { event ->
            val date = Instant.ofEpochMilli(event.ts).atZone(zone).toLocalDate()
            if (date != day) {
                add(JournalRow.Day(date))
                day = date
            }
            add(JournalRow.Event(event))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.comparison?.let { comparison -> item { ComparisonCard(comparison) } }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JournalFilter.entries.forEach { filter ->
                    FilterChip(selected = state.filter == filter, onClick = { viewModel.setFilter(filter) }, label = { Text(filter.label) })
                }
            }
        }
        if (state.filter == JournalFilter.STABILITY || state.filter == JournalFilter.ALL) {
            item {
                if (!state.hasDump) {
                    MissingAccessHint(
                        "Les plantages, ANR et arrêts mémoire des autres applis demandent la permission DUMP.",
                        "Accorder",
                        onOpenAccess,
                    )
                } else if (state.filter == JournalFilter.STABILITY && state.unstableApps.isNotEmpty()) {
                    SectionCard("Applis les moins stables (7 jours)", Icons.Outlined.BugReport) {
                        state.unstableApps.forEach { (label, count) -> MetricRow(label, "$count incident(s)") }
                    }
                }
            }
        }
        if (rows.isEmpty()) {
            item {
                Text(
                    "Aucun événement pour ce filtre.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
        items(rows, key = { row ->
            when (row) {
                is JournalRow.Day -> "day-${row.date}"
                is JournalRow.Event -> "event-${row.event.id}"
            }
        }) { row ->
            when (row) {
                is JournalRow.Day -> Text(
                    dayLabel(row.date),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp),
                )
                is JournalRow.Event -> EventRow(row.event, zone)
            }
        }
        item {
            OutlinedButton(
                onClick = { exportLauncher.launch("droidboost-${EXPORT_NAME.format(java.time.LocalDateTime.now())}.zip") },
                enabled = !state.exporting,
                modifier = Modifier.padding(top = 16.dp),
            ) { Text(if (state.exporting) "Export en cours…" else "Exporter tout l'historique (ZIP de CSV)") }
        }
    }
}

@Composable
private fun ComparisonCard(comparison: Comparison) {
    SectionCard(comparison.title, Icons.Outlined.CompareArrows) {
        comparison.metrics.filter { it.before != null || it.after != null }.forEach { metric -> ComparedRow(metric) }
        Text(
            "En rouge : dégradation d'au moins 10 %. Il faut quelques jours de données de chaque côté pour que la comparaison soit fiable.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ComparedRow(metric: ComparedMetric) {
    val colors = MaterialTheme.colorScheme
    val change = metric.relativeChange
    MetricRow(
        metric.label,
        "${metric.before?.let { NUMBER.format(it) } ?: "—"} → ${metric.after?.let { NUMBER.format(it) } ?: "—"}" +
            (change?.let { " (${if (it >= 0) "+" else ""}${NUMBER.format(it * 100)} %)" } ?: ""),
        valueColor = when {
            metric.isRegression -> colors.critical
            change != null && (if (metric.higherIsWorse) change <= -0.10 else change >= 0.10) -> colors.ok
            else -> androidx.compose.ui.graphics.Color.Unspecified
        },
    )
}

@Composable
private fun EventRow(event: JournalEventEntity, zone: ZoneId) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Icon(iconFor(event), contentDescription = null, tint = colorFor(event), modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.bodyMedium)
            event.details?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            TIME_FORMAT.format(Instant.ofEpochMilli(event.ts).atZone(zone)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun iconFor(event: JournalEventEntity): ImageVector = when (event.type) {
    JournalTypes.APP -> Icons.Outlined.Apps
    JournalTypes.PERMISSION -> Icons.Outlined.Lock
    JournalTypes.EXIT -> Icons.Outlined.BugReport
    JournalTypes.ACTION -> Icons.Outlined.TouchApp
    JournalTypes.SHIZUKU -> Icons.Outlined.AdminPanelSettings
    JournalTypes.ANOMALY -> Icons.Outlined.ReportProblem
    else -> Icons.Outlined.PhoneAndroid
}

@Composable
private fun colorFor(event: JournalEventEntity) = when (event.type) {
    JournalTypes.EXIT, JournalTypes.ANOMALY -> MaterialTheme.colorScheme.critical
    JournalTypes.PERMISSION -> MaterialTheme.colorScheme.tertiary
    JournalTypes.SYSTEM -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.primary
}

private fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Aujourd'hui"
        today.minusDays(1) -> "Hier"
        else -> DAY_FORMAT.format(date).replaceFirstChar { it.uppercase() }
    }
}
