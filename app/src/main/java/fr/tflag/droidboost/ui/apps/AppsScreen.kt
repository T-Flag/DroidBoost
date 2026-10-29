package fr.tflag.droidboost.ui.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.tflag.droidboost.apps.AppEntry
import fr.tflag.droidboost.apps.AppRepository
import fr.tflag.droidboost.core.format.Formats
import fr.tflag.droidboost.ui.components.AppIcon
import fr.tflag.droidboost.ui.components.EmptyState
import fr.tflag.droidboost.ui.components.StatusPill
import fr.tflag.droidboost.ui.components.TwoLineText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    viewModel: AppsViewModel,
    repository: AppRepository,
    contentPadding: PaddingValues,
    onOpenApp: (String) -> Unit,
    onOpenAccess: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sortMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Nom ou paquet") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setQuery("") }) { Icon(Icons.Outlined.Clear, contentDescription = "Effacer") }
                }
            },
            singleLine = true,
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { viewModel.setFilter(filter) },
                    label = { Text(filter.label) },
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (state.loading) "Chargement…" else "${state.visible.size} sur ${state.total} · tri : ${state.sort.label.lowercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Trier") }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    AppSort.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(sort.label) },
                            onClick = {
                                viewModel.setSort(sort)
                                sortMenu = false
                            },
                        )
                    }
                }
            }
            IconButton(onClick = viewModel::refresh) { Icon(Icons.Outlined.Refresh, contentDescription = "Actualiser") }
        }
        if (!state.hasUsageAccess && !state.loading) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Tailles et dernière utilisation : accès aux données d'usage requis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onOpenAccess) { Text("Activer") }
            }
        }
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.visible.isEmpty() -> EmptyState("Aucune appli ne correspond.")
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 8.dp),
            ) {
                items(state.visible, key = { it.packageName }) { app ->
                    AppRow(app, state.sort, repository) { onOpenApp(app.packageName) }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppEntry, sort: AppSort, repository: AppRepository, onClick: () -> Unit) {
    val now = System.currentTimeMillis()
    val secondary = when (sort) {
        AppSort.SIZE, AppSort.CACHE -> app.size?.let { "${Formats.bytes(it.totalBytes)} · cache ${Formats.bytes(it.cacheBytes)}" }
        AppSort.LAST_USED -> app.lastUsed?.let { "Utilisée ${Formats.ago(now - it)}" } ?: "Pas utilisée depuis 30 jours"
        AppSort.INSTALLED -> "Installée ${Formats.ago(now - app.firstInstallTime)}"
        AppSort.UPDATED -> "Mise à jour ${Formats.ago(now - app.lastUpdateTime)}"
        AppSort.NAME -> null
    } ?: app.packageName
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(repository, app.packageName)
        Spacer(Modifier.width(16.dp))
        TwoLineText(app.label, secondary, Modifier.weight(1f))
        when {
            app.archived -> StatusPill("Archivée", colors.tertiary)
            !app.installedForUser -> StatusPill("Retirée", colors.error)
            app.suspended -> StatusPill("Suspendue", colors.tertiary)
            !app.enabled -> StatusPill("Désactivée", colors.outline)
        }
    }
}
