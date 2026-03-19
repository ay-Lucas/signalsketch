package com.example.signalsketch.ui.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.signalsketch.viewmodel.SavedSessionListItemUiState
import com.example.signalsketch.viewmodel.SessionsUiState
import com.example.signalsketch.viewmodel.SessionsViewModel

@Composable
fun SavedSessionsScreen(viewModel: SessionsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    SavedSessionsScreen(uiState = uiState)
}

@Composable
private fun SavedSessionsScreen(uiState: SessionsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Saved Sessions",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Normal mapping and AR mapping sessions are stored together in the same local session model.",
            style = MaterialTheme.typography.bodyMedium
        )
        if (uiState.sessions.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = uiState.emptyMessage,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.sessions, key = { it.sessionId }) { session ->
                    SessionCard(session = session)
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: SavedSessionListItemUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = session.subtitle,
                style = MaterialTheme.typography.bodySmall
            )
            session.notes?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
