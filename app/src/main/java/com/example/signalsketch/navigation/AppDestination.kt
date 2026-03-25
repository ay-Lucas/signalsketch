package com.example.signalsketch.navigation

sealed class AppDestination(
    val route: String,
    val title: String
) {
    data object Home : AppDestination("home", "Home")
    data object LiveScan : AppDestination("live_scan", "Live Scan")
    data object Mapping : AppDestination("mapping", "Mapping")
    data object ArMapping : AppDestination("ar_mapping", "AR Mapping")
    data object SavedSessions : AppDestination("saved_sessions", "Saved Sessions")
    data object SavedSessionDetail : AppDestination("saved_sessions/{sessionId}", "Session Detail") {
        fun createRoute(sessionId: Long): String = "saved_sessions/$sessionId"
    }
    data object Settings : AppDestination("settings", "Settings")
}
