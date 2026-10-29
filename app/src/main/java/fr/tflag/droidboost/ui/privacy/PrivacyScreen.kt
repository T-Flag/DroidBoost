package fr.tflag.droidboost.ui.privacy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.tflag.droidboost.core.privacy.CheckLevel
import fr.tflag.droidboost.core.privacy.PermissionGroup
import fr.tflag.droidboost.privacy.AppPermissionUse
import fr.tflag.droidboost.shell.ShizukuState
import fr.tflag.droidboost.ui.components.MetricRow
import fr.tflag.droidboost.ui.components.MissingAccessHint
import fr.tflag.droidboost.ui.components.SectionCard
import fr.tflag.droidboost.ui.components.StatusPill
import fr.tflag.droidboost.ui.theme.ok
import fr.tflag.droidboost.ui.theme.warning

@Composable
fun PrivacyScreen(viewModel: PrivacyViewModel, contentPadding: PaddingValues, onOpenApp: (String) -> Unit, onOpenAccess: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf<PermissionGroup?>(null) }
    val report = state.report
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Inclure les applis système", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = state.includeSystem, onCheckedChange = viewModel::setIncludeSystem)
            }
        }
        if (report == null) {
            item { CircularProgressIndicator(Modifier.padding(16.dp)) }
            return@LazyColumn
        }
        if (report.backgroundLocation.isNotEmpty()) {
            item {
                SectionCard("Localisation en arrière-plan", Icons.Outlined.LocationOn) {
                    Text(
                        "Ces applis peuvent connaître votre position même fermées.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    report.backgroundLocation.forEach { AppLine(it, onOpenApp) }
                }
            }
        }
        item {
            SectionCard("Permissions sensibles accordées", Icons.Outlined.PrivacyTip) {
                report.groups.forEach { (group, apps) ->
                    Column(Modifier.fillMaxWidth().clickable { expanded = if (expanded == group) null else group }.padding(vertical = 4.dp)) {
                        MetricRow(group.label, if (apps.isEmpty()) "aucune appli" else "${apps.size} appli(s)")
                        if (expanded == group) apps.forEach { AppLine(it, onOpenApp) }
                    }
                }
                Text(
                    "Touchez une catégorie pour voir les applis ; touchez une appli pour révoquer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionCard("Accès spéciaux", Icons.Outlined.AdminPanelSettings) {
                report.specialAccesses.forEach { access ->
                    Column(Modifier.padding(vertical = 4.dp)) {
                        MetricRow(
                            access.title,
                            access.apps?.let { if (it.isEmpty()) "aucune" else "${it.size}" } ?: "Shizuku requis",
                        )
                        Text(
                            access.apps?.takeIf { it.isNotEmpty() }?.joinToString() ?: access.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (state.shizuku !is ShizukuState.Ready) {
                    MissingAccessHint("Une partie de ces accès n'est lisible qu'avec Shizuku.", "Configurer", onOpenAccess)
                }
            }
        }
    }
}

@Composable
fun SecurityScreen(viewModel: PrivacyViewModel, contentPadding: PaddingValues) {
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
        item {
            SectionCard("Bilan de sécurité", Icons.Outlined.Shield) {
                if (state.loading && state.security.isEmpty()) CircularProgressIndicator()
                state.security.forEach { check ->
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(check.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            StatusPill(
                                check.value,
                                when (check.level) {
                                    CheckLevel.OK -> colors.ok
                                    CheckLevel.WARNING -> colors.warning
                                    CheckLevel.INFO -> colors.primary
                                },
                            )
                        }
                        check.advice?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLine(app: AppPermissionUse, onOpenApp: (String) -> Unit) {
    Text(
        app.label + if (app.isSystem) " (système)" else "",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().clickable { onOpenApp(app.packageName) }.padding(vertical = 4.dp, horizontal = 8.dp),
    )
}
