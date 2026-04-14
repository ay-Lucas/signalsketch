package com.example.signalsketch.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavType
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.signalsketch.ui.ar.ArMappingScreen
import com.example.signalsketch.ui.home.HomeScreen
import com.example.signalsketch.ui.mapping.MappingScreen
import com.example.signalsketch.ui.scan.LiveScanScreen
import com.example.signalsketch.ui.sessions.SavedSessionDetailScreen
import com.example.signalsketch.ui.sessions.SavedSessionsScreen
import com.example.signalsketch.ui.settings.SettingsScreen
import com.example.signalsketch.ui.theme.OnDark
import com.example.signalsketch.ui.theme.SurfaceDark
import com.example.signalsketch.ui.theme.YellowPrimaryLight
import com.example.signalsketch.viewmodel.ArMappingViewModel
import com.example.signalsketch.viewmodel.HomeViewModel
import com.example.signalsketch.viewmodel.MappingSessionViewModel
import com.example.signalsketch.viewmodel.SavedSessionDetailViewModel
import com.example.signalsketch.viewmodel.ScanViewModel
import com.example.signalsketch.viewmodel.SessionsViewModel
import com.example.signalsketch.viewmodel.SettingsViewModel

private val topLevelDestinations = listOf(
    AppDestination.Home,
    AppDestination.LiveScan,
    AppDestination.Mapping,
    AppDestination.ArMapping,
    AppDestination.SavedSessions,
    AppDestination.Settings,
)

@Composable
fun SignalSketchApp() {
    val navController = rememberNavController()
    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry.value?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = OnDark
            ) {
                topLevelDestinations.forEach { destination ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == destination.route } == true

                    val icon = when (destination) {
                        AppDestination.Home -> Icons.Outlined.Home
                        AppDestination.LiveScan -> Icons.Outlined.Wifi
                        AppDestination.Mapping -> Icons.Outlined.Map
                        AppDestination.ArMapping -> Icons.Outlined.ViewInAr
                        AppDestination.SavedSessions -> Icons.Outlined.ListAlt
                        AppDestination.SavedSessionDetail -> Icons.Outlined.ListAlt
                        AppDestination.Settings -> Icons.Outlined.Settings
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = destination.title
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = YellowPrimaryLight,
                            selectedTextColor = YellowPrimaryLight,
                            unselectedIconColor = OnDark.copy(alpha = 0.7f),
                            unselectedTextColor = OnDark.copy(alpha = 0.7f),
                            indicatorColor = SurfaceDark
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.Home.route) {
                HomeScreen(
                    viewModel = viewModel(),
                    onLiveScanClick = { navController.navigate(AppDestination.LiveScan.route) },
                    onMappingClick = { navController.navigate(AppDestination.Mapping.route) },
                    onArMappingClick = { navController.navigate(AppDestination.ArMapping.route) },
                    onSavedSessionsClick = { navController.navigate(AppDestination.SavedSessions.route) },
                    onSettingsClick = { navController.navigate(AppDestination.Settings.route) }
                )
            }
            composable(AppDestination.LiveScan.route) {
                LiveScanScreen(viewModel = viewModel<ScanViewModel>())
            }
            composable(AppDestination.Mapping.route) {
                MappingScreen(viewModel = viewModel<MappingSessionViewModel>())
            }
            composable(AppDestination.ArMapping.route) {
                ArMappingScreen(
                    viewModel = viewModel<ArMappingViewModel>(),
                    onOpenStandardMapping = {
                        navController.navigate(AppDestination.Mapping.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppDestination.SavedSessions.route) {
                SavedSessionsScreen(
                    viewModel = viewModel<SessionsViewModel>(),
                    onSessionClick = { sessionId ->
                        navController.navigate(AppDestination.SavedSessionDetail.createRoute(sessionId))
                    }
                )
            }
            composable(
                route = AppDestination.SavedSessionDetail.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) {
                SavedSessionDetailScreen(
                    viewModel = viewModel<SavedSessionDetailViewModel>(),
                    onBack = { navController.popBackStack() }
                )
            }
            composable(AppDestination.Settings.route) {
                SettingsScreen(viewModel = viewModel<SettingsViewModel>())
            }
        }
    }
}
