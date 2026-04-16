package com.example.signalsketch.ui.mapping

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.signalsketch.ui.theme.OnDark
import com.example.signalsketch.ui.theme.SurfaceDark
import com.example.signalsketch.viewmodel.FloorplanRoomBox
import com.example.signalsketch.position.PositionSourceType
import com.example.signalsketch.position.TrackingQuality
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
        onSaveSession = viewModel::saveSession,
        onResetSession = viewModel::resetSession,
        onAddFloorplanBox = viewModel::addFloorplanBox,
        onUpdateFloorplanBoxLabel = viewModel::updateFloorplanBoxLabel,
        onUpdateFloorplanBoxWidth = viewModel::updateFloorplanBoxWidth,
        onUpdateFloorplanBoxHeight = viewModel::updateFloorplanBoxHeight,
        onUpdateFloorplanBoxPosition = viewModel::updateFloorplanBoxPosition,
        onRemoveFloorplanBox = viewModel::removeFloorplanBox
    )
}

@Composable
private fun MappingScreen(
    state: MappingSessionUiState,
    onStartSession: () -> Unit,
    onPauseSession: () -> Unit,
    onResumeSession: () -> Unit,
    onSaveSession: () -> Unit,
    onResetSession: () -> Unit,
    onAddFloorplanBox: (String) -> Unit,
    onUpdateFloorplanBoxLabel: (Long, String) -> Unit,
    onUpdateFloorplanBoxWidth: (Long, Float) -> Unit,
    onUpdateFloorplanBoxHeight: (Long, Float) -> Unit,
    onUpdateFloorplanBoxPosition: (Long, Float, Float) -> Unit,
    onRemoveFloorplanBox: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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
            onSaveSession = onSaveSession,
            onResetSession = onResetSession
        )
        SessionMapCard(
            pathSamples = state.pathSamples,
            wifiSamples = state.wifiSamples,
            roomBoxes = state.floorplanBoxes,
            emptyMessage = "Start a session to render the walked path and Wi-Fi samples."
        )
        FloorplanBuilderCard(
            boxes = state.floorplanBoxes,
            onAddBox = onAddFloorplanBox,
            onUpdateBoxLabel = onUpdateFloorplanBoxLabel,
            onUpdateBoxWidth = onUpdateFloorplanBoxWidth,
            onUpdateBoxHeight = onUpdateFloorplanBoxHeight,
            onUpdateBoxPosition = onUpdateFloorplanBoxPosition,
            onRemoveBox = onRemoveFloorplanBox
        )
        MotionDebugCard(state = state)
    }
}

@Composable
private fun FloorplanBuilderCard(
    boxes: List<FloorplanRoomBox>,
    onAddBox: (String) -> Unit,
    onUpdateBoxLabel: (Long, String) -> Unit,
    onUpdateBoxWidth: (Long, Float) -> Unit,
    onUpdateBoxHeight: (Long, Float) -> Unit,
    onUpdateBoxPosition: (Long, Float, Float) -> Unit,
    onRemoveBox: (Long) -> Unit
) {
    var newLabel by remember { mutableStateOf("") }

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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Floorplan Builder",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Add labeled room boxes over the live heatmap. Boxes are centered on your latest tracked position.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    label = { Text("Room label") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        onAddBox(newLabel)
                        newLabel = ""
                    }
                ) {
                    Text("Add")
                }
            }

            if (boxes.isEmpty()) {
                Text(
                    text = "No room boxes yet.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    boxes.forEachIndexed { index, box ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = androidx.compose.ui.graphics.Color(box.colorArgb).copy(alpha = 0.75f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(
                                                color = androidx.compose.ui.graphics.Color(box.colorArgb),
                                                shape = CircleShape
                                            )
                                    )
                                    Text(
                                        text = "Room ${index + 1}",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = box.label,
                                    onValueChange = { onUpdateBoxLabel(box.id, it) },
                                    label = { Text("Label") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedButton(onClick = { onRemoveBox(box.id) }) {
                                    Text("Remove")
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NumericStepper(
                                    label = "Width (m)",
                                    value = box.widthMeters,
                                    onDecrease = { onUpdateBoxWidth(box.id, box.widthMeters - 1f) },
                                    onIncrease = { onUpdateBoxWidth(box.id, box.widthMeters + 1f) },
                                    modifier = Modifier.weight(1f)
                                )
                                NumericStepper(
                                    label = "Height (m)",
                                    value = box.heightMeters,
                                    onDecrease = { onUpdateBoxHeight(box.id, box.heightMeters - 1f) },
                                    onIncrease = { onUpdateBoxHeight(box.id, box.heightMeters + 1f) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NumericStepper(
                                    label = "X (m)",
                                    value = box.centerXMeters,
                                    onDecrease = { onUpdateBoxPosition(box.id, box.centerXMeters - 1f, box.centerYMeters) },
                                    onIncrease = { onUpdateBoxPosition(box.id, box.centerXMeters + 1f, box.centerYMeters) },
                                    modifier = Modifier.weight(1f)
                                )
                                NumericStepper(
                                    label = "Y (m)",
                                    value = box.centerYMeters,
                                    onDecrease = { onUpdateBoxPosition(box.id, box.centerXMeters, box.centerYMeters - 1f) },
                                    onIncrease = { onUpdateBoxPosition(box.id, box.centerXMeters, box.centerYMeters + 1f) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumericStepper(
    label: String,
    value: Float,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$label: ${value.format(1)}",
            style = MaterialTheme.typography.labelMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDecrease,
                modifier = Modifier.weight(1f)
            ) {
                Text("-")
            }
            OutlinedButton(
                onClick = onIncrease,
                modifier = Modifier.weight(1f)
            ) {
                Text("+")
            }
        }
    }
}

@Composable
private fun SessionStatusCard(
    state: MappingSessionUiState,
    onStartSession: () -> Unit,
    onPauseSession: () -> Unit,
    onResumeSession: () -> Unit,
    onSaveSession: () -> Unit,
    onResetSession: () -> Unit
) {
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
            Text(text = "Preferred Position Source: ${state.positionSourceType.name}")
            Text(text = "Tracking Quality: ${state.trackingQuality.name}")
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
                Button(
                    onClick = onSaveSession,
                    enabled = state.canSave,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onResetSession,
                    enabled = state.canReset,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Discard")
                }
            }
        }
    }
}

@Composable
private fun MotionDebugCard(state: MappingSessionUiState) {
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
            Text(text = "Position Status: ${state.trackingStatus}")
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
            if (state.positionSourceType == PositionSourceType.AR &&
                state.trackingQuality == TrackingQuality.GOOD
            ) {
                Text(
                    text = "AR is providing the preferred live position.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun Float.format(decimals: Int): String {
    return "%.${decimals}f".format(this)
}
