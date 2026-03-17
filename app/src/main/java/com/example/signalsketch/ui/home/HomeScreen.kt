package com.example.signalsketch.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        HomeActionButton(label = "Live Scan", onClick = onLiveScanClick)
        HomeActionButton(label = "Mapping", onClick = onMappingClick)
        HomeActionButton(label = "AR Mapping", onClick = onArMappingClick)
        HomeActionButton(label = "Saved Sessions", onClick = onSavedSessionsClick)
        HomeActionButton(label = "Settings", onClick = onSettingsClick)
    }
}

@Composable
private fun HomeActionButton(
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = label)
    }
}
