package com.example.signalsketch.ui.scan

import androidx.compose.runtime.Composable
import com.example.signalsketch.ui.shared.PlaceholderScreen
import com.example.signalsketch.viewmodel.ScanViewModel

@Composable
fun LiveScanScreen(viewModel: ScanViewModel) {
    PlaceholderScreen(
        title = "Live Scan",
        description = viewModel.description
    )
}
