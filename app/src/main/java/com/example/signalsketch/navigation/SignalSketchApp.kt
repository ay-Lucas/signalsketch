package com.example.signalsketch.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.signalsketch.ui.ar.ArMappingScreen
import com.example.signalsketch.ui.home.HomeScreen
import com.example.signalsketch.ui.mapping.MappingScreen
import com.example.signalsketch.ui.scan.LiveScanScreen
import com.example.signalsketch.ui.sessions.SavedSessionsScreen
import com.example.signalsketch.ui.settings.SettingsScreen
import com.example.signalsketch.viewmodel.ArMappingViewModel
import com.example.signalsketch.viewmodel.HomeViewModel
import com.example.signalsketch.viewmodel.MappingViewModel
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
            NavigationBar {
                topLevelDestinations.forEach { destination ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == destination.route } == true
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
                        icon = { Text(destination.title.take(1)) },
                        label = { Text(destination.title) }
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
                MappingScreen(viewModel = viewModel<MappingViewModel>())
            }
            composable(AppDestination.ArMapping.route) {
                ArMappingScreen(viewModel = viewModel<ArMappingViewModel>())
            }
            composable(AppDestination.SavedSessions.route) {
                SavedSessionsScreen(viewModel = viewModel<SessionsViewModel>())
            }
            composable(AppDestination.Settings.route) {
                SettingsScreen(viewModel = viewModel<SettingsViewModel>())
            }
        }
    }
}
