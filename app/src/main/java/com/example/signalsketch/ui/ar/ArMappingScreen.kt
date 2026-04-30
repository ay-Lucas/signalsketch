package com.example.signalsketch.ui.ar

import android.app.Activity
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.signalsketch.ui.mapping.FloorplanBuilderCard
import com.example.signalsketch.ui.mapping.SessionMapCard
import com.example.signalsketch.ui.theme.OnDark
import com.example.signalsketch.ui.theme.SurfaceDark
import com.example.signalsketch.position.PositionSourceType
import com.example.signalsketch.position.TrackingQuality
import com.example.signalsketch.viewmodel.ArMappingUiState
import com.example.signalsketch.viewmodel.ArMappingViewModel
import com.google.ar.core.Config
import com.google.ar.core.Frame
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener

@Composable
fun ArMappingScreen(
    viewModel: ArMappingViewModel,
    onOpenStandardMapping: () -> Unit = {}
) {
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
        activity = activity,
        onRefresh = viewModel::refresh,
        onRequestCameraPermission = {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        },
        onRequestArInstall = {
            activity?.let(viewModel::requestArInstall)
        },
        onSessionCreated = viewModel::onArSessionCreated,
        onSessionResumed = viewModel::onArSessionResumed,
        onSessionPaused = viewModel::onArSessionPaused,
        onSessionFailed = viewModel::onArSessionFailed,
        onScreenDisposed = viewModel::onScreenDisposed,
        onFrameUpdated = viewModel::onArFrameUpdated,
        onTapFrame = viewModel::onArTap,
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
        onRemoveFloorplanBox = viewModel::removeFloorplanBox,
        onOpenStandardMapping = onOpenStandardMapping
    )
}

@Composable
private fun ArMappingScreen(
    state: ArMappingUiState,
    activity: Activity?,
    onRefresh: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRequestArInstall: () -> Unit,
    onSessionCreated: () -> Unit,
    onSessionResumed: () -> Unit,
    onSessionPaused: () -> Unit,
    onSessionFailed: (String?) -> Unit,
    onScreenDisposed: () -> Unit,
    onFrameUpdated: (Frame) -> Unit,
    onTapFrame: (Frame, android.view.MotionEvent) -> com.google.ar.core.Anchor?,
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
    onRemoveFloorplanBox: (Long) -> Unit,
    onOpenStandardMapping: () -> Unit = {}
) {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val cameraStream = rememberARCameraStream(materialLoader)
    val currentFrame = remember { mutableStateOf<Frame?>(null) }
    val anchorNodes = rememberNodes()
    val pathMarkerNodes = rememberNodes()
    val wifiMarkerNodes = rememberNodes()
    var selectedBoxId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(state.pathSamples) {
        pathMarkerNodes.clear()
        state.pathSamples.forEachIndexed { index, sample ->
            val isStart = index == 0
            pathMarkerNodes += CylinderNode(
                engine = engine,
                radius = if (isStart) 0.05f else 0.03f,
                height = 0.01f,
                materialInstance = materialLoader.createColorInstance(
                    color = if (isStart) Color(0xFF00E5FF) else Color(0xFFFFF9C4)
                )
            ).apply {
                position = Float3(sample.xMeters, 0.005f, sample.yMeters)
            }
        }
    }

    LaunchedEffect(state.liveSampleMarkers) {
        wifiMarkerNodes.clear()
        state.liveSampleMarkers.forEach { marker ->
            wifiMarkerNodes += CubeNode(
                engine = engine,
                size = Float3(0.08f, 0.08f, 0.08f),
                materialInstance = materialLoader.createColorInstance(marker.colorArgb)
            ).apply {
                position = Float3(marker.xMeters, 0.04f, marker.yMeters)
            }
        }
    }

    LaunchedEffect(state.availability.canStartAr) {
        if (!state.availability.canStartAr) {
            currentFrame.value = null
            anchorNodes.clear()
            pathMarkerNodes.clear()
            wifiMarkerNodes.clear()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            currentFrame.value = null
            anchorNodes.clear()
            pathMarkerNodes.clear()
            wifiMarkerNodes.clear()
            onScreenDisposed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.availability.canStartAr && activity != null) {
            ARScene(
                modifier = Modifier.fillMaxSize(),
                planeRenderer = true,
                cameraStream = cameraStream,
                sessionConfiguration = { session, config ->
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    config.depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        Config.DepthMode.AUTOMATIC
                    } else {
                        Config.DepthMode.DISABLED
                    }
                },
                childNodes = anchorNodes + pathMarkerNodes + wifiMarkerNodes,
                onSessionCreated = { onSessionCreated() },
                onSessionResumed = { onSessionResumed() },
                onSessionPaused = { onSessionPaused() },
                onSessionFailed = { exception -> onSessionFailed(exception.message) },
                onSessionUpdated = { _, frame ->
                    currentFrame.value = frame
                    onFrameUpdated(frame)
                },
                onGestureListener = rememberOnGestureListener(
                    onSingleTapConfirmed = { motionEvent, _ ->
                        val frame = currentFrame.value ?: return@rememberOnGestureListener
                        val anchor = onTapFrame(frame, motionEvent) ?: return@rememberOnGestureListener
                        anchorNodes += AnchorNode(
                            engine = engine,
                            anchor = anchor
                        ).apply {
                            addChildNode(
                                CylinderNode(
                                    engine = engine,
                                    radius = 0.06f,
                                    height = 0.01f,
                                    materialInstance = materialLoader.createColorInstance(
                                        color = Color(0xFF00C853)
                                    )
                                ).apply {
                                    position = Float3(y = 0.005f)
                                }
                            )
                        }
                    }
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusCard(
                state = state,
                onRefresh = onRefresh,
                onRequestCameraPermission = onRequestCameraPermission,
                onRequestArInstall = onRequestArInstall,
                onStartSession = onStartSession,
                onPauseSession = onPauseSession,
                onResumeSession = onResumeSession,
                onSaveSession = onSaveSession,
                onResetSession = onResetSession,
                onOpenStandardMapping = onOpenStandardMapping
            )
            if (state.availability.canStartAr) {
                PlaneIndicatorCard(
                    hasDetectedHorizontalPlane = state.hasDetectedHorizontalPlane,
                    anchorCount = state.anchorCount,
                    wifiSampleCount = state.wifiSampleCount
                )
            }
            SessionMapCard(
                pathSamples = state.pathSamples,
                wifiSamples = state.wifiSamples,
                roomBoxes = state.floorplanBoxes,
                selectedRoomBoxId = selectedBoxId,
                onSelectRoomBox = { selectedBoxId = it },
                onMoveRoomBox = onUpdateFloorplanBoxPosition,
                onResizeRoomBox = { boxId, widthMeters, heightMeters ->
                    onUpdateFloorplanBoxWidth(boxId, widthMeters)
                    onUpdateFloorplanBoxHeight(boxId, heightMeters)
                },
                emptyMessage = "Start an AR session to capture path samples, Wi-Fi samples, and floorplan boxes.",
                title = "AR Floorplan Map",
                description = "This 2D map mirrors the AR capture session so you can place and edit floorplan boxes while recording."
            )
            FloorplanBuilderCard(
                boxes = state.floorplanBoxes,
                selectedBoxId = selectedBoxId,
                onAddBox = onAddFloorplanBox,
                onUpdateBoxLabel = onUpdateFloorplanBoxLabel,
                onSelectBox = { selectedBoxId = it },
                onRemoveBox = onRemoveFloorplanBox,
                title = "AR Floorplan Builder",
                description = "Manage the same room boxes used by the live AR mapping session. Select a box on the map to move or resize it."
            )
        }
    }
}

@Composable
private fun StatusCard(
    state: ArMappingUiState,
    onRefresh: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRequestArInstall: () -> Unit,
    onStartSession: () -> Unit,
    onPauseSession: () -> Unit,
    onResumeSession: () -> Unit,
    onSaveSession: () -> Unit,
    onResetSession: () -> Unit,
    onOpenStandardMapping: () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "AR Mapping",
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = "Device Support: ${state.availability.supportState.name}")
            Text(text = "Install / Update Status: ${state.availability.installState.name}")
            Text(text = "Camera Permission: ${if (state.availability.hasCameraPermission) "Granted" else "Missing"}")
            Text(text = "Can Start AR: ${if (state.availability.canStartAr) "Yes" else "No"}")
            Text(text = "AR Session: ${state.sessionLifecycleState.name}")
            Text(text = "Recording Session: ${state.recordingSessionState.name}")
            Text(text = "Position Source: ${state.preferredPositionSource.name}")
            Text(text = "Tracking Quality: ${state.trackingQuality.name}")
            if (state.lastErrorMessage != null) {
                Text(
                    text = "AR Error: ${state.lastErrorMessage}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = state.statusMessage ?: state.availability.statusMessage,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            state.fallbackMessage?.let { fallbackMessage ->
                Text(
                    text = fallbackMessage,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (state.trackingQuality == TrackingQuality.LIMITED) {
                Text(
                    text = "Move slowly, keep the floor in view, and avoid quick turns.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (state.trackingQuality == TrackingQuality.UNAVAILABLE &&
                state.preferredPositionSource != PositionSourceType.SENSORS
            ) {
                Text(
                    text = "Tracking is lost. Repoint at the floor and improve lighting.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRequestCameraPermission,
                    modifier = Modifier.weight(1f),
                    enabled = !state.availability.hasCameraPermission
                ) {
                    Text("Grant Camera")
                }
                OutlinedButton(
                    onClick = onRequestArInstall,
                    modifier = Modifier.weight(1f),
                    enabled = state.availability.supportState == com.example.signalsketch.ar.ArSupportState.SUPPORTED &&
                        state.availability.installState != com.example.signalsketch.ar.ArInstallState.READY
                ) {
                    Text("Install / Update AR")
                }
            }
            if (!state.availability.canStartAr || state.preferredPositionSource == PositionSourceType.SENSORS) {
                OutlinedButton(
                    onClick = onOpenStandardMapping,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use Standard Mapping")
                }
            }
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh AR Status")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartSession,
                    enabled = state.canStartSession && state.availability.canStartAr,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start")
                }
                OutlinedButton(
                    onClick = onPauseSession,
                    enabled = state.canPauseSession,
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
                    enabled = state.canResumeSession,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Resume")
                }
                Button(
                    onClick = onSaveSession,
                    enabled = state.canSaveSession,
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
                    enabled = state.canResetSession,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Discard")
                }
            }
        }
    }
}

@Composable
private fun PlaneIndicatorCard(
    hasDetectedHorizontalPlane: Boolean,
    anchorCount: Int,
    wifiSampleCount: Int
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "AR Session Debug",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (hasDetectedHorizontalPlane) {
                    "Horizontal plane detected. Tap the floor to place an anchor."
                } else {
                    "Searching for a horizontal plane..."
                }
            )
            Text(text = "Anchors Placed: $anchorCount")
            Text(text = "Wi-Fi Samples Captured: $wifiSampleCount")
            Text(text = "Signal markers:")
            Text(text = "Strong: >= -55 dBm", style = MaterialTheme.typography.bodySmall)
            Text(text = "Medium: -67 to -55 dBm", style = MaterialTheme.typography.bodySmall)
            Text(text = "Weak: < -67 dBm", style = MaterialTheme.typography.bodySmall)
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
