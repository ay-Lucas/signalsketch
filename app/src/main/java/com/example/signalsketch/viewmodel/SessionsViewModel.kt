package com.example.signalsketch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.signalsketch.data.local.ScanSessionEntity
import com.example.signalsketch.data.repo.ScanSessionRepositoryFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.DateFormat
import java.util.Date

data class SavedSessionListItemUiState(
    val sessionId: Long,
    val title: String,
    val subtitle: String,
    val notes: String?
)

data class SessionsUiState(
    val sessions: List<SavedSessionListItemUiState> = emptyList(),
    val emptyMessage: String = "No saved sessions yet."
)

class SessionsViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = ScanSessionRepositoryFactory.create(application)

    val uiState: StateFlow<SessionsUiState> = repository.observeSessions()
        .map { sessions ->
            SessionsUiState(
                sessions = sessions.map { it.toUiState() },
                emptyMessage = "Complete and reset a 2D or AR mapping session to save it here."
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionsUiState()
        )

    private fun ScanSessionEntity.toUiState(): SavedSessionListItemUiState {
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        val startedLabel = dateFormat.format(Date(startedAtEpochMillis))
        val endedLabel = endedAtEpochMillis?.let { dateFormat.format(Date(it)) } ?: "In progress"

        return SavedSessionListItemUiState(
            sessionId = sessionId,
            title = name,
            subtitle = "Started $startedLabel  •  Ended $endedLabel",
            notes = notes
        )
    }
}
