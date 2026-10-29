package fr.tflag.droidboost.ui.apps

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.tflag.droidboost.apps.AppEntry
import fr.tflag.droidboost.apps.AppRepository
import fr.tflag.droidboost.apps.PermissionEntry
import fr.tflag.droidboost.core.format.Formats
import fr.tflag.droidboost.core.shell.StandbyBucket
import fr.tflag.droidboost.shell.ShizukuState
import fr.tflag.droidboost.ui.components.AppIcon
import fr.tflag.droidboost.ui.components.EmptyState
import fr.tflag.droidboost.ui.components.MetricRow
import fr.tflag.droidboost.ui.components.MissingAccessHint
import fr.tflag.droidboost.ui.components.SectionCard
import fr.tflag.droidboost.ui.components.StatusPill
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private enum class Confirm(val title: String, val text: String, val action: String) {
    CLEAR_DATA(
        "Effacer les données ?",
        "Toutes les données de l'appli (comptes, réglages, fichiers) seront supprimées. C'est irréversible.",
        "Effacer",
    ),
    UNINSTALL_FOR_USER(
        "Retirer l'appli ?",
        "L'appli est retirée pour votre utilisateur mais reste sur l'image système : elle pourra être réinstallée " +
            "depuis cet écran. Retirer une appli système critique peut rendre le téléphone instable.",
        "Retirer",
    ),
    DISABLE(
        "Désactiver l'appli ?",
        "L'appli ne pourra plus démarrer jusqu'à sa réactivation. Désactiver une appli système critique peut " +
            "perturber le téléphone.",
        "Désactiver",
    ),
}

private val BUCKET_LABELS = mapOf(
    StandbyBucket.ACTIVE to "Active",
    StandbyBucket.WORKING_SET to "Usage régulier",
    StandbyBucket.FREQUENT to "Fréquente",
    StandbyBucket.RARE to "Rare",
    StandbyBucket.RESTRICTED to "Restreinte",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppDetailScreen(
    viewModel: AppDetailViewModel,
    repository: AppRepository,
    contentPadding: PaddingValues,
    snackbar: SnackbarHostState,
    onOpenAccess: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    var showAllPermissions by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    val app = state.app
    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (app == null) {
        EmptyState("Cette appli n'est plus installée.", Modifier.padding(contentPadding))
        return
    }
    val shizukuReady = state.shizuku is ShizukuState.Ready
    val actionsEnabled = shizukuReady && !state.busy && !state.isSelf

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
        item { Header(app, repository) }
        item {
            SectionCard("Actions", Icons.Outlined.Info) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (app.hasLauncher) {
                        FilledTonalButton(onClick = {
                            repository.launchIntent(app.packageName)?.let { context.startActivity(it) }
                        }) { Text("Ouvrir") }
                    }
                    if (app.installedForUser) {
                        OutlinedButton(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${app.packageName}".toUri()),
                            )
                        }) { Text("Infos système") }
                    }
                    if (!app.isSystem && !state.isSelf) {
                        OutlinedButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_DELETE, "package:${app.packageName}".toUri()))
                        }) { Text("Désinstaller") }
                    }
                }
            }
        }
        item {
            SectionCard(
                "Actions avancées",
                Icons.Outlined.AdminPanelSettings,
                trailing = { if (state.busy) CircularProgressIndicator(Modifier.padding(4.dp)) },
            ) {
                when {
                    state.isSelf -> MissingAccessHint("Indisponible pour DroidBoost lui-même.")
                    !shizukuReady -> MissingAccessHint("Ces actions passent par Shizuku.", "Configurer", onOpenAccess)
                }
                state.protectedReason?.let {
                    Text(
                        "Composant protégé : désactivation et retrait bloqués. $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                val destructiveEnabled = actionsEnabled && state.protectedReason == null
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (app.installedForUser) {
                        FilledTonalButton(onClick = viewModel::forceStop, enabled = actionsEnabled) { Text("Forcer l'arrêt") }
                        if (app.enabled) {
                            OutlinedButton(onClick = { confirm = Confirm.DISABLE }, enabled = destructiveEnabled) { Text("Désactiver") }
                        } else {
                            FilledTonalButton(onClick = { viewModel.setEnabled(true) }, enabled = actionsEnabled) { Text("Réactiver") }
                        }
                        if (app.suspended) {
                            FilledTonalButton(onClick = { viewModel.setSuspended(false) }, enabled = actionsEnabled) { Text("Lever la suspension") }
                        } else {
                            OutlinedButton(onClick = { viewModel.setSuspended(true) }, enabled = destructiveEnabled) { Text("Suspendre") }
                        }
                        OutlinedButton(onClick = { confirm = Confirm.CLEAR_DATA }, enabled = destructiveEnabled) { Text("Effacer les données") }
                        OutlinedButton(onClick = { confirm = Confirm.UNINSTALL_FOR_USER }, enabled = destructiveEnabled) { Text("Retirer") }
                    } else {
                        FilledTonalButton(onClick = viewModel::reinstall, enabled = actionsEnabled) { Text("Réinstaller") }
                    }
                }
                if (app.installedForUser) {
                    state.backgroundRestricted?.let { restricted ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Restreindre l'arrière-plan", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Switch(checked = restricted, onCheckedChange = viewModel::setBackgroundRestricted, enabled = actionsEnabled)
                        }
                    }
                    state.standbyBucket?.let { bucket -> BucketSelector(bucket, actionsEnabled, viewModel::setStandbyBucket) }
                }
            }
        }
        item { InfoCard(app) }
        item { StorageCard(app, onOpenAccess) }
        if (state.permissions.isNotEmpty()) {
            item {
                PermissionsCard(
                    permissions = state.permissions,
                    showAll = showAllPermissions,
                    onToggleShowAll = { showAllPermissions = !showAllPermissions },
                    canToggle = actionsEnabled,
                    onToggle = viewModel::setPermission,
                )
            }
        }
    }

    confirm?.let { pending ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(pending.title) },
            text = { Text(pending.text) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    when (pending) {
                        Confirm.CLEAR_DATA -> viewModel.clearData()
                        Confirm.UNINSTALL_FOR_USER -> viewModel.uninstallForUser()
                        Confirm.DISABLE -> viewModel.setEnabled(false)
                    }
                }) { Text(pending.action) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Annuler") } },
        )
    }

    if (state.offerDisable) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDisableOffer,
            title = { Text("Retrait refusé par Android") },
            text = {
                Text(
                    "Depuis Android 17, retirer une appli système demande le root. La désactiver produit presque " +
                        "le même effet : elle ne démarre plus et disparaît du lanceur.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissDisableOffer()
                    viewModel.setEnabled(false)
                }) { Text("Désactiver") }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissDisableOffer) { Text("Annuler") } },
        )
    }
}

@Composable
private fun Header(app: AppEntry, repository: AppRepository) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        AppIcon(repository, app.packageName, size = 56.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(app.label, style = MaterialTheme.typography.titleLarge)
            Text(app.packageName, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = colors.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(if (app.isSystem) "Système" else "Utilisateur", colors.primary)
                if (!app.installedForUser) StatusPill("Retirée", colors.error)
                if (app.installedForUser && !app.enabled) StatusPill("Désactivée", colors.outline)
                if (app.suspended) StatusPill("Suspendue", colors.tertiary)
                if (app.stopped && app.installedForUser) StatusPill("Arrêtée", colors.outline)
            }
        }
    }
}

@Composable
private fun InfoCard(app: AppEntry) {
    val dates = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.FRANCE)
    val now = System.currentTimeMillis()
    SectionCard("Informations", Icons.Outlined.Info) {
        MetricRow("Version", listOfNotNull(app.versionName, "(${app.versionCode})").joinToString(" "))
        MetricRow("SDK cible / minimal", "${app.targetSdk} / ${app.minSdk}")
        MetricRow("Installée par", app.installer ?: if (app.isSystem) "Image système" else "Inconnu")
        if (app.isUpdatedSystem) MetricRow("Appli système", "Mise à jour depuis l'image d'origine")
        MetricRow("Installée le", dates.format(Date(app.firstInstallTime)))
        MetricRow("Mise à jour le", dates.format(Date(app.lastUpdateTime)))
        MetricRow("Dernière utilisation", app.lastUsed?.let { Formats.ago(now - it) } ?: "Pas depuis 30 jours")
        app.foregroundMs30d?.takeIf { it > 0 }?.let { MetricRow("Premier plan (30 jours)", Formats.duration(it)) }
    }
}

@Composable
private fun StorageCard(app: AppEntry, onOpenAccess: () -> Unit) {
    SectionCard("Stockage", Icons.Outlined.SdStorage) {
        val size = app.size
        when {
            size != null -> {
                MetricRow("Total", Formats.bytes(size.totalBytes))
                MetricRow("Appli (APK, bibliothèques)", Formats.bytes(size.appBytes))
                MetricRow("Données (cache inclus)", Formats.bytes(size.dataBytes))
                MetricRow("Cache", Formats.bytes(size.cacheBytes))
                Text(
                    "Android ne permet pas de vider le cache d'une seule appli sans root : utilisez le vidage global " +
                        "dans l'onglet Stockage, ou les Infos système.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            app.installedForUser -> MissingAccessHint("Tailles : accès aux données d'usage requis.", "Activer", onOpenAccess)
            else -> MissingAccessHint("Appli retirée : seule l'APK d'origine reste sur l'image système.")
        }
    }
}

@Composable
private fun PermissionsCard(
    permissions: List<PermissionEntry>,
    showAll: Boolean,
    onToggleShowAll: () -> Unit,
    canToggle: Boolean,
    onToggle: (String, Boolean) -> Unit,
) {
    val dangerous = permissions.filter { it.dangerous }
    val others = permissions.filterNot { it.dangerous }
    SectionCard("Permissions", Icons.Outlined.Lock) {
        Text(
            "${dangerous.count { it.granted }} sur ${dangerous.size} permissions sensibles accordées",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        dangerous.forEach { permission ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(permission.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        permission.name.removePrefix("android.permission."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = permission.granted, onCheckedChange = { onToggle(permission.name, it) }, enabled = canToggle)
            }
        }
        if (others.isNotEmpty()) {
            TextButton(onClick = onToggleShowAll) {
                Text(if (showAll) "Masquer les autres permissions" else "Afficher les ${others.size} autres permissions")
            }
            if (showAll) {
                others.forEach { permission ->
                    MetricRow(permission.name.substringAfterLast('.'), if (permission.granted) "Accordée" else "Non accordée")
                }
            }
        }
    }
}

@Composable
private fun BucketSelector(current: StandbyBucket, enabled: Boolean, onSelect: (StandbyBucket) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Bucket de veille", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled) { Text(BUCKET_LABELS.getValue(current)) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                StandbyBucket.entries.forEach { bucket ->
                    DropdownMenuItem(
                        text = { Text(BUCKET_LABELS.getValue(bucket)) },
                        onClick = {
                            expanded = false
                            onSelect(bucket)
                        },
                    )
                }
            }
        }
    }
}
