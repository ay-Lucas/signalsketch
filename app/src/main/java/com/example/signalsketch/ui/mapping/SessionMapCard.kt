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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.signalsketch.heatmap.HeatmapRenderer
import com.example.signalsketch.heatmap.MapViewportState
import com.example.signalsketch.data.repo.ColorScalePreference
import com.example.signalsketch.data.repo.DataStoreAppPreferencesRepository
import com.example.signalsketch.storage.appPreferencesDataStore
import kotlinx.coroutines.flow.map
import com.example.signalsketch.viewmodel.FloorplanRoomBox
import com.example.signalsketch.viewmodel.RecordedPathSample
import com.example.signalsketch.viewmodel.RecordedWifiSample
import kotlin.math.max
import kotlin.math.min

@Composable
fun SessionMapCard(
    pathSamples: List<RecordedPathSample>,
    wifiSamples: List<RecordedWifiSample>,
    roomBoxes: List<FloorplanRoomBox> = emptyList(),
    emptyMessage: String,
    modifier: Modifier = Modifier,
    title: String = "Map View",
    description: String = "A simple grid heatmap is derived from Wi-Fi samples, while the original path and sample points stay visible."
) {
    val context = LocalContext.current
    val preferencesRepository = remember { DataStoreAppPreferencesRepository(context.appPreferencesDataStore) }
    val colorScale by preferencesRepository.preferences
        .map { it.selectedColorScale }
        .collectAsState(initial = com.example.signalsketch.data.repo.ColorScalePreference.VIRIDIS)

    val renderer = remember { HeatmapRenderer() }
    var viewport by remember { mutableStateOf(MapViewportState()) }

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
                        pathSamples = pathSamples,
                        wifiSamples = wifiSamples,
                        viewport = viewport
                    )
                    val projection = renderer.buildProjection(
                        canvasSize = size,
                        pathSamples = pathSamples,
                        wifiSamples = wifiSamples,
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

                        drawRect(
                            color = boxColor.copy(alpha = 0.22f),
                            topLeft = rectTopLeft,
                            size = rectSize
                        )
                        drawRect(
                            color = boxColor,
                            topLeft = rectTopLeft,
                            size = rectSize,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                        )

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
