package fr.tflag.droidboost.ui.network

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.tflag.droidboost.core.format.Formats
import fr.tflag.droidboost.network.AppTraffic
import fr.tflag.droidboost.ui.components.MetricRow
import fr.tflag.droidboost.ui.components.MissingAccessHint
import fr.tflag.droidboost.ui.components.SectionCard
import fr.tflag.droidboost.ui.components.StackedBarChart
import fr.tflag.droidboost.ui.components.TwoLineText

@Composable
fun NetworkScreen(viewModel: NetworkViewModel, contentPadding: PaddingValues, onOpenApp: (String) -> Unit, onOpenAccess: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
            SectionCard("Débit en direct", Icons.Outlined.NetworkCheck) {
                state.speed?.let {
                    MetricRow("Réception", Formats.bytesPerSecond(it.rxBytesPerSecond))
                    MetricRow("Envoi", Formats.bytesPerSecond(it.txBytesPerSecond))
                } ?: Text("Mesure en cours…", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Le débit par appli en direct demanderait un VPN local, qui occuperait l'unique emplacement VPN du téléphone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionCard("Consommation", Icons.Outlined.BarChart) {
                if (!state.hasUsageAccess && !state.loading) {
                    MissingAccessHint("La consommation par appli demande l'accès aux données d'usage.", "Activer", onOpenAccess)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrafficPeriod.entries.forEach { period ->
                        FilterChip(selected = state.period == period, onClick = { viewModel.load(period) }, label = { Text(period.label) })
                    }
                }
                MetricRow("Données mobiles", Formats.bytes(state.mobileBytes))
                MetricRow("Wi-Fi", Formats.bytes(state.wifiBytes))
                Text("30 derniers jours (mobile en bas, Wi-Fi au-dessus)", style = MaterialTheme.typography.labelLarge)
                StackedBarChart(state.days.map { it.mobileBytes to it.wifiBytes })
                state.days.lastOrNull()?.let {
                    MetricRow("Aujourd'hui", "mobile ${Formats.bytes(it.mobileBytes)} · Wi-Fi ${Formats.bytes(it.wifiBytes)}")
                }
            }
        }
        if (state.loading) {
            item { CircularProgressIndicator(Modifier.padding(16.dp)) }
        }
        items(state.apps.take(MAX_APPS), key = { it.uid }) { app -> TrafficRow(app, onOpenApp) }
    }
}

@Composable
private fun TrafficRow(app: AppTraffic, onOpenApp: (String) -> Unit) {
    val pkg = app.packageName
    Row(
        Modifier.fillMaxWidth().then(if (pkg != null) Modifier.clickable { onOpenApp(pkg) } else Modifier).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TwoLineText(app.label, "mobile ${Formats.bytes(app.mobileBytes)} · Wi-Fi ${Formats.bytes(app.wifiBytes)}", Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(Formats.bytes(app.totalBytes), style = MaterialTheme.typography.labelLarge)
    }
}

private const val MAX_APPS = 60
