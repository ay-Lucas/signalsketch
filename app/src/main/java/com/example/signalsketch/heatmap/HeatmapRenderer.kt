package com.example.signalsketch.heatmap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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

        return MapRenderModel(
            path = pathSamples.map { sample ->
                mapPoint(sample.xMeters, sample.yMeters, minX, maxY, scale, center)
            },
            heatmapCells = buildHeatmapCells(
                wifiSamples = wifiSamples,
                minX = minX,
                maxX = maxX,
                minY = minY,
                maxY = maxY,
                scale = scale,
                center = center
            ),
            wifiPoints = wifiSamples.map { sample ->
                MapPoint(
                    offset = mapPoint(sample.xMeters, sample.yMeters, minX, maxY, scale, center),
                    bucket = sample.rssiDbm.toSignalBucket()
                )
            },
            center = center
        )
    }

    fun colorFor(bucket: SignalBucket): Color {
        return when (bucket) {
            SignalBucket.WEAK -> Color(0xFFC62828)
            SignalBucket.MEDIUM -> Color(0xFFF9A825)
            SignalBucket.STRONG -> Color(0xFF2E7D32)
        }
    }

    fun heatmapColorFor(bucket: SignalBucket): Color {
        return colorFor(bucket).copy(alpha = 0.28f)
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
