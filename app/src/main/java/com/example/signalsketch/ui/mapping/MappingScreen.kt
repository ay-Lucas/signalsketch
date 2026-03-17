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
        onStartTracking = viewModel::startTracking,
        onStopTracking = viewModel::stopTracking
    )
}

@Composable
private fun MappingScreen(
    state: MappingSessionUiState,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit
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
            text = "First-pass motion tracking debug view for accelerometer and gyroscope estimates.",
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStartTracking,
                modifier = Modifier.weight(1f),
                enabled = state.trackingState != MotionTrackingState.TRACKING
            ) {
                Text("Start Tracking")
            }
            OutlinedButton(
                onClick = onStopTracking,
                modifier = Modifier.weight(1f),
                enabled = state.trackingState == MotionTrackingState.TRACKING
            ) {
                Text("Stop Tracking")
            }
        }
        MotionDebugCard(state = state)
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
                    text = "Tracking is idle. Start tracking to watch heading and movement estimates update.",
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
