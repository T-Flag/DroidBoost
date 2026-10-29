package fr.tflag.droidboost.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

data class MoreEntry(val route: String, val title: String, val subtitle: String, val icon: ImageVector)

val MORE_ENTRIES = listOf(
    MoreEntry("battery", "Batterie", "Santé, historique, sessions de charge", Icons.Outlined.BatteryChargingFull),
    MoreEntry("thermal", "Thermique", "Températures, bridage, épisodes", Icons.Outlined.Thermostat),
    MoreEntry("network", "Réseau", "Données par appli, débit en direct", Icons.Outlined.NetworkCheck),
    MoreEntry("privacy", "Vie privée", "Permissions sensibles, accès spéciaux", Icons.Outlined.PrivacyTip),
    MoreEntry("security", "Sécurité", "Correctif, démarrage vérifié, débogage, DNS", Icons.Outlined.Shield),
    MoreEntry("access", "Accès", "Shizuku, accès spéciaux, permissions ADB", Icons.Outlined.AdminPanelSettings),
    MoreEntry("settings", "Réglages", "Enregistrement, rétention, alertes", Icons.Outlined.Settings),
)

@Composable
fun MoreScreen(contentPadding: PaddingValues, onOpen: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(MORE_ENTRIES, key = { it.route }) { entry ->
            ListItem(
                headlineContent = { Text(entry.title) },
                supportingContent = { Text(entry.subtitle) },
                leadingContent = { Icon(entry.icon, contentDescription = null) },
                modifier = Modifier.clickable { onOpen(entry.route) },
            )
        }
    }
}
