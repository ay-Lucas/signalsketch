package com.example.signalsketch.ui.mapping

import androidx.compose.runtime.Composable
import com.example.signalsketch.ui.shared.PlaceholderScreen
import com.example.signalsketch.viewmodel.MappingViewModel

@Composable
fun MappingScreen(viewModel: MappingViewModel) {
    PlaceholderScreen(
        title = "Mapping",
        description = viewModel.description
    )
}
