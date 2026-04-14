package com.example.signalsketch.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.signalsketch.ui.theme.OnDark
import com.example.signalsketch.ui.theme.SurfaceDark
import com.example.signalsketch.viewmodel.SettingsUiState
import com.example.signalsketch.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    SettingsScreen(
        uiState = uiState,
        onCycleColorScale = viewModel::cycleColorScale,
        onCycleMappingMode = viewModel::cyclePreferredMappingMode,
        onCycleScanInterval = viewModel::cycleScanInterval,
        onToggleOnboarding = viewModel::toggleOnboardingComplete
    )
}

@Composable
private fun SettingsScreen(
    uiState: SettingsUiState,
    onCycleColorScale: () -> Unit,
    onCycleMappingMode: () -> Unit,
    onCycleScanInterval: () -> Unit,
    onToggleOnboarding: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "These preferences are stored locally with DataStore and apply across app restarts.",
            style = MaterialTheme.typography.bodyMedium
        )

        if (uiState.isLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceDark,
                    contentColor = OnDark
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Loading settings...")
                }
            }
        } else {
            PreferenceCard(
                title = "Heatmap Color Scale",
                value = uiState.colorScale,
                buttonLabel = "Change Color Scale",
                onClick = onCycleColorScale
            )
            PreferenceCard(
                title = "Preferred Mapping Mode",
                value = uiState.preferredMappingMode,
                buttonLabel = "Toggle Preferred Mode",
                onClick = onCycleMappingMode
            )
            PreferenceCard(
                title = "Wi-Fi Scan Interval",
                value = uiState.scanIntervalLabel,
                buttonLabel = "Change Scan Interval",
                onClick = onCycleScanInterval
            )
            PreferenceCard(
                title = "Onboarding Status",
                value = uiState.onboardingLabel,
                buttonLabel = "Toggle Onboarding Status",
                onClick = onToggleOnboarding
            )
        }
    }
}

@Composable
private fun PreferenceCard(
    title: String,
    value: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$title. Current value $value."
            },
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark,
            contentColor = OnDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonLabel)
            }
        }
    }
}
