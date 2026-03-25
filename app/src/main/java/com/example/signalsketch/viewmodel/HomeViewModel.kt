package com.example.signalsketch.viewmodel

import androidx.lifecycle.ViewModel

data class HomeActionUiState(
    val label: String,
    val description: String
)

class HomeViewModel : ViewModel() {
    val subtitle: String = "Choose a workflow for live scanning, path mapping, AR-assisted capture, or reviewing saved results."

    val actionSummaries: Map<String, HomeActionUiState> = listOf(
        HomeActionUiState(
            label = "Live Scan",
            description = "Inspect the current Wi-Fi environment without starting a mapping session."
        ),
        HomeActionUiState(
            label = "Mapping",
            description = "Record motion and Wi-Fi samples together in the standard non-AR workflow."
        ),
        HomeActionUiState(
            label = "AR Mapping",
            description = "Use AR when supported, with safe fallback to standard mapping when tracking is poor."
        ),
        HomeActionUiState(
            label = "Saved Sessions",
            description = "Review, share, or delete previously saved mapping sessions."
        ),
        HomeActionUiState(
            label = "Settings",
            description = "Adjust local app preferences stored on this device."
        )
    ).associateBy { it.label }
}
