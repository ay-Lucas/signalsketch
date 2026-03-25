package com.example.signalsketch.ui.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.signalsketch.ui.mapping.SessionMapCard
import com.example.signalsketch.viewmodel.SavedSessionDetailUiState
import com.example.signalsketch.viewmodel.SavedSessionDetailViewModel
import com.example.signalsketch.viewmodel.SavedSessionListItemUiState
import com.example.signalsketch.viewmodel.SessionsUiState
import com.example.signalsketch.viewmodel.SessionsViewModel

@Composable
fun SavedSessionsScreen(
    viewModel: SessionsViewModel,
    onSessionClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    SavedSessionsScreen(
        uiState = uiState,
        onSessionClick = onSessionClick
    )
}

@Composable
fun SavedSessionDetailScreen(
    viewModel: SavedSessionDetailViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBack()
        }
    }

    SavedSessionDetailScreen(
        uiState = uiState,
        onDeleteSession = viewModel::deleteSession,
        onBack = onBack
    )
}

@Composable
private fun SavedSessionsScreen(
    uiState: SessionsUiState,
    onSessionClick: (Long) -> Unit
) {
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
            text = "Saved sessions include the original walked path plus all linked Wi-Fi samples.",
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
                    SessionCard(
                        session = session,
                        onClick = { onSessionClick(session.sessionId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedSessionDetailScreen(
    uiState: SavedSessionDetailUiState,
    onDeleteSession: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Saved Sessions")
        }

        when {
            uiState.isLoading -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Loading saved session...",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            uiState.title.isBlank() -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "This session is unavailable.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = uiState.title,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = uiState.subtitle,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = uiState.metadata,
                            style = MaterialTheme.typography.bodySmall
                        )
                        uiState.notes?.let { notes ->
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                SessionMapCard(
                    pathSamples = uiState.pathSamples,
                    wifiSamples = uiState.wifiSamples,
                    emptyMessage = "This saved session does not include map samples."
                )

                Button(
                    onClick = onDeleteSession,
                    enabled = uiState.canDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Session")
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: SavedSessionListItemUiState,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
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
            Text(
                text = session.metadata,
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
