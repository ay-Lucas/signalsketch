package com.example.signalsketch.ui.mapping

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.signalsketch.heatmap.HeatmapRenderer
import com.example.signalsketch.heatmap.MapViewportState
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
        MapCanvasCard(state = state)
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
private fun MapCanvasCard(state: MappingSessionUiState) {
    val renderer = remember { HeatmapRenderer() }
    var viewport by remember { mutableStateOf(MapViewportState()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Map View",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Path is drawn as connected points. Wi-Fi samples are colored by RSSI bucket.",
                style = MaterialTheme.typography.bodySmall
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Color(0xFFF6F3EC))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            viewport = viewport.copy(
                                scale = (viewport.scale * zoom).coerceIn(0.5f, 6f),
                                offsetX = viewport.offsetX + pan.x,
                                offsetY = viewport.offsetY + pan.y
                            )
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val renderModel = renderer.buildRenderModel(
                        canvasSize = size,
                        pathSamples = state.pathSamples,
                        wifiSamples = state.wifiSamples,
                        viewport = viewport
                    )

                    drawLine(
                        color = Color(0xFFB0A89C),
                        start = Offset(renderModel.center.x, 0f),
                        end = Offset(renderModel.center.x, size.height),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                    )
                    drawLine(
                        color = Color(0xFFB0A89C),
                        start = Offset(0f, renderModel.center.y),
                        end = Offset(size.width, renderModel.center.y),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                    )

                    renderModel.path.zipWithNext { start, end ->
                        drawLine(
                            color = Color(0xFF2B4C7E),
                            start = start,
                            end = end,
                            strokeWidth = 5f
                        )
                    }
                    renderModel.path.forEach { point ->
                        drawCircle(
                            color = Color(0xFF16324F),
                            radius = 5f,
                            center = point
                        )
                    }
                    renderModel.wifiPoints.forEach { point ->
                        drawCircle(
                            color = renderer.colorFor(point.bucket),
                            radius = 7f,
                            center = point.offset
                        )
                    }
                }

                if (state.pathSamples.isEmpty() && state.wifiSamples.isEmpty()) {
                    Text(
                        text = "Start a session to render the walked path and Wi-Fi samples.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            SignalLegend(renderer = renderer)
        }
    }
}

@Composable
private fun SignalLegend(renderer: HeatmapRenderer) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendItem(color = renderer.colorFor(com.example.signalsketch.heatmap.SignalBucket.WEAK), label = "Weak")
        LegendItem(color = renderer.colorFor(com.example.signalsketch.heatmap.SignalBucket.MEDIUM), label = "Medium")
        LegendItem(color = renderer.colorFor(com.example.signalsketch.heatmap.SignalBucket.STRONG), label = "Strong")
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(modifier = Modifier.padding(top = 4.dp)) {
            drawCircle(color = color, radius = 8f, center = Offset(8f, 8f))
        }
        Text(text = label, style = MaterialTheme.typography.bodySmall)
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
