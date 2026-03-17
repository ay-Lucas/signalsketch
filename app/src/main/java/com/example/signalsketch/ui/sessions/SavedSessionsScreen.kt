package com.example.signalsketch.ui.sessions

import androidx.compose.runtime.Composable
import com.example.signalsketch.ui.shared.PlaceholderScreen
import com.example.signalsketch.viewmodel.SessionsViewModel

@Composable
fun SavedSessionsScreen(viewModel: SessionsViewModel) {
    PlaceholderScreen(
        title = "Saved Sessions",
        description = viewModel.description
    )
}
