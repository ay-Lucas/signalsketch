package com.example.signalsketch.ui.mapping

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
import androidx.compose.ui.unit.dp
import com.example.signalsketch.sensors.MotionTrackingState
import com.example.signalsketch.viewmodel.MappingSessionUiState
import com.example.signalsketch.viewmodel.MappingSessionViewModel
import kotlin.math.absoluteValue

@Composable
fun MappingScreen(viewModel: MappingSessionViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    MappingScreen(
        state = uiState,
        onStartSession = viewModel::startSession,
        onPauseSession = viewModel::pauseSession,
        onResumeSession = viewModel::resumeSession,
        onResetSession = viewModel::resetSession
    )
}

@Composable
private fun MappingScreen(
    state: MappingSessionUiState,
    onStartSession: () -> Unit,
    onPauseSession: () -> Unit,
    onResumeSession: () -> Unit,
    onResetSession: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Mapping",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Normal mapping mode records motion and Wi-Fi samples together in one live session.",
            style = MaterialTheme.typography.bodyMedium
        )
        SessionStatusCard(
            state = state,
            onStartSession = onStartSession,
            onPauseSession = onPauseSession,
            onResumeSession = onResumeSession,
            onResetSession = onResetSession
        )
        MotionDebugCard(state = state)
    }
}

@Composable
private fun SessionStatusCard(
    state: MappingSessionUiState,
    onStartSession: () -> Unit,
    onPauseSession: () -> Unit,
    onResumeSession: () -> Unit,
    onResetSession: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Session Status",
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = "Lifecycle: ${state.sessionState.name}")
            Text(text = "Session ID: ${state.sessionId ?: "Not started"}")
            Text(text = "Wi-Fi Samples Recorded: ${state.wifiSampleCount}")
            Text(text = "Path Samples Recorded: ${state.pathSampleCount}")
            Text(text = "Last Wi-Fi Capture: ${state.lastWifiCaptureAtEpochMillis ?: "None"}")
            Text(text = "Last Path Capture: ${state.lastPathCaptureAtEpochMillis ?: "None"}")
            if (state.statusMessage != null) {
                Text(
                    text = state.statusMessage,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartSession,
                    enabled = state.canStart,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start")
                }
                OutlinedButton(
                    onClick = onPauseSession,
                    enabled = state.canPause,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pause")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onResumeSession,
                    enabled = state.canResume,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Resume")
                }
                OutlinedButton(
                    onClick = onResetSession,
                    enabled = state.canReset,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun MotionDebugCard(state: MappingSessionUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Motion Debug",
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = "Tracking State: ${state.trackingState.name}")
            Text(text = "Heading Estimate: ${state.headingDegrees.format(1)}°")
            Text(
                text = "Movement Delta: x=${state.deltaXMeters.format(3)} m, y=${state.deltaYMeters.format(3)} m"
            )
            Text(text = "Sensor Sample Count: ${state.sensorSampleCount}")
            if (state.trackingState == MotionTrackingState.IDLE &&
                state.sensorSampleCount == 0 &&
                state.deltaXMeters.absoluteValue < 0.0001f &&
                state.deltaYMeters.absoluteValue < 0.0001f
            ) {
                Text(
                    text = "Tracking is idle. Start a session to capture motion and Wi-Fi together.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (state.trackingState == MotionTrackingState.SENSOR_UNAVAILABLE) {
                Text(
                    text = "This device does not expose both required motion sensors.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun Float.format(decimals: Int): String {
    return "%.${decimals}f".format(this)
}
