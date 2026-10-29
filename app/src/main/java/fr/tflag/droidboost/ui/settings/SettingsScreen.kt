package fr.tflag.droidboost.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.tflag.droidboost.core.format.Formats
import fr.tflag.droidboost.ui.components.MetricRow
import fr.tflag.droidboost.ui.components.SectionCard

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, contentPadding: PaddingValues) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config = state.config
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
        item {
            SectionCard("Historique", Icons.Outlined.Storage) {
                Choice(
                    "Enregistrer toutes les",
                    listOf(30 to "30 s", 60 to "1 min", 300 to "5 min"),
                    config.recordIntervalSec,
                    viewModel::setRecordInterval,
                )
                Choice(
                    "Conserver",
                    listOf(7 to "7 jours", 30 to "30 jours", 90 to "90 jours"),
                    config.retentionDays,
                    viewModel::setRetention,
                )
                state.stats?.let { stats ->
                    MetricRow("Mesures enregistrées", Formats.number(stats.sampleCount))
                    stats.oldestTimestamp?.let { MetricRow("Depuis", Formats.ago(System.currentTimeMillis() - it)) }
                }
                Hint("Rien ne quitte le téléphone : l'appli n'a pas la permission Internet.")
            }
        }
        item {
            SectionCard("Alertes", Icons.Outlined.NotificationsActive) {
                Choice(
                    "Prévenir quand la charge atteint",
                    listOf(0 to "Jamais", 80 to "80 %", 85 to "85 %", 90 to "90 %"),
                    config.chargeAlertPercent,
                    viewModel::setChargeAlert,
                )
                Choice(
                    "Batterie trop chaude à partir de",
                    listOf(0 to "Jamais", 40 to "40 °C", 42 to "42 °C", 45 to "45 °C"),
                    config.batteryTempAlertC,
                    viewModel::setBatteryTempAlert,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Bridage thermique sévère", style = MaterialTheme.typography.bodyMedium)
                        Hint("Avec le nom de l'appli au premier plan.")
                    }
                    Switch(checked = config.thermalAlert, onCheckedChange = viewModel::setThermalAlert)
                }
                Hint("Les alertes fonctionnent quand la notification permanente est active.")
            }
        }
    }
}

@Composable
private fun Choice(label: String, options: List<Pair<Int, String>>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, text) ->
                FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(text) })
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
