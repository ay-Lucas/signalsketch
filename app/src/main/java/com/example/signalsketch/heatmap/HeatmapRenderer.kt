package com.example.signalsketch.heatmap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.example.signalsketch.data.repo.ColorScalePreference
import com.example.signalsketch.viewmodel.RecordedPathSample
import com.example.signalsketch.viewmodel.RecordedWifiSample
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class SignalBucket {
    WEAK,
    MEDIUM,
    STRONG
}

data class MapViewportState(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

data class MapPoint(
    val offset: Offset,
    val bucket: SignalBucket
)

data class MapProjection(
    val minX: Float,
    val maxY: Float,
    val scale: Float,
    val center: Offset
)

data class HeatmapCell(
    val rect: Rect,
    val bucket: SignalBucket,
    val averageRssiDbm: Float,
    val sampleCount: Int
)

data class MapRenderModel(
    val path: List<Offset>,
    val heatmapCells: List<HeatmapCell>,
    val wifiPoints: List<MapPoint>,
    val center: Offset
)

class HeatmapRenderer {
    fun buildProjection(
        canvasSize: Size,
        pathSamples: List<RecordedPathSample>,
        wifiSamples: List<RecordedWifiSample>,
        viewport: MapViewportState
    ): MapProjection {
        val allX = pathSamples.map { it.xMeters } + wifiSamples.map { it.xMeters }
        val allY = pathSamples.map { it.yMeters } + wifiSamples.map { it.yMeters }
        val minX = allX.minOrNull() ?: -1f
        val maxX = allX.maxOrNull() ?: 1f
        val minY = allY.minOrNull() ?: -1f
        val maxY = allY.maxOrNull() ?: 1f
        val width = max(maxX - minX, 1f)
        val height = max(maxY - minY, 1f)
        val padding = 40f
        val drawableWidth = max(canvasSize.width - padding * 2f, 1f)
        val drawableHeight = max(canvasSize.height - padding * 2f, 1f)
        val scaleToFit = min(drawableWidth / width, drawableHeight / height)
        val scale = scaleToFit * viewport.scale
        val baseCenter = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        val center = Offset(
            x = baseCenter.x + viewport.offsetX,
            y = baseCenter.y + viewport.offsetY
        )

        return MapProjection(
            minX = minX,
            maxY = maxY,
            scale = scale,
            center = center
        )
    }

    fun projectPoint(
        xMeters: Float,
        yMeters: Float,
        projection: MapProjection
    ): Offset {
        return Offset(
            x = projection.center.x + (xMeters - projection.minX) * projection.scale - projection.scale / 2f,
            y = projection.center.y - (projection.maxY - yMeters) * projection.scale + projection.scale / 2f
        )
    }

    fun buildRenderModel(
        canvasSize: Size,
        pathSamples: List<RecordedPathSample>,
        wifiSamples: List<RecordedWifiSample>,
        viewport: MapViewportState
    ): MapRenderModel {
        if (canvasSize.width <= 0f || canvasSize.height <= 0f) {
            return MapRenderModel(
                path = emptyList(),
                heatmapCells = emptyList(),
                wifiPoints = emptyList(),
                center = Offset.Zero
            )
        }

        val projection = buildProjection(
            canvasSize = canvasSize,
            pathSamples = pathSamples,
            wifiSamples = wifiSamples,
            viewport = viewport
        )
        val allX = pathSamples.map { it.xMeters } + wifiSamples.map { it.xMeters }
        val allY = pathSamples.map { it.yMeters } + wifiSamples.map { it.yMeters }
        val minX = allX.minOrNull() ?: -1f
        val maxX = allX.maxOrNull() ?: 1f
        val minY = allY.minOrNull() ?: -1f
        val maxY = allY.maxOrNull() ?: 1f

        return MapRenderModel(
            path = pathSamples.map { sample ->
                projectPoint(sample.xMeters, sample.yMeters, projection)
            },
            heatmapCells = buildHeatmapCells(
                wifiSamples = wifiSamples,
                minX = minX,
                maxX = maxX,
                minY = minY,
                maxY = maxY,
                scale = projection.scale,
                center = projection.center
            ),
            wifiPoints = wifiSamples.map { sample ->
                MapPoint(
                    offset = projectPoint(sample.xMeters, sample.yMeters, projection),
                    bucket = sample.rssiDbm.toSignalBucket()
                )
            },
            center = projection.center
        )
    }

    fun colorFor(
        bucket: SignalBucket,
        colorScale: ColorScalePreference
    ): Color {
        return when (colorScale) {
            ColorScalePreference.VIRIDIS -> when (bucket) {
                SignalBucket.WEAK -> Color(0xFF424242)  // dark grey
                SignalBucket.MEDIUM -> Color(0xFFFFB300) // amber
                SignalBucket.STRONG -> Color(0xFFFFC107) // yellow accent
            }
            ColorScalePreference.TURBO -> when (bucket) {
                SignalBucket.WEAK -> Color(0xFFEF5350)  // red
                SignalBucket.MEDIUM -> Color(0xFFFFCA28) // warm yellow
                SignalBucket.STRONG -> Color(0xFFFFF176) // soft bright yellow
            }
            ColorScalePreference.GRAYSCALE -> when (bucket) {
                SignalBucket.WEAK -> Color(0xFF616161)
                SignalBucket.MEDIUM -> Color(0xFFBDBDBD)
                SignalBucket.STRONG -> Color(0xFFFFFFFF)
            }
        }
    }

    fun heatmapColorFor(
        bucket: SignalBucket,
        colorScale: ColorScalePreference
    ): Color {
        return colorFor(bucket, colorScale).copy(alpha = 0.32f)
    }

    private fun mapPoint(
        xMeters: Float,
        yMeters: Float,
        minX: Float,
        maxY: Float,
        scale: Float,
        center: Offset
    ): Offset {
        return Offset(
            x = center.x + (xMeters - minX) * scale - scale / 2f,
            y = center.y - (maxY - yMeters) * scale + scale / 2f
        )
    }

    private fun buildHeatmapCells(
        wifiSamples: List<RecordedWifiSample>,
        minX: Float,
        maxX: Float,
        minY: Float,
        maxY: Float,
        scale: Float,
        center: Offset
    ): List<HeatmapCell> {
        if (wifiSamples.isEmpty()) {
            return emptyList()
        }

        val span = max(maxX - minX, maxY - minY)
        val cellSizeMeters = (span / 8f).coerceIn(0.75f, 2.5f)
        val buckets = linkedMapOf<Pair<Int, Int>, MutableList<RecordedWifiSample>>()

        wifiSamples.forEach { sample ->
            val cellX = floor((sample.xMeters - minX) / cellSizeMeters).toInt()
            val cellY = floor((sample.yMeters - minY) / cellSizeMeters).toInt()
            buckets.getOrPut(cellX to cellY) { mutableListOf() }.add(sample)
        }

        return buckets.map { (cellKey, samples) ->
            val cellMinX = minX + cellKey.first * cellSizeMeters
            val cellMinY = minY + cellKey.second * cellSizeMeters
            val cellMaxX = min(cellMinX + cellSizeMeters, maxX + cellSizeMeters * 0.5f)
            val cellMaxY = min(cellMinY + cellSizeMeters, maxY + cellSizeMeters * 0.5f)
            val topLeft = mapPoint(cellMinX, cellMaxY, minX, maxY, scale, center)
            val bottomRight = mapPoint(cellMaxX, cellMinY, minX, maxY, scale, center)
            val averageRssi = samples.map { it.rssiDbm }.average().toFloat()

            HeatmapCell(
                rect = Rect(
                    left = topLeft.x,
                    top = topLeft.y,
                    right = bottomRight.x,
                    bottom = bottomRight.y
                ),
                bucket = averageRssi.roundedSignalBucket(),
                averageRssiDbm = averageRssi,
                sampleCount = samples.size
            )
        }
    }

    private fun Int.toSignalBucket(): SignalBucket {
        return when {
            this >= -55 -> SignalBucket.STRONG
            this >= -67 -> SignalBucket.MEDIUM
            else -> SignalBucket.WEAK
        }
    }

    private fun Float.roundedSignalBucket(): SignalBucket {
        return when {
            this >= -55f -> SignalBucket.STRONG
            this >= -67f -> SignalBucket.MEDIUM
            else -> SignalBucket.WEAK
        }
    }
}
