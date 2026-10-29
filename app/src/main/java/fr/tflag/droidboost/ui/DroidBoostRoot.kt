package fr.tflag.droidboost.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fr.tflag.droidboost.AppContainer
import fr.tflag.droidboost.ui.access.AccessScreen
import fr.tflag.droidboost.ui.access.AccessViewModel
import fr.tflag.droidboost.ui.apps.AppDetailScreen
import fr.tflag.droidboost.ui.apps.AppDetailViewModel
import fr.tflag.droidboost.ui.apps.AppsScreen
import fr.tflag.droidboost.ui.apps.AppsViewModel
import fr.tflag.droidboost.ui.battery.BatteryScreen
import fr.tflag.droidboost.ui.battery.BatteryViewModel
import fr.tflag.droidboost.ui.cleaner.CleanerScreen
import fr.tflag.droidboost.ui.cleaner.CleanerViewModel
import fr.tflag.droidboost.ui.dashboard.DashboardScreen
import fr.tflag.droidboost.ui.debloat.DebloatScreen
import fr.tflag.droidboost.ui.debloat.DebloatViewModel
import fr.tflag.droidboost.ui.dashboard.DashboardViewModel
import fr.tflag.droidboost.ui.journal.JournalScreen
import fr.tflag.droidboost.ui.journal.JournalViewModel
import fr.tflag.droidboost.ui.more.MoreScreen
import fr.tflag.droidboost.ui.network.NetworkScreen
import fr.tflag.droidboost.ui.network.NetworkViewModel
import fr.tflag.droidboost.ui.privacy.PrivacyScreen
import fr.tflag.droidboost.ui.privacy.PrivacyViewModel
import fr.tflag.droidboost.ui.privacy.SecurityScreen
import fr.tflag.droidboost.ui.settings.SettingsScreen
import fr.tflag.droidboost.ui.settings.SettingsViewModel
import fr.tflag.droidboost.ui.storage.StorageScreen
import fr.tflag.droidboost.ui.storage.StorageViewModel
import fr.tflag.droidboost.ui.thermal.ThermalScreen
import fr.tflag.droidboost.ui.thermal.ThermalViewModel

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Tableau de bord", Icons.Outlined.Speed),
    APPS("apps", "Applis", Icons.Outlined.Apps),
    STORAGE("storage", "Stockage", Icons.Outlined.Storage),
    JOURNAL("journal", "Journal", Icons.Outlined.History),
    MORE("more", "Plus", Icons.Outlined.MoreHoriz),
}

private const val APP_DETAIL = "app/{pkg}"

/** Secondary screens: title shown in the top bar and tab highlighted underneath. */
private val SECONDARY = mapOf(
    APP_DETAIL to ("Détail de l'appli" to Tab.APPS),
    "battery" to ("Batterie" to Tab.MORE),
    "thermal" to ("Thermique" to Tab.MORE),
    "network" to ("Réseau" to Tab.MORE),
    "cleaner" to ("Nettoyage" to Tab.MORE),
    "debloat" to ("Debloat Pixel" to Tab.MORE),
    "privacy" to ("Vie privée" to Tab.MORE),
    "security" to ("Sécurité" to Tab.MORE),
    "access" to ("Accès" to Tab.MORE),
    "settings" to ("Réglages" to Tab.MORE),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroidBoostRoot(container: AppContainer) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    val secondary = SECONDARY[route]
    val currentTab = Tab.entries.firstOrNull { it.route == route } ?: secondary?.second ?: Tab.DASHBOARD

    fun openTab(tab: Tab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openApp(packageName: String) = navController.navigate("app/$packageName")

    fun navigateTo(route: String) = navController.navigate(route) { launchSingleTop = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(secondary?.first ?: currentTab.label) },
                navigationIcon = {
                    if (secondary != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == currentTab,
                        onClick = { openTab(tab) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        NavHost(navController, startDestination = Tab.DASHBOARD.route) {
            composable(Tab.DASHBOARD.route) {
                DashboardScreen(
                    viewModel { DashboardViewModel(container) },
                    padding,
                    onOpenAccess = { navigateTo("access") },
                    onOpenBattery = { navigateTo("battery") },
                    onOpenThermal = { navigateTo("thermal") },
                )
            }
            composable(Tab.APPS.route) {
                AppsScreen(
                    viewModel { AppsViewModel(container) },
                    container.apps,
                    padding,
                    onOpenApp = ::openApp,
                    onOpenAccess = { navigateTo("access") },
                )
            }
            composable(APP_DETAIL) { entry ->
                val pkg = entry.arguments?.getString("pkg").orEmpty()
                AppDetailScreen(
                    viewModel(key = pkg) { AppDetailViewModel(container, pkg) },
                    container.apps,
                    padding,
                    snackbar,
                    onOpenAccess = { navigateTo("access") },
                )
            }
            composable(Tab.STORAGE.route) {
                StorageScreen(
                    viewModel { StorageViewModel(container) },
                    container.apps,
                    padding,
                    snackbar,
                    onOpenApp = ::openApp,
                    onOpenAccess = { navigateTo("access") },
                )
            }
            composable(Tab.JOURNAL.route) {
                JournalScreen(viewModel { JournalViewModel(container) }, padding, snackbar, onOpenAccess = { navigateTo("access") })
            }
            composable(Tab.MORE.route) { MoreScreen(padding, onOpen = ::navigateTo) }
            composable("access") {
                AccessScreen(viewModel { AccessViewModel(container) }, padding, snackbar)
            }
            composable("battery") {
                BatteryScreen(
                    viewModel { BatteryViewModel(container) },
                    padding,
                    onOpenAccess = { navigateTo("access") },
                    onOpenDashboard = { openTab(Tab.DASHBOARD) },
                    onOpenApp = ::openApp,
                )
            }
            composable("thermal") {
                ThermalScreen(
                    viewModel { ThermalViewModel(container) },
                    padding,
                    onOpenAccess = { navigateTo("access") },
                    onOpenDashboard = { openTab(Tab.DASHBOARD) },
                )
            }
            composable("network") {
                NetworkScreen(
                    viewModel { NetworkViewModel(container) },
                    padding,
                    onOpenApp = ::openApp,
                    onOpenAccess = { navigateTo("access") },
                )
            }
            composable("privacy") {
                PrivacyScreen(
                    viewModel { PrivacyViewModel(container) },
                    padding,
                    onOpenApp = ::openApp,
                    onOpenAccess = { navigateTo("access") },
                )
            }
            composable("cleaner") {
                CleanerScreen(viewModel { CleanerViewModel(container) }, padding, snackbar, onOpenAccess = { navigateTo("access") })
            }
            composable("debloat") {
                DebloatScreen(
                    viewModel { DebloatViewModel(container) },
                    container.apps,
                    padding,
                    snackbar,
                    onOpenApp = ::openApp,
                    onOpenAccess = { navigateTo("access") },
                )
            }
            composable("security") { SecurityScreen(viewModel { PrivacyViewModel(container) }, padding) }
            composable("settings") { SettingsScreen(viewModel { SettingsViewModel(container) }, padding) }
        }
    }
}
