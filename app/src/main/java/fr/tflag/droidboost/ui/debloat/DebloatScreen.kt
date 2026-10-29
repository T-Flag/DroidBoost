package fr.tflag.droidboost.ui.debloat

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.tflag.droidboost.apps.AppRepository
import fr.tflag.droidboost.core.maintenance.DebloatRisk
import fr.tflag.droidboost.shell.ShizukuState
import fr.tflag.droidboost.ui.components.AppIcon
import fr.tflag.droidboost.ui.components.MissingAccessHint
import fr.tflag.droidboost.ui.components.SectionCard
import fr.tflag.droidboost.ui.components.StatusPill
import fr.tflag.droidboost.ui.theme.critical
import fr.tflag.droidboost.ui.theme.ok
import fr.tflag.droidboost.ui.theme.warning

@Composable
fun DebloatScreen(
    viewModel: DebloatViewModel,
    repository: AppRepository,
    contentPadding: PaddingValues,
    snackbar: SnackbarHostState,
    onOpenApp: (String) -> Unit,
    onOpenAccess: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf<DebloatItem?>(null) }
    val ready = state.shizuku is ShizukuState.Ready && !state.busy
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

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
        item {
            SectionCard("Debloat Pixel", Icons.Outlined.DeleteSweep) {
                Text(
                    "Applis préinstallées classées selon le risque (listes UAD-ng). DroidBoost désactive au lieu de " +
                        "désinstaller : c'est réversible à tout moment et cela fonctionne encore sous Android 17. " +
                        "Les paquets protégés ne peuvent pas être touchés.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.shizuku !is ShizukuState.Ready) MissingAccessHint("La désactivation passe par Shizuku.", "Configurer", onOpenAccess)
            }
        }
        if (state.reverted.isNotEmpty()) {
            item {
                SectionCard("Réactivées depuis votre debloat", Icons.Outlined.Restore) {
                    Text(
                        "${state.reverted.size} appli(s) désactivée(s) depuis DroidBoost sont de nouveau actives, " +
                            "probablement après une mise à jour : ${state.reverted.joinToString { it.entry.label }}.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    FilledTonalButton(onClick = viewModel::redoReverted, enabled = ready) { Text("Les désactiver à nouveau") }
                }
            }
        }
        if (state.loading) item { CircularProgressIndicator(Modifier.padding(16.dp)) }
        DebloatRisk.entries.forEach { risk ->
            val group = state.items.filter { it.entry.risk == risk }
            if (group.isEmpty()) return@forEach
            item {
                Text(
                    "${risk.label} (${group.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            items(group, key = { it.entry.packageName }) { item ->
                DebloatRow(item, repository, ready, onOpenApp, onDisable = { confirm = item }, onEnable = { viewModel.enable(item) })
            }
        }
    }

    confirm?.let { item ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Désactiver ${item.entry.label} ?") },
            text = { Text(item.entry.note + "\n\nRéactivable à tout moment depuis cet écran.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    viewModel.disable(item)
                }) { Text("Désactiver") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun DebloatRow(
    item: DebloatItem,
    repository: AppRepository,
    enabled: Boolean,
    onOpenApp: (String) -> Unit,
    onDisable: () -> Unit,
    onEnable: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().clickable { onOpenApp(item.app.packageName) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(repository, item.app.packageName, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.entry.label, style = MaterialTheme.typography.bodyMedium)
            Text(item.entry.note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        when {
            item.entry.risk == DebloatRisk.PROTECTED -> StatusPill("Protégé", colors.critical)
            item.active -> OutlinedButton(onClick = onDisable, enabled = enabled) { Text("Désactiver") }
            else -> Column(horizontalAlignment = Alignment.End) {
                StatusPill(if (item.app.installedForUser) "Désactivée" else "Retirée", if (item.entry.risk == DebloatRisk.SAFE) colors.ok else colors.warning)
                TextButton(onClick = onEnable, enabled = enabled) { Text("Réactiver") }
            }
        }
    }
}
