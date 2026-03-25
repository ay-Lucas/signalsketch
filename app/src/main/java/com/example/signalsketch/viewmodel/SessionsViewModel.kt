package com.example.signalsketch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.signalsketch.data.repo.SavedSessionDetail
import com.example.signalsketch.data.repo.SavedSessionStatus
import com.example.signalsketch.data.repo.SavedSessionSummary
import com.example.signalsketch.data.repo.ScanSessionRepositoryFactory
import com.example.signalsketch.storage.SessionExportManagerFactory
import com.example.signalsketch.storage.SharedSessionExport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

data class SavedSessionListItemUiState(
    val sessionId: Long,
    val title: String,
    val subtitle: String,
    val metadata: String,
    val notes: String?
)

data class SessionsUiState(
    val sessions: List<SavedSessionListItemUiState> = emptyList(),
    val emptyMessage: String = "No saved sessions yet."
)

data class SavedSessionDetailUiState(
    val sessionId: Long = 0,
    val title: String = "",
    val subtitle: String = "",
    val metadata: String = "",
    val notes: String? = null,
    val pathSamples: List<RecordedPathSample> = emptyList(),
    val wifiSamples: List<RecordedWifiSample> = emptyList(),
    val isLoading: Boolean = true,
    val canDelete: Boolean = false,
    val isDeleted: Boolean = false,
    val isExporting: Boolean = false
)

class SessionsViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = ScanSessionRepositoryFactory.create(application)

    val uiState: StateFlow<SessionsUiState> = repository.observeSessions()
        .map { sessions ->
            SessionsUiState(
                sessions = sessions.map { it.toListItemUiState() },
                emptyMessage = "Save a paused or completed mapping session to see it here."
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionsUiState()
        )
}

class SavedSessionDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository = ScanSessionRepositoryFactory.create(application)
    private val exportManager = SessionExportManagerFactory.create(application)
    private val sessionId = checkNotNull(savedStateHandle.get<Long>("sessionId"))
    private val deletedState = MutableStateFlow(false)
    private val exportingState = MutableStateFlow(false)
    private val pendingShareExport = MutableStateFlow<SharedSessionExport?>(null)

    val uiState: StateFlow<SavedSessionDetailUiState> = combine(
        repository.observeSessionDetail(sessionId),
        deletedState,
        exportingState
    ) { detail, isDeleted, isExporting ->
        when {
            isDeleted -> SavedSessionDetailUiState(
                sessionId = sessionId,
                isLoading = false,
                isDeleted = true
            )
            detail == null -> SavedSessionDetailUiState(
                sessionId = sessionId,
                isLoading = false,
                isExporting = isExporting
            )
            else -> detail.toUiState(isExporting = isExporting)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SavedSessionDetailUiState(sessionId = sessionId)
    )

    val shareExport: StateFlow<SharedSessionExport?> = pendingShareExport

    fun deleteSession() {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            deletedState.value = true
        }
    }

    fun shareSession() {
        viewModelScope.launch {
            exportingState.value = true
            try {
                val detail = repository.observeSessionDetail(sessionId).first() ?: return@launch
                pendingShareExport.value = exportManager.exportSession(detail)
            } finally {
                exportingState.value = false
            }
        }
    }

    fun onShareHandled() {
        pendingShareExport.value = null
    }
}

private fun SavedSessionSummary.toListItemUiState(): SavedSessionListItemUiState {
    return SavedSessionListItemUiState(
        sessionId = sessionId,
        title = name,
        subtitle = formatSessionTimeRange(startedAtEpochMillis, endedAtEpochMillis),
        metadata = buildString {
            append(status.label)
            append("  •  ")
            append("${pathPointCount} path points")
            append("  •  ")
            append("${wifiSampleCount} Wi-Fi samples")
        },
        notes = notes
    )
}

private fun SavedSessionDetail.toUiState(isExporting: Boolean): SavedSessionDetailUiState {
    return SavedSessionDetailUiState(
        sessionId = summary.sessionId,
        title = summary.name,
        subtitle = formatSessionTimeRange(summary.startedAtEpochMillis, summary.endedAtEpochMillis),
        metadata = buildString {
            append(summary.status.label)
            append("  •  ")
            append("${summary.pathPointCount} path points")
            append("  •  ")
            append("${summary.wifiSampleCount} Wi-Fi samples")
        },
        notes = summary.notes,
        pathSamples = pathPoints.mapIndexed { index, point ->
            RecordedPathSample(
                index = index,
                xMeters = point.xMeters,
                yMeters = point.yMeters,
                headingDegrees = point.headingDegrees ?: 0f,
                sensorSampleCount = index + 1,
                recordedAtEpochMillis = point.recordedAtEpochMillis
            )
        },
        wifiSamples = wifiSamples.map { sample ->
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
        },
        isLoading = false,
        canDelete = true,
        isExporting = isExporting
    )
}

private fun formatSessionTimeRange(startedAtEpochMillis: Long, endedAtEpochMillis: Long?): String {
    val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    val startedLabel = dateFormat.format(Date(startedAtEpochMillis))
    val endedLabel = endedAtEpochMillis?.let { dateFormat.format(Date(it)) } ?: "Paused"
    return "Started $startedLabel  •  Ended $endedLabel"
}

private val SavedSessionStatus.label: String
    get() = when (this) {
        SavedSessionStatus.PAUSED -> "Paused"
        SavedSessionStatus.COMPLETED -> "Completed"
    }
