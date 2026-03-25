package com.example.signalsketch.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.signalsketch.data.repo.SavedSessionDetail
import com.example.signalsketch.data.repo.SavedSessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

            FileOutputStream(previewFile).use { stream ->
                buildPreviewBitmap(session).compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            dataFile.writeText(buildExportJson(session).toString(2))

            SharedSessionExport(
                imageUri = previewFile.toContentUri(context),
                dataUri = dataFile.toContentUri(context)
            )
        }
    }
}

private fun buildPreviewBitmap(session: SavedSessionDetail): Bitmap {
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
        color = Color.rgb(176, 168, 156)
        strokeWidth = 3f
    }
    val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(43, 76, 126)
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }
    val pathPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(22, 50, 79)
        style = Paint.Style.FILL
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

    val xValues = buildList {
        addAll(session.pathPoints.map { it.xMeters })
        addAll(session.wifiSamples.mapNotNull { it.xMeters })
    }
    val yValues = buildList {
        addAll(session.pathPoints.map { it.yMeters })
        addAll(session.wifiSamples.mapNotNull { it.yMeters })
    }
    val minX = xValues.minOrNull() ?: -1f
    val maxX = xValues.maxOrNull() ?: 1f
    val minY = yValues.minOrNull() ?: -1f
    val maxY = yValues.maxOrNull() ?: 1f
    val spanX = max(maxX - minX, 1f)
    val spanY = max(maxY - minY, 1f)
    val scale = min(plotWidth / spanX, plotHeight / spanY)

    fun mapX(value: Float): Float = plotLeft + ((value - minX) * scale)
    fun mapY(value: Float): Float = plotBottom - ((value - minY) * scale)

    canvas.drawLine((plotLeft + plotRight) / 2f, plotTop, (plotLeft + plotRight) / 2f, plotBottom, axisPaint)
    canvas.drawLine(plotLeft, (plotTop + plotBottom) / 2f, plotRight, (plotTop + plotBottom) / 2f, axisPaint)

    session.wifiSamples.forEach { sample ->
        val x = sample.xMeters ?: return@forEach
        val y = sample.yMeters ?: return@forEach
        val wifiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sample.rssiDbm.toPreviewColor()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(mapX(x), mapY(y), 14f, wifiPaint)
    }

    session.pathPoints.zipWithNext { start, end ->
        canvas.drawLine(
            mapX(start.xMeters),
            mapY(start.yMeters),
            mapX(end.xMeters),
            mapY(end.yMeters),
            pathPaint
        )
    }
    session.pathPoints.forEach { point ->
        canvas.drawCircle(mapX(point.xMeters), mapY(point.yMeters), 9f, pathPointPaint)
    }

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
    }
}

private fun Int.toPreviewColor(): Int {
    return when {
        this >= -55 -> Color.rgb(46, 125, 50)
        this >= -67 -> Color.rgb(249, 168, 37)
        else -> Color.rgb(198, 40, 40)
    }
}

private val SavedSessionStatus.label: String
    get() = when (this) {
        SavedSessionStatus.PAUSED -> "Paused"
        SavedSessionStatus.COMPLETED -> "Completed"
    }

private fun File.toContentUri(context: Context): Uri {
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        this
    )
}
