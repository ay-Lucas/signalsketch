package com.example.signalsketch.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.signalsketch.ui.theme.OnDark
import com.example.signalsketch.ui.theme.SurfaceDark
import com.example.signalsketch.viewmodel.HomeActionUiState
import com.example.signalsketch.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onLiveScanClick: () -> Unit,
    onMappingClick: () -> Unit,
    onArMappingClick: () -> Unit,
    onSavedSessionsClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val actions = viewModel.actionSummaries

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SignalSketch",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = viewModel.subtitle,
            style = MaterialTheme.typography.bodyLarge
        )
        HomeActionButton(
            action = requireNotNull(actions["Live Scan"]),
            onClick = onLiveScanClick
        )
        HomeActionButton(
            action = requireNotNull(actions["Mapping"]),
            onClick = onMappingClick
        )
        HomeActionButton(
            action = requireNotNull(actions["AR Mapping"]),
            onClick = onArMappingClick
        )
        HomeActionButton(
            action = requireNotNull(actions["Saved Sessions"]),
            onClick = onSavedSessionsClick
        )
        HomeActionButton(
            action = requireNotNull(actions["Settings"]),
            onClick = onSettingsClick
        )
    }
}

@Composable
private fun HomeActionButton(
    action: HomeActionUiState,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark,
            contentColor = OnDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = onClick,
                modifier = Modifier.semantics {
                    contentDescription = "${action.label}. ${action.description}"
                }
            ) {
                Text(text = "Open")
            }
        }
    }
}
