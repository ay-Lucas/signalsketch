package com.example.signalsketch.heatmap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.example.signalsketch.data.repo.ColorScalePreference
import com.example.signalsketch.viewmodel.FloorplanRoomBox
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

data class SignalBucketPresentation(
    val bucket: SignalBucket,
    val label: String,
    val rangeLabel: String,
    val color: Color
)

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
    val worldCenterX: Float,
    val worldCenterY: Float,
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
    fun bucketForRssi(rssiDbm: Int): SignalBucket {
        return when {
            rssiDbm >= STRONG_SIGNAL_MIN_DBM -> SignalBucket.STRONG
            rssiDbm >= MEDIUM_SIGNAL_MIN_DBM -> SignalBucket.MEDIUM
            else -> SignalBucket.WEAK
        }
    }

    fun presentationFor(bucket: SignalBucket): SignalBucketPresentation {
        return when (bucket) {
            SignalBucket.WEAK -> SignalBucketPresentation(
                bucket = bucket,
                label = "Weak",
                rangeLabel = "< -67 dBm",
                color = Color(0xFFC62828)
            )
            SignalBucket.MEDIUM -> SignalBucketPresentation(
                bucket = bucket,
                label = "Medium",
                rangeLabel = "-67 to -55 dBm",
                color = Color(0xFFF9A825)
            )
            SignalBucket.STRONG -> SignalBucketPresentation(
                bucket = bucket,
                label = "Strong",
                rangeLabel = ">= -55 dBm",
                color = Color(0xFF2E7D32)
            )
        }
    }

    fun markerColorFor(bucket: SignalBucket): Color = presentationFor(bucket).color

    fun buildProjection(
        canvasSize: Size,
        pathSamples: List<RecordedPathSample>,
        wifiSamples: List<RecordedWifiSample>,
        roomBoxes: List<FloorplanRoomBox> = emptyList(),
        viewport: MapViewportState
    ): MapProjection {
        val bounds = resolveBounds(pathSamples, wifiSamples, roomBoxes)
        val width = max(bounds.maxX - bounds.minX, 1f)
        val height = max(bounds.maxY - bounds.minY, 1f)
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
            worldCenterX = (bounds.minX + bounds.maxX) / 2f,
            worldCenterY = (bounds.minY + bounds.maxY) / 2f,
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
            x = projection.center.x + (xMeters - projection.worldCenterX) * projection.scale,
            y = projection.center.y - (yMeters - projection.worldCenterY) * projection.scale
        )
    }

    fun unprojectPoint(
        point: Offset,
        projection: MapProjection
    ): Offset {
        return Offset(
            x = ((point.x - projection.center.x) / projection.scale) + projection.worldCenterX,
            y = projection.worldCenterY - ((point.y - projection.center.y) / projection.scale)
        )
    }

    fun buildRenderModel(
        canvasSize: Size,
        pathSamples: List<RecordedPathSample>,
        wifiSamples: List<RecordedWifiSample>,
        roomBoxes: List<FloorplanRoomBox> = emptyList(),
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
            roomBoxes = roomBoxes,
            viewport = viewport
        )
        val bounds = resolveBounds(pathSamples, wifiSamples, roomBoxes)

        return MapRenderModel(
            path = pathSamples.map { sample ->
                projectPoint(sample.xMeters, sample.yMeters, projection)
            },
            heatmapCells = buildHeatmapCells(
                wifiSamples = wifiSamples,
                minX = bounds.minX,
                maxX = bounds.maxX,
                minY = bounds.minY,
                maxY = bounds.maxY,
                scale = projection.scale,
                center = projection.center
            ),
            wifiPoints = wifiSamples.map { sample ->
                MapPoint(
                    offset = projectPoint(sample.xMeters, sample.yMeters, projection),
                    bucket = bucketForRssi(sample.rssiDbm)
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
        worldCenterX: Float,
        worldCenterY: Float,
        scale: Float,
        center: Offset
    ): Offset {
        return Offset(
            x = center.x + (xMeters - worldCenterX) * scale,
            y = center.y - (yMeters - worldCenterY) * scale
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

        val worldCenterX = (minX + maxX) / 2f
        val worldCenterY = (minY + maxY) / 2f
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
            val topLeft = mapPoint(cellMinX, cellMaxY, worldCenterX, worldCenterY, scale, center)
            val bottomRight = mapPoint(cellMaxX, cellMinY, worldCenterX, worldCenterY, scale, center)
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
        return bucketForRssi(this)
    }

    private fun Float.roundedSignalBucket(): SignalBucket {
        return when {
            this >= STRONG_SIGNAL_MIN_DBM.toFloat() -> SignalBucket.STRONG
            this >= MEDIUM_SIGNAL_MIN_DBM.toFloat() -> SignalBucket.MEDIUM
            else -> SignalBucket.WEAK
        }
    }

    private fun resolveBounds(
        pathSamples: List<RecordedPathSample>,
        wifiSamples: List<RecordedWifiSample>,
        roomBoxes: List<FloorplanRoomBox>
    ): WorldBounds {
        val trackedX = pathSamples.map { it.xMeters } + wifiSamples.map { it.xMeters }
        val trackedY = pathSamples.map { it.yMeters } + wifiSamples.map { it.yMeters }
        val includeRoomsOnly = trackedX.isEmpty() && trackedY.isEmpty()
        val roomX = if (includeRoomsOnly) {
            roomBoxes.flatMap { listOf(it.centerXMeters - it.widthMeters / 2f, it.centerXMeters + it.widthMeters / 2f) }
        } else {
            emptyList()
        }
        val roomY = if (includeRoomsOnly) {
            roomBoxes.flatMap { listOf(it.centerYMeters - it.heightMeters / 2f, it.centerYMeters + it.heightMeters / 2f) }
        } else {
            emptyList()
        }
        val allX = trackedX + roomX
        val allY = trackedY + roomY
        return WorldBounds(
            minX = allX.minOrNull() ?: -1f,
            maxX = allX.maxOrNull() ?: 1f,
            minY = allY.minOrNull() ?: -1f,
            maxY = allY.maxOrNull() ?: 1f
        )
    }
}

private data class WorldBounds(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float
)

private const val STRONG_SIGNAL_MIN_DBM = -55
private const val MEDIUM_SIGNAL_MIN_DBM = -67
