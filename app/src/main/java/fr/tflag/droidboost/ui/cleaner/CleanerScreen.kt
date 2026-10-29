package fr.tflag.droidboost.ui.cleaner

import android.os.Environment
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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import fr.tflag.droidboost.core.format.Formats
import fr.tflag.droidboost.shell.ShizukuState
import fr.tflag.droidboost.ui.components.MetricRow
import fr.tflag.droidboost.ui.components.MissingAccessHint
import fr.tflag.droidboost.ui.components.SectionCard
import fr.tflag.droidboost.ui.components.TwoLineText

private const val SHOWN_FILES = 30
private const val SHOWN_GROUPS = 20

@Composable
fun CleanerScreen(viewModel: CleanerViewModel, contentPadding: PaddingValues, snackbar: SnackbarHostState, onOpenAccess: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingConfirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    val root = Environment.getExternalStorageDirectory().absolutePath
    val result = state.result

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
            SectionCard("Analyse du stockage partagé", Icons.Outlined.Search) {
                Text(
                    "Gros fichiers, doublons, dossiers vides et restes d'applis désinstallées. Rien n'est supprimé sans votre confirmation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!state.hasAllFiles) MissingAccessHint("L'analyse des fichiers demande l'accès à tous les fichiers.", "Activer", onOpenAccess)
                if (state.shizuku !is ShizukuState.Ready) MissingAccessHint("Les restes dans Android/data demandent Shizuku.", "Configurer", onOpenAccess)
                if (state.scanning) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("${Formats.number(state.scannedFiles.toLong())} fichiers parcourus…", style = MaterialTheme.typography.bodySmall)
                } else {
                    FilledTonalButton(onClick = viewModel::scan) { Text(if (result == null) "Lancer l'analyse" else "Relancer l'analyse") }
                }
                result?.let {
                    MetricRow("Fichiers", Formats.number(it.fileCount.toLong()))
                    MetricRow("Volume total", Formats.bytes(it.totalBytes))
                    MetricRow("Récupérable avec les doublons", Formats.bytes(it.duplicates.sumOf { group -> group.wastedBytes }))
                }
            }
        }
        if (result != null && result.duplicates.isNotEmpty()) {
            item { Header("Doublons (${result.duplicates.size} groupes)", Icons.Outlined.ContentCopy) }
            items(result.duplicates.take(SHOWN_GROUPS), key = { "dup-" + it.files.first().path }) { group ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    group.files.forEachIndexed { i, file ->
                        Text(
                            (if (i == 0) "Gardé : " else "Copie : ") + file.path.removePrefix(root),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (i == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = {
                        pendingConfirm = "Supprimer ${group.files.size - 1} copie(s) (${Formats.bytes(group.wastedBytes)}) ?" to { viewModel.deleteDuplicates(group) }
                    }, enabled = !state.busy) { Text("Supprimer les copies · ${Formats.bytes(group.wastedBytes)}") }
                }
            }
        }
        if (result != null && result.largest.isNotEmpty()) {
            item { Header("Plus gros fichiers", Icons.Outlined.Inventory2) }
            items(result.largest.take(SHOWN_FILES), key = { "big-" + it.path }) { file ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TwoLineText(file.path.substringAfterLast('/'), file.path.removePrefix(root).substringBeforeLast('/'), Modifier.weight(1f))
                    Text(Formats.bytes(file.sizeBytes), style = MaterialTheme.typography.labelLarge)
                    IconButton(onClick = {
                        pendingConfirm = "Supprimer définitivement ${file.path.substringAfterLast('/')} ?" to { viewModel.deleteFile(file) }
                    }, enabled = !state.busy) { Icon(Icons.Outlined.Delete, contentDescription = "Supprimer") }
                }
            }
        }
        if (result != null && result.emptyFolders.isNotEmpty()) {
            item {
                SectionCard("Dossiers vides (${result.emptyFolders.size})", Icons.Outlined.FolderOff) {
                    Text(
                        result.emptyFolders.take(8).joinToString("\n") { it.removePrefix(root) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = viewModel::deleteEmptyFolders, enabled = !state.busy) { Text("Supprimer les dossiers vides") }
                }
            }
        }
        state.leftovers?.let { leftovers ->
            item { Header("Restes d'applis désinstallées (${leftovers.size})", Icons.Outlined.FolderOff) }
            items(leftovers, key = { "left-" + it.area.name + it.packageName }) { leftover ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TwoLineText(leftover.packageName, leftover.area.label, Modifier.weight(1f))
                    Text(Formats.bytes(leftover.sizeBytes), style = MaterialTheme.typography.labelLarge)
                    IconButton(onClick = {
                        pendingConfirm = "Supprimer ${leftover.area.label}/${leftover.packageName} ?" to { viewModel.deleteLeftover(leftover) }
                    }, enabled = !state.busy) { Icon(Icons.Outlined.Delete, contentDescription = "Supprimer") }
                }
            }
        }
    }

    pendingConfirm?.let { (question, action) ->
        AlertDialog(
            onDismissRequest = { pendingConfirm = null },
            title = { Text(question) },
            text = { Text("La suppression est définitive : il n'y a pas de corbeille.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingConfirm = null
                    action()
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { pendingConfirm = null }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun Header(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp))
    }
}
