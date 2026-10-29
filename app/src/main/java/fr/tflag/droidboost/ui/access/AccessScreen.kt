package fr.tflag.droidboost.ui.access

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.tflag.droidboost.access.DevGrant
import fr.tflag.droidboost.shell.ShizukuState
import fr.tflag.droidboost.ui.components.SectionCard
import fr.tflag.droidboost.ui.components.StatusPill
import fr.tflag.droidboost.ui.theme.ok
import fr.tflag.droidboost.ui.theme.warning

private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccessScreen(viewModel: AccessViewModel, contentPadding: PaddingValues, snackbar: SnackbarHostState) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refresh() }

    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    fun openSettings(action: String, withPackage: Boolean = true) {
        val intent = Intent(action)
        if (withPackage) intent.data = "package:${viewModel.packageName}".toUri()
        runCatching { context.startActivity(intent) }.onFailure { context.startActivity(Intent(action)) }
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
        item { ShizukuCard(state.shizuku, viewModel::requestShizukuPermission) { openShizuku(context) } }
        item {
            SectionCard("Accès spéciaux", Icons.Outlined.Tune) {
                AccessRow("Données d'usage", "Tailles des applis, temps d'écran, données par appli", state.usageStats) {
                    openSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                }
                AccessRow("Accès à tous les fichiers", "Analyse du stockage, dialogue de vidage des caches", state.allFiles) {
                    openSettings(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                }
                AccessRow("Notifications", "Notification permanente et alertes", state.notifications) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                AccessRow("Superposition", "Moniteur flottant (à venir)", state.overlay) {
                    openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                }
                Text(
                    "Si Android affiche « paramètre restreint », ouvrez Infos de l'appli > ⋮ > « Autoriser les paramètres " +
                        "restreints », puis réessayez.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { openSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS) }) { Text("Infos de l'appli") }
            }
        }
        item {
            SectionCard("Permissions ADB", Icons.Outlined.Key) {
                Text(
                    "Accordées une seule fois, elles restent actives après redémarrage, même sans Shizuku.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DevGrant.entries.forEach { grant ->
                    val granted = state.grants[grant] == true
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(grant.title, style = MaterialTheme.typography.bodyMedium)
                            Text(grant.unlocks, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPill(
                            if (granted) "Accordée" else "Manquante",
                            if (granted) MaterialTheme.colorScheme.ok else MaterialTheme.colorScheme.warning,
                        )
                    }
                }
                FilledTonalButton(
                    onClick = viewModel::grantAllViaShizuku,
                    enabled = state.shizuku is ShizukuState.Ready && !state.granting && state.grants.values.any { !it },
                ) { Text(if (state.granting) "Octroi en cours…" else "Tout accorder via Shizuku") }
            }
        }
        item {
            SectionCard("Depuis un PC", Icons.Outlined.Computer) {
                Text(
                    "Sans Shizuku : branchez le téléphone (débogage USB activé) et lancez ces commandes une fois.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        state.adbScript,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { copy(context, state.adbScript) }) { Text("Copier") }
                }
            }
        }
    }
}

@Composable
private fun ShizukuCard(state: ShizukuState, onRequestPermission: () -> Unit, onOpenShizuku: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (label, color, explanation) = when (state) {
        is ShizukuState.Ready -> Triple(
            if (state.isRoot) "Actif (root)" else "Actif (ADB)",
            colors.ok,
            "Actions avancées, températures des capteurs et charge CPU disponibles. Serveur v${state.serverVersion}.",
        )
        ShizukuState.PermissionRequired -> Triple("À autoriser", colors.warning, "Shizuku tourne : autorisez DroidBoost à l'utiliser.")
        ShizukuState.PermissionDenied -> Triple(
            "Refusé",
            colors.error,
            "L'autorisation a été refusée. Réautorisez DroidBoost depuis l'appli Shizuku.",
        )
        ShizukuState.Unavailable -> Triple(
            "Inactif",
            colors.outline,
            "Installez Shizuku (ou un serveur compatible comme Porter) et démarrez-le par le débogage sans fil. " +
                "Il doit être relancé après chaque redémarrage du téléphone.",
        )
    }
    SectionCard("Shizuku", Icons.Outlined.AdminPanelSettings, trailing = { StatusPill(label, color) }) {
        Text(explanation, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state == ShizukuState.PermissionRequired) FilledTonalButton(onClick = onRequestPermission) { Text("Autoriser") }
            OutlinedButton(onClick = onOpenShizuku) { Text("Ouvrir Shizuku") }
        }
    }
}

@Composable
private fun AccessRow(title: String, unlocks: String, granted: Boolean, onOpen: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(unlocks, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (granted) StatusPill("Activé", MaterialTheme.colorScheme.ok) else TextButton(onClick = onOpen) { Text("Activer") }
    }
}

private fun openShizuku(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        ?: Intent(Intent.ACTION_VIEW, "https://github.com/RikkaApps/Shizuku/releases".toUri())
    runCatching { context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun copy(context: Context, text: String) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Commandes ADB", text))
}
