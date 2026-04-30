package com.example.signalsketch.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.ui.geometry.Size
import com.example.signalsketch.data.repo.ColorScalePreference
import com.example.signalsketch.data.repo.DataStoreAppPreferencesRepository
import com.example.signalsketch.data.repo.SavedSessionDetail
import com.example.signalsketch.heatmap.HeatmapRenderer
import com.example.signalsketch.heatmap.MapViewportState
import com.example.signalsketch.storage.appPreferencesDataStore
import com.example.signalsketch.viewmodel.FloorplanRoomBox
import com.example.signalsketch.viewmodel.RecordedPathSample
import com.example.signalsketch.viewmodel.RecordedWifiSample
import com.example.signalsketch.data.repo.SavedSessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class SharedSessionExport(
    val imageUri: Uri,
    val dataUri: Uri,
    val imageMimeType: String = "image/png",
    val dataMimeType: String = "application/json"
)

interface SessionExportManager {
    suspend fun exportSession(session: SavedSessionDetail): SharedSessionExport
}

object SessionExportManagerFactory {
    @Volatile
    private var manager: SessionExportManager? = null

    fun create(context: Context): SessionExportManager {
        return manager ?: synchronized(this) {
            manager ?: FileSessionExportManager(context.applicationContext).also { manager = it }
        }
    }
}

private class FileSessionExportManager(
    private val context: Context
) : SessionExportManager {
    override suspend fun exportSession(session: SavedSessionDetail): SharedSessionExport {
        return withContext(Dispatchers.IO) {
            // Cache keeps exports shareable without requesting broad storage access.
            val exportDir = File(context.cacheDir, "shared_exports").apply {
                mkdirs()
            }
            exportDir.listFiles()?.forEach(File::delete)

            val safeName = session.summary.name
                .lowercase(Locale.US)
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .ifBlank { "session_${session.summary.sessionId}" }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(Date(System.currentTimeMillis()))
            val baseName = "${safeName}_$timestamp"

            val previewFile = File(exportDir, "$baseName.png")
            val dataFile = File(exportDir, "$baseName.json")
            val colorScale = DataStoreAppPreferencesRepository(context.appPreferencesDataStore)
                .preferences
                .first()
                .selectedColorScale

            FileOutputStream(previewFile).use { stream ->
                buildPreviewBitmap(
                    session = session,
                    colorScale = colorScale
                ).compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            dataFile.writeText(buildExportJson(session).toString(2))

            SharedSessionExport(
                imageUri = previewFile.toContentUri(context),
                dataUri = dataFile.toContentUri(context)
            )
        }
    }
}

private fun buildPreviewBitmap(
    session: SavedSessionDetail,
    colorScale: ColorScalePreference
): Bitmap {
    val width = 1440
    val height = 1440
    val padding = 120f
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.rgb(246, 243, 236))

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(22, 50, 79)
        textSize = 54f
        isFakeBoldText = true
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(54, 64, 79)
        textSize = 34f
    }
    val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(66, 66, 66)
        strokeWidth = 3f
    }
    val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 193, 7)
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }
    val pathPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 249, 196)
        style = Paint.Style.FILL
    }
    val startOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    val startInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 229, 255)
        style = Paint.Style.FILL
    }
    val startCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(16, 16, 16)
        style = Paint.Style.FILL
    }
    val roomLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        isFakeBoldText = true
    }
    val startLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        textSize = 30f
        isFakeBoldText = true
    }

    canvas.drawText(session.summary.name, padding, 88f, titlePaint)
    canvas.drawText(
        "${session.summary.status.label}  •  ${session.summary.pathPointCount} path points  •  ${session.summary.wifiSampleCount} Wi-Fi samples",
        padding,
        138f,
        bodyPaint
    )

    val plotLeft = padding
    val plotTop = 210f
    val plotRight = width - padding
    val plotBottom = height - padding
    val plotWidth = plotRight - plotLeft
    val plotHeight = plotBottom - plotTop
    val mapBitmap = Bitmap.createBitmap(plotWidth.toInt(), plotHeight.toInt(), Bitmap.Config.ARGB_8888)
    val mapCanvas = Canvas(mapBitmap)
    mapCanvas.drawColor(Color.rgb(16, 16, 16))

    val renderer = HeatmapRenderer()
    val pathSamples = session.pathPoints.mapIndexed { index, point ->
        RecordedPathSample(
            index = index,
            xMeters = point.xMeters,
            yMeters = point.yMeters,
            headingDegrees = point.headingDegrees ?: 0f,
            sensorSampleCount = index + 1,
            recordedAtEpochMillis = point.recordedAtEpochMillis
        )
    }
    val wifiSamples = session.wifiSamples.map { sample ->
        RecordedWifiSample(
            bssid = sample.bssid,
            ssid = sample.ssid,
            rssiDbm = sample.rssiDbm,
            frequencyMhz = sample.frequencyMhz,
            timestampMicros = sample.sampledAtEpochMillis * 1_000L,
            xMeters = sample.xMeters ?: 0f,
            yMeters = sample.yMeters ?: 0f,
            headingDegrees = sample.headingDegrees ?: 0f,
            pathSampleIndex = null,
            recordedAtEpochMillis = sample.sampledAtEpochMillis
        )
    }
    val roomBoxes = session.floorplanBoxes.map { box ->
        FloorplanRoomBox(
            id = box.boxId,
            label = box.label,
            centerXMeters = box.centerXMeters,
            centerYMeters = box.centerYMeters,
            widthMeters = box.widthMeters,
            heightMeters = box.heightMeters,
            colorArgb = box.colorArgb
        )
    }
    val renderModel = renderer.buildRenderModel(
        canvasSize = Size(plotWidth, plotHeight),
        pathSamples = pathSamples,
        wifiSamples = wifiSamples,
        roomBoxes = roomBoxes,
        viewport = MapViewportState()
    )
    val projection = renderer.buildProjection(
        canvasSize = Size(plotWidth, plotHeight),
        pathSamples = pathSamples,
        wifiSamples = wifiSamples,
        roomBoxes = roomBoxes,
        viewport = MapViewportState()
    )

    mapCanvas.drawLine(renderModel.center.x, 0f, renderModel.center.x, plotHeight, axisPaint)
    mapCanvas.drawLine(0f, renderModel.center.y, plotWidth, renderModel.center.y, axisPaint)

    renderModel.heatmapCells.forEach { cell ->
        val color = renderer.heatmapColorFor(cell.bucket, colorScale).toArgb()
        mapCanvas.drawRect(
            cell.rect.left,
            cell.rect.top,
            cell.rect.right,
            cell.rect.bottom,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }
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
        val left = min(topLeft.x, bottomRight.x)
        val top = min(topLeft.y, bottomRight.y)
        val right = max(topLeft.x, bottomRight.x)
        val bottom = max(topLeft.y, bottomRight.y)
        val boxColor = box.colorArgb
        mapCanvas.drawRect(
            RectF(left, top, right, bottom),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = withAlpha(boxColor, 56)
                style = Paint.Style.FILL
            }
        )
        mapCanvas.drawRect(
            RectF(left, top, right, bottom),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = boxColor
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
        )
        mapCanvas.drawText(
            box.label,
            left + 10f,
            top + 30f,
            roomLabelPaint
        )
    }

    renderModel.path.zipWithNext { start, end ->
        mapCanvas.drawLine(start.x, start.y, end.x, end.y, pathPaint)
    }
    renderModel.path.forEach { point ->
        mapCanvas.drawCircle(point.x, point.y, 5f, pathPointPaint)
    }
    renderModel.path.firstOrNull()?.let { origin ->
        mapCanvas.drawCircle(origin.x, origin.y, 14f, startOuterPaint)
        mapCanvas.drawCircle(origin.x, origin.y, 11f, startInnerPaint)
        mapCanvas.drawCircle(origin.x, origin.y, 5f, startCorePaint)
        mapCanvas.drawText("START", origin.x + 18f, origin.y - 18f, startLabelPaint)
    }
    renderModel.wifiPoints.forEach { point ->
        mapCanvas.drawCircle(
            point.offset.x,
            point.offset.y,
            7f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = renderer.markerColorFor(point.bucket).toArgb()
                style = Paint.Style.FILL
            }
        )
    }

    canvas.drawBitmap(mapBitmap, plotLeft, plotTop, null)

    return bitmap
}

private fun buildExportJson(session: SavedSessionDetail): JSONObject {
    return JSONObject().apply {
        put("sessionId", session.summary.sessionId)
        put("name", session.summary.name)
        put("status", session.summary.status.name)
        put("startedAtEpochMillis", session.summary.startedAtEpochMillis)
        put("endedAtEpochMillis", session.summary.endedAtEpochMillis)
        put("notes", session.summary.notes)
        put("pathPointCount", session.summary.pathPointCount)
        put("wifiSampleCount", session.summary.wifiSampleCount)
        put(
            "pathPoints",
            JSONArray().apply {
                session.pathPoints.forEach { point ->
                    put(
                        JSONObject().apply {
                            put("pointId", point.pointId)
                            put("xMeters", point.xMeters)
                            put("yMeters", point.yMeters)
                            put("headingDegrees", point.headingDegrees)
                            put("recordedAtEpochMillis", point.recordedAtEpochMillis)
                        }
                    )
                }
            }
        )
        put(
            "wifiSamples",
            JSONArray().apply {
                session.wifiSamples.forEach { sample ->
                    put(
                        JSONObject().apply {
                            put("sampleId", sample.sampleId)
                            put("ssid", sample.ssid)
                            put("bssid", sample.bssid)
                            put("rssiDbm", sample.rssiDbm)
                            put("frequencyMhz", sample.frequencyMhz)
                            put("sampledAtEpochMillis", sample.sampledAtEpochMillis)
                            put("xMeters", sample.xMeters)
                            put("yMeters", sample.yMeters)
                            put("headingDegrees", sample.headingDegrees)
                            put("pathPointId", sample.pathPointId)
                        }
                    )
                }
            }
        )
        put(
            "floorplanBoxes",
            JSONArray().apply {
                session.floorplanBoxes.forEach { box ->
                    put(
                        JSONObject().apply {
                            put("boxId", box.boxId)
                            put("label", box.label)
                            put("centerXMeters", box.centerXMeters)
                            put("centerYMeters", box.centerYMeters)
                            put("widthMeters", box.widthMeters)
                            put("heightMeters", box.heightMeters)
                            put("colorArgb", box.colorArgb)
                        }
                    )
                }
            }
        )
    }
}

private val SavedSessionStatus.label: String
    get() = when (this) {
        SavedSessionStatus.PAUSED -> "Paused"
        SavedSessionStatus.COMPLETED -> "Completed"
    }

private fun androidx.compose.ui.graphics.Color.toArgb(): Int {
    return Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}

private fun withAlpha(color: Int, alpha: Int): Int {
    return Color.argb(
        alpha.coerceIn(0, 255),
        Color.red(color),
        Color.green(color),
        Color.blue(color)
    )
}

private fun File.toContentUri(context: Context): Uri {
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        this
    )
}
