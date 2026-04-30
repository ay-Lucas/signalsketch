package com.example.signalsketch.ui.mapping

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.signalsketch.heatmap.HeatmapRenderer
import com.example.signalsketch.heatmap.MapProjection
import com.example.signalsketch.heatmap.MapViewportState
import com.example.signalsketch.data.repo.ColorScalePreference
import com.example.signalsketch.data.repo.DataStoreAppPreferencesRepository
import com.example.signalsketch.storage.appPreferencesDataStore
import kotlinx.coroutines.flow.map
import com.example.signalsketch.viewmodel.FloorplanRoomBox
import com.example.signalsketch.viewmodel.RecordedPathSample
import com.example.signalsketch.viewmodel.RecordedWifiSample
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun SessionMapCard(
    pathSamples: List<RecordedPathSample>,
    wifiSamples: List<RecordedWifiSample>,
    roomBoxes: List<FloorplanRoomBox> = emptyList(),
    selectedRoomBoxId: Long? = null,
    onSelectRoomBox: (Long?) -> Unit = {},
    onMoveRoomBox: (Long, Float, Float) -> Unit = { _, _, _ -> },
    onResizeRoomBox: (Long, Float, Float) -> Unit = { _, _, _ -> },
    emptyMessage: String,
    modifier: Modifier = Modifier,
    title: String = "Map View",
    description: String = "A simple grid heatmap is derived from Wi-Fi samples, while the original path and sample points stay visible.",
    isInteractive: Boolean = true
) {
    val context = LocalContext.current
    val preferencesRepository = remember { DataStoreAppPreferencesRepository(context.appPreferencesDataStore) }
    val colorScaleFlow = remember(preferencesRepository) {
        preferencesRepository.preferences.map { it.selectedColorScale }
    }
    val colorScale by colorScaleFlow.collectAsState(initial = com.example.signalsketch.data.repo.ColorScalePreference.VIRIDIS)

    val renderer = remember { HeatmapRenderer() }
    var viewport by remember { mutableStateOf(MapViewportState()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val pathSamplesState = rememberUpdatedState(pathSamples)
    val wifiSamplesState = rememberUpdatedState(wifiSamples)
    val roomBoxesState = rememberUpdatedState(roomBoxes)
    val selectedRoomBoxIdState = rememberUpdatedState(selectedRoomBoxId)
    val viewportState = rememberUpdatedState(viewport)
    val onSelectRoomBoxState = rememberUpdatedState(onSelectRoomBox)
    val onMoveRoomBoxState = rememberUpdatedState(onMoveRoomBox)
    val onResizeRoomBoxState = rememberUpdatedState(onResizeRoomBox)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Color(0xFF101010))
                    .onSizeChanged { canvasSize = it }
                    .then(
                        if (isInteractive) {
                            Modifier.pointerInput(Unit) {
                                awaitEachGesture {
                                    val firstDown = awaitPointerEvent().changes.firstOrNull { it.pressed }
                                        ?: return@awaitEachGesture
                                    if (canvasSize == IntSize.Zero) {
                                        return@awaitEachGesture
                                    }

                                    val currentPathSamples = pathSamplesState.value
                                    val currentWifiSamples = wifiSamplesState.value
                                    val currentRoomBoxes = roomBoxesState.value
                                    val currentSelectedRoomBoxId = selectedRoomBoxIdState.value
                                    val currentViewport = viewportState.value
                                    val projection = renderer.buildProjection(
                                        canvasSize = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()),
                                        pathSamples = currentPathSamples,
                                        wifiSamples = currentWifiSamples,
                                        roomBoxes = currentRoomBoxes,
                                        viewport = currentViewport
                                    )
                                    val selectedBox = currentRoomBoxes.firstOrNull { it.id == currentSelectedRoomBoxId }
                                    val resizeHandle = selectedBox?.let { box ->
                                        hitTestRoomBoxHandle(
                                            box = box,
                                            projection = projection,
                                            renderer = renderer,
                                            point = firstDown.position
                                        )
                                    }
                                    val touchedBox = currentRoomBoxes.lastOrNull { box ->
                                        roomBoxScreenBounds(box, projection, renderer).contains(firstDown.position)
                                    }
                                    val interactionTarget = when {
                                        resizeHandle != null && selectedBox != null -> {
                                            RoomBoxInteractionTarget.Resize(
                                                boxId = selectedBox.id,
                                                handle = resizeHandle,
                                                fixedCorner = fixedCornerForHandle(selectedBox, resizeHandle)
                                            )
                                        }

                                        touchedBox != null -> RoomBoxInteractionTarget.Move(
                                            boxId = touchedBox.id,
                                            centerXMeters = touchedBox.centerXMeters,
                                            centerYMeters = touchedBox.centerYMeters
                                        )

                                        else -> RoomBoxInteractionTarget.MapPan
                                    }
                                    var didDrag = false
                                    var gestureViewport = currentViewport

                                    if (interactionTarget is RoomBoxInteractionTarget.Move) {
                                        onSelectRoomBoxState.value(interactionTarget.boxId)
                                    }

                                    do {
                                        val event = awaitPointerEvent()
                                        val pressedChanges = event.changes.filter { it.pressed }
                                        if (pressedChanges.isEmpty()) {
                                            break
                                        }

                                        val gestureProjection = renderer.buildProjection(
                                            canvasSize = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()),
                                            pathSamples = currentPathSamples,
                                            wifiSamples = currentWifiSamples,
                                            roomBoxes = currentRoomBoxes,
                                            viewport = gestureViewport
                                        )

                                        if (pressedChanges.size > 1) {
                                            val centroid = event.calculateCentroid(useCurrent = true)
                                            val pan = event.calculatePan()
                                            val zoom = event.calculateZoom()
                                            if (pan != Offset.Zero || zoom != 1f) {
                                                didDrag = true
                                                val zoomedViewport = gestureViewport.copy(
                                                    scale = (gestureViewport.scale * zoom).coerceIn(0.2f, 6f),
                                                    offsetX = gestureViewport.offsetX + pan.x,
                                                    offsetY = gestureViewport.offsetY + pan.y
                                                )
                                                val worldPointAtCentroid = renderer.unprojectPoint(
                                                    point = centroid,
                                                    projection = gestureProjection
                                                )
                                                val zoomedProjection = renderer.buildProjection(
                                                    canvasSize = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()),
                                                    pathSamples = currentPathSamples,
                                                    wifiSamples = currentWifiSamples,
                                                    roomBoxes = currentRoomBoxes,
                                                    viewport = zoomedViewport
                                                )
                                                val anchoredPoint = renderer.projectPoint(
                                                    xMeters = worldPointAtCentroid.x,
                                                    yMeters = worldPointAtCentroid.y,
                                                    projection = zoomedProjection
                                                )
                                                gestureViewport = zoomedViewport.copy(
                                                    offsetX = zoomedViewport.offsetX + (centroid.x - anchoredPoint.x),
                                                    offsetY = zoomedViewport.offsetY + (centroid.y - anchoredPoint.y)
                                                )
                                                viewport = gestureViewport
                                            }
                                        } else {
                                            when (interactionTarget) {
                                                is RoomBoxInteractionTarget.Move -> {
                                                    val change = pressedChanges.first()
                                                    val delta = gestureProjection.deltaMeters(change)
                                                    if (delta != Offset.Zero) {
                                                        didDrag = true
                                                        interactionTarget.centerXMeters += delta.x
                                                        interactionTarget.centerYMeters += delta.y
                                                        onMoveRoomBoxState.value(
                                                            interactionTarget.boxId,
                                                            interactionTarget.centerXMeters,
                                                            interactionTarget.centerYMeters
                                                        )
                                                    }
                                                }

                                                is RoomBoxInteractionTarget.Resize -> {
                                                    val change = pressedChanges.first()
                                                    val dragged = renderer.unprojectPoint(change.position, gestureProjection)
                                                    val resized = resizeBoxFromHandle(
                                                        draggedPoint = dragged,
                                                        fixedCorner = interactionTarget.fixedCorner,
                                                        handle = interactionTarget.handle
                                                    )
                                                    if (resized != null) {
                                                        didDrag = true
                                                        onMoveRoomBoxState.value(
                                                            interactionTarget.boxId,
                                                            resized.centerX,
                                                            resized.centerY
                                                        )
                                                        onResizeRoomBoxState.value(
                                                            interactionTarget.boxId,
                                                            resized.width,
                                                            resized.height
                                                        )
                                                    }
                                                }

                                                RoomBoxInteractionTarget.MapPan -> {
                                                    val change = pressedChanges.first()
                                                    val delta = change.position - change.previousPosition
                                                    if (delta != Offset.Zero) {
                                                        didDrag = true
                                                        gestureViewport = gestureViewport.copy(
                                                            offsetX = gestureViewport.offsetX + delta.x,
                                                            offsetY = gestureViewport.offsetY + delta.y
                                                        )
                                                        viewport = gestureViewport
                                                    }
                                                }
                                            }
                                        }

                                        event.changes.forEach { change ->
                                            if (change.position != change.previousPosition) {
                                                change.consume()
                                            }
                                        }
                                    } while (true)

                                    if (!didDrag) {
                                        when {
                                            resizeHandle != null && selectedBox != null -> {
                                                onSelectRoomBoxState.value(selectedBox.id)
                                            }

                                            touchedBox != null -> {
                                                onSelectRoomBoxState.value(touchedBox.id)
                                            }

                                            else -> {
                                                onSelectRoomBoxState.value(null)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val renderModel = renderer.buildRenderModel(
                        canvasSize = size,
                        pathSamples = pathSamples,
                        wifiSamples = wifiSamples,
                        roomBoxes = roomBoxes,
                        viewport = viewport
                    )
                    val projection = renderer.buildProjection(
                        canvasSize = size,
                        pathSamples = pathSamples,
                        wifiSamples = wifiSamples,
                        roomBoxes = roomBoxes,
                        viewport = viewport
                    )

                    drawLine(
                        color = Color(0xFF424242),
                        start = Offset(renderModel.center.x, 0f),
                        end = Offset(renderModel.center.x, size.height),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                    )
                    drawLine(
                        color = Color(0xFF424242),
                        start = Offset(0f, renderModel.center.y),
                        end = Offset(size.width, renderModel.center.y),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                    )

                    renderModel.heatmapCells.forEach { cell ->
                        drawRect(
                            color = renderer.heatmapColorFor(cell.bucket, colorScale),
                            topLeft = cell.rect.topLeft,
                            size = cell.rect.size
                        )
                    }

                    renderModel.path.zipWithNext { start, end ->
                        drawLine(
                            color = Color(0xFFFFC107),
                            start = start,
                            end = end,
                            strokeWidth = 5f
                        )
                    }
                    renderModel.path.forEach { point ->
                        drawCircle(
                            color = Color(0xFFFFF9C4),
                            radius = 5f,
                            center = point
                        )
                    }
                    renderModel.path.firstOrNull()?.let { origin ->
                        drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = 11f,
                            center = origin
                        )
                        drawCircle(
                            color = Color(0xFF101010),
                            radius = 5f,
                            center = origin
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 14f,
                            center = origin,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                        )
                    }
                    renderModel.wifiPoints.forEach { point ->
                        drawCircle(
                            color = renderer.colorFor(point.bucket, colorScale),
                            radius = 7f,
                            center = point.offset
                        )
                    }

                    val labelPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 28f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                    }
                    val startLabelPaint = android.graphics.Paint(labelPaint).apply {
                        color = android.graphics.Color.CYAN
                        textSize = 30f
                    }

                    renderModel.path.firstOrNull()?.let { origin ->
                        drawContext.canvas.nativeCanvas.drawText(
                            "START",
                            origin.x + 18f,
                            origin.y - 18f,
                            startLabelPaint
                        )
                    }

                    roomBoxes.forEach { box ->
                        val topLeft = renderer.projectPoint(
                            xMeters = box.centerXMeters - box.widthMeters / 2f,
                            yMeters = box.centerYMeters + box.heightMeters / 2f,
                            projection = projection
                        )
                        val bottomRight = renderer.projectPoint(
                            xMeters = box.centerXMeters + box.widthMeters / 2f,
                            yMeters = box.centerYMeters - box.heightMeters / 2f,
                            projection = projection
                        )
                        val boxColor = Color(box.colorArgb)
                        val left = min(topLeft.x, bottomRight.x)
                        val top = min(topLeft.y, bottomRight.y)
                        val right = max(topLeft.x, bottomRight.x)
                        val bottom = max(topLeft.y, bottomRight.y)
                        val rectTopLeft = Offset(left, top)
                        val rectSize = Size(
                            width = right - left,
                            height = bottom - top
                        )
                        val isSelected = box.id == selectedRoomBoxId

                        drawRect(
                            color = boxColor.copy(alpha = 0.22f),
                            topLeft = rectTopLeft,
                            size = rectSize
                        )
                        drawRect(
                            color = if (isSelected) Color.White else boxColor,
                            topLeft = rectTopLeft,
                            size = rectSize,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = if (isSelected) 5f else 3f
                            )
                        )
                        if (isSelected) {
                            drawRect(
                                color = boxColor,
                                topLeft = rectTopLeft,
                                size = rectSize,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                            )
                            roomBoxHandleCenters(box, projection, renderer).forEach { handle ->
                                drawCircle(
                                    color = Color.White,
                                    radius = 10f,
                                    center = handle.center
                                )
                                drawCircle(
                                    color = boxColor,
                                    radius = 6f,
                                    center = handle.center
                                )
                            }
                        }

                        drawContext.canvas.nativeCanvas.drawText(
                            box.label,
                            rectTopLeft.x + 10f,
                            rectTopLeft.y + 30f,
                            labelPaint
                        )
                    }
                }

                if (pathSamples.isEmpty() && wifiSamples.isEmpty()) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            SignalLegend(renderer = renderer, colorScale = colorScale)
        }
    }
}

private sealed interface RoomBoxInteractionTarget {
    data class Move(
        val boxId: Long,
        var centerXMeters: Float,
        var centerYMeters: Float
    ) : RoomBoxInteractionTarget

    data class Resize(
        val boxId: Long,
        val handle: RoomBoxHandle,
        val fixedCorner: Offset
    ) : RoomBoxInteractionTarget

    data object MapPan : RoomBoxInteractionTarget
}

private enum class RoomBoxHandle(
    val xDirection: Float,
    val yDirection: Float
) {
    TOP_LEFT(-1f, 1f),
    TOP_RIGHT(1f, 1f),
    BOTTOM_LEFT(-1f, -1f),
    BOTTOM_RIGHT(1f, -1f)
}

private data class RoomBoxHandlePosition(
    val handle: RoomBoxHandle,
    val center: Offset
)

private data class RoomBoxScreenBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun contains(point: Offset): Boolean {
        val hitPaddingPx = 20f
        return point.x in (left - hitPaddingPx)..(right + hitPaddingPx) &&
            point.y in (top - hitPaddingPx)..(bottom + hitPaddingPx)
    }
}

private data class ResizedRoomBox(
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float
)

private fun roomBoxScreenBounds(
    box: FloorplanRoomBox,
    projection: MapProjection,
    renderer: HeatmapRenderer
): RoomBoxScreenBounds {
    val topLeft = renderer.projectPoint(
        xMeters = box.centerXMeters - box.widthMeters / 2f,
        yMeters = box.centerYMeters + box.heightMeters / 2f,
        projection = projection
    )
    val bottomRight = renderer.projectPoint(
        xMeters = box.centerXMeters + box.widthMeters / 2f,
        yMeters = box.centerYMeters - box.heightMeters / 2f,
        projection = projection
    )
    return RoomBoxScreenBounds(
        left = min(topLeft.x, bottomRight.x),
        top = min(topLeft.y, bottomRight.y),
        right = max(topLeft.x, bottomRight.x),
        bottom = max(topLeft.y, bottomRight.y)
    )
}

private fun roomBoxHandleCenters(
    box: FloorplanRoomBox,
    projection: MapProjection,
    renderer: HeatmapRenderer
): List<RoomBoxHandlePosition> {
    val bounds = roomBoxScreenBounds(box, projection, renderer)
    return listOf(
        RoomBoxHandlePosition(RoomBoxHandle.TOP_LEFT, Offset(bounds.left, bounds.top)),
        RoomBoxHandlePosition(RoomBoxHandle.TOP_RIGHT, Offset(bounds.right, bounds.top)),
        RoomBoxHandlePosition(RoomBoxHandle.BOTTOM_LEFT, Offset(bounds.left, bounds.bottom)),
        RoomBoxHandlePosition(RoomBoxHandle.BOTTOM_RIGHT, Offset(bounds.right, bounds.bottom))
    )
}

private fun hitTestRoomBoxHandle(
    box: FloorplanRoomBox,
    projection: MapProjection,
    renderer: HeatmapRenderer,
    point: Offset
): RoomBoxHandle? {
    val hitRadiusPx = 28f
    return roomBoxHandleCenters(box, projection, renderer)
        .firstOrNull { handle ->
            abs(handle.center.x - point.x) <= hitRadiusPx &&
                abs(handle.center.y - point.y) <= hitRadiusPx
        }
        ?.handle
}

private fun fixedCornerForHandle(
    box: FloorplanRoomBox,
    handle: RoomBoxHandle
): Offset {
    val oppositeX = box.centerXMeters - handle.xDirection * (box.widthMeters / 2f)
    val oppositeY = box.centerYMeters - handle.yDirection * (box.heightMeters / 2f)
    return Offset(oppositeX, oppositeY)
}

private fun resizeBoxFromHandle(
    draggedPoint: Offset,
    fixedCorner: Offset,
    handle: RoomBoxHandle,
    minSizeMeters: Float = 0.05f
): ResizedRoomBox? {
    val resolvedX = if (handle.xDirection > 0f) {
        max(draggedPoint.x, fixedCorner.x + minSizeMeters)
    } else {
        min(draggedPoint.x, fixedCorner.x - minSizeMeters)
    }
    val resolvedY = if (handle.yDirection > 0f) {
        max(draggedPoint.y, fixedCorner.y + minSizeMeters)
    } else {
        min(draggedPoint.y, fixedCorner.y - minSizeMeters)
    }

    val width = abs(resolvedX - fixedCorner.x)
    val height = abs(resolvedY - fixedCorner.y)
    if (width <= 0f || height <= 0f) {
        return null
    }

    return ResizedRoomBox(
        centerX = (resolvedX + fixedCorner.x) / 2f,
        centerY = (resolvedY + fixedCorner.y) / 2f,
        width = width,
        height = height
    )
}

private fun MapProjection.deltaMeters(change: PointerInputChange): Offset {
    return Offset(
        x = (change.position.x - change.previousPosition.x) / scale,
        y = -(change.position.y - change.previousPosition.y) / scale
    )
}

@Composable
private fun SignalLegend(
    renderer: HeatmapRenderer,
    colorScale: ColorScalePreference
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendItem(color = renderer.colorFor(com.example.signalsketch.heatmap.SignalBucket.WEAK, colorScale), label = "Weak")
        LegendItem(color = renderer.colorFor(com.example.signalsketch.heatmap.SignalBucket.MEDIUM, colorScale), label = "Medium")
        LegendItem(color = renderer.colorFor(com.example.signalsketch.heatmap.SignalBucket.STRONG, colorScale), label = "Strong")
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
