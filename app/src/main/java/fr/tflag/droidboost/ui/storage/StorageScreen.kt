package fr.tflag.droidboost.ui.storage

import android.content.Intent
import android.provider.Settings
import android.os.storage.StorageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.tflag.droidboost.apps.AppEntry
import fr.tflag.droidboost.apps.AppRepository
import fr.tflag.droidboost.core.format.Formats
import fr.tflag.droidboost.shell.ShizukuState
import fr.tflag.droidboost.ui.components.AppIcon
import fr.tflag.droidboost.ui.components.MetricRow
import fr.tflag.droidboost.ui.components.MissingAccessHint
import fr.tflag.droidboost.ui.components.SectionCard
import fr.tflag.droidboost.ui.components.TwoLineText

private data class Segment(val label: String, val bytes: Long, val color: Color)

private const val TOP_APPS = 40

@Composable
fun StorageScreen(
    viewModel: StorageViewModel,
    repository: AppRepository,
    contentPadding: PaddingValues,
    snackbar: SnackbarHostState,
    onOpenApp: (String) -> Unit,
    onOpenAccess: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var byCache by remember { mutableStateOf(false) }
    val systemCacheDialog = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onSystemCacheDialogClosed()
    }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

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
        item { BreakdownCard(state, onOpenAccess) }
        item {
            SectionCard("Caches", Icons.Outlined.CleaningServices) {
                if (state.hasUsageAccess) MetricRow("Caches de toutes les applis", Formats.bytes(state.cacheBytes))
                Text(
                    "Android vide déjà les caches quand l'espace manque ; un vidage manuel ne fait gagner que temporairement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    state.shizuku is ShizukuState.Ready -> FilledTonalButton(onClick = viewModel::trimAllCaches, enabled = !state.trimming) {
                        if (state.trimming) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Vider tous les caches")
                    }
                    state.hasAllFilesAccess -> FilledTonalButton(onClick = {
                        systemCacheDialog.launch(Intent(StorageManager.ACTION_CLEAR_APP_CACHE))
                    }) { Text("Vider les caches (dialogue système)") }
                    else -> MissingAccessHint(
                        "Le vidage demande Shizuku, ou l'accès à tous les fichiers pour le dialogue système.",
                        "Configurer",
                        onOpenAccess,
                    )
                }
            }
        }
        if (state.growth.isNotEmpty()) {
            item {
                SectionCard("Plus fortes croissances (7 jours)", Icons.Outlined.TrendingUp) {
                    state.growth.forEach { (app, delta) ->
                        MetricRow(app.label, "+${Formats.bytes(delta)}", Modifier.clickable { onOpenApp(app.packageName) })
                    }
                    Text(
                        "Données et cache, mesurés toutes les 6 h quand la surveillance continue est active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (state.archiveCandidates.isNotEmpty()) {
            item {
                SectionCard("Applis inutilisées depuis 30 jours", Icons.Outlined.Archive) {
                    Text(
                        "Archiver retire l'APK et le cache mais garde vos données ; l'icône reste et l'appli se réinstalle d'un toucher.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.archiveCandidates.forEach { app ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TwoLineText(app.label, app.size?.let { Formats.bytes(it.totalBytes) } ?: "", Modifier.weight(1f))
                            TextButton(onClick = {
                                if (!viewModel.archive(app)) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${app.packageName}".toUri()),
                                    )
                                }
                            }) { Text("Archiver") }
                        }
                    }
                }
            }
        }
        if (state.apps.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Applis les plus lourdes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    FilterChip(selected = !byCache, onClick = { byCache = false }, label = { Text("Taille") })
                    FilterChip(selected = byCache, onClick = { byCache = true }, label = { Text("Cache") })
                }
            }
            val ranked = state.apps
                .sortedByDescending { if (byCache) it.size?.cacheBytes ?: 0L else it.size?.totalBytes ?: 0L }
                .take(TOP_APPS)
            items(ranked, key = { it.packageName }) { app -> RankedAppRow(app, byCache, repository) { onOpenApp(app.packageName) } }
        }
    }
}

@Composable
private fun BreakdownCard(state: StorageState, onOpenAccess: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    SectionCard("Répartition", Icons.Outlined.PieChart) {
        val totals = state.totals
        if (state.loading && totals == null) {
            CircularProgressIndicator()
            return@SectionCard
        }
        if (totals == null) {
            Text("Stockage illisible.")
            return@SectionCard
        }
        MetricRow("Utilisé", "${Formats.bytes(totals.usedBytes)} sur ${Formats.bytes(totals.totalBytes)}")
        MetricRow("Libre", Formats.bytes(totals.freeBytes))
        val media = state.media
        val segments = buildList {
            if (state.appsBytes > 0) add(Segment("Applis et leurs données", state.appsBytes, colors.primary))
            if (media != null) {
                add(Segment("Images", media.imageBytes, colors.secondary))
                add(Segment("Vidéos", media.videoBytes, colors.tertiary))
                add(Segment("Audio", media.audioBytes, colors.error))
            }
        }
        val known = segments.sumOf { it.bytes }
        val other = (totals.usedBytes - known).coerceAtLeast(0)
        val all = segments + Segment(if (segments.isEmpty()) "Utilisé" else "Système et autres", other, colors.outline)
        Row(
            Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(colors.surfaceContainerHighest),
        ) {
            all.filter { it.bytes > 0 }.forEach { segment ->
                Box(Modifier.weight(segment.bytes.toFloat()).fillMaxHeight().background(segment.color))
            }
            if (totals.freeBytes > 0) Spacer(Modifier.weight(totals.freeBytes.toFloat()))
        }
        all.forEach { segment ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(segment.color))
                Spacer(Modifier.width(8.dp))
                MetricRow(segment.label, Formats.bytes(segment.bytes))
            }
        }
        if (!state.hasUsageAccess) {
            MissingAccessHint("Le détail par catégorie et par appli demande l'accès aux données d'usage.", "Activer", onOpenAccess)
        }
    }
}

@Composable
private fun RankedAppRow(app: AppEntry, byCache: Boolean, repository: AppRepository, onClick: () -> Unit) {
    val size = app.size ?: return
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(repository, app.packageName, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        TwoLineText(
            app.label,
            "Appli ${Formats.bytes(size.appBytes)} · données ${Formats.bytes(size.dataBytes)} · cache ${Formats.bytes(size.cacheBytes)}",
            Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(Formats.bytes(if (byCache) size.cacheBytes else size.totalBytes), style = MaterialTheme.typography.labelLarge)
    }
}
