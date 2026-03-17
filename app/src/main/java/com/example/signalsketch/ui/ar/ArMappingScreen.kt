package com.example.signalsketch.ui.ar

import android.app.Activity
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.signalsketch.ar.ArAvailabilityState
import com.example.signalsketch.viewmodel.ArMappingViewModel

@Composable
fun ArMappingScreen(viewModel: ArMappingViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context.findActivity()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.onCameraPermissionResult()
    }

    ArMappingScreen(
        state = uiState,
        onRefresh = viewModel::refresh,
        onRequestCameraPermission = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
        onRequestArInstall = {
            activity?.let(viewModel::requestArInstall)
        }
    )
}

@Composable
private fun ArMappingScreen(
    state: ArAvailabilityState,
    onRefresh: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRequestArInstall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "AR Mapping",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "AR is optional. The core 2D mapping flow remains available even when AR is unsupported or not installed.",
            style = MaterialTheme.typography.bodyMedium
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "AR Availability",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(text = "Device Support: ${state.supportState.name}")
                Text(text = "Install / Update Status: ${state.installState.name}")
                Text(text = "Camera Permission: ${if (state.hasCameraPermission) "Granted" else "Missing"}")
                Text(text = "Can Start AR: ${if (state.canStartAr) "Yes" else "No"}")
                Text(
                    text = state.statusMessage,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRequestCameraPermission,
                        modifier = Modifier.weight(1f),
                        enabled = !state.hasCameraPermission
                    ) {
                        Text("Grant Camera")
                    }
                    OutlinedButton(
                        onClick = onRequestArInstall,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Install / Update AR")
                    }
                }
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Refresh AR Status")
                }
            }
        }
    }
}

private tailrec fun android.content.Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
