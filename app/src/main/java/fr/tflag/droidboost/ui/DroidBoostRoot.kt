package fr.tflag.droidboost.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Apps
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
import fr.tflag.droidboost.ui.dashboard.DashboardScreen
import fr.tflag.droidboost.ui.dashboard.DashboardViewModel
import fr.tflag.droidboost.ui.storage.StorageScreen
import fr.tflag.droidboost.ui.storage.StorageViewModel

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Tableau de bord", Icons.Outlined.Speed),
    APPS("apps", "Applis", Icons.Outlined.Apps),
    STORAGE("storage", "Stockage", Icons.Outlined.Storage),
    ACCESS("access", "Accès", Icons.Outlined.AdminPanelSettings),
}

private const val APP_DETAIL = "app/{pkg}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroidBoostRoot(container: AppContainer) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    val currentTab = Tab.entries.firstOrNull { it.route == route } ?: if (route == APP_DETAIL) Tab.APPS else Tab.DASHBOARD

    fun openTab(tab: Tab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openApp(packageName: String) = navController.navigate("app/$packageName")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (route == APP_DETAIL) "Détail de l'appli" else currentTab.label) },
                navigationIcon = {
                    if (route == APP_DETAIL) {
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
                DashboardScreen(viewModel { DashboardViewModel(container) }, padding, onOpenAccess = { openTab(Tab.ACCESS) })
            }
            composable(Tab.APPS.route) {
                AppsScreen(
                    viewModel { AppsViewModel(container) },
                    container.apps,
                    padding,
                    onOpenApp = ::openApp,
                    onOpenAccess = { openTab(Tab.ACCESS) },
                )
            }
            composable(APP_DETAIL) { entry ->
                val pkg = entry.arguments?.getString("pkg").orEmpty()
                AppDetailScreen(
                    viewModel(key = pkg) { AppDetailViewModel(container, pkg) },
                    container.apps,
                    padding,
                    snackbar,
                    onOpenAccess = { openTab(Tab.ACCESS) },
                )
            }
            composable(Tab.STORAGE.route) {
                StorageScreen(
                    viewModel { StorageViewModel(container) },
                    container.apps,
                    padding,
                    snackbar,
                    onOpenApp = ::openApp,
                    onOpenAccess = { openTab(Tab.ACCESS) },
                )
            }
            composable(Tab.ACCESS.route) {
                AccessScreen(viewModel { AccessViewModel(container) }, padding, snackbar)
            }
        }
    }
}
