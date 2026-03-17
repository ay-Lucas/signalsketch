package com.example.signalsketch.ui.settings

import androidx.compose.runtime.Composable
import com.example.signalsketch.ui.shared.PlaceholderScreen
import com.example.signalsketch.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    PlaceholderScreen(
        title = "Settings",
        description = viewModel.description
    )
}
