package com.example.signalsketch.ui.ar

import androidx.compose.runtime.Composable
import com.example.signalsketch.ui.shared.PlaceholderScreen
import com.example.signalsketch.viewmodel.ArMappingViewModel

@Composable
fun ArMappingScreen(viewModel: ArMappingViewModel) {
    PlaceholderScreen(
        title = "AR Mapping",
        description = viewModel.description
    )
}
