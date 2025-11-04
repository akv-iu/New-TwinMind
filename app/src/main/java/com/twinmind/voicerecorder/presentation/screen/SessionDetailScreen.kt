package com.twinmind.voicerecorder.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinmind.voicerecorder.domain.audio.PlaybackState
import com.twinmind.voicerecorder.domain.model.RecordingSession
import com.twinmind.voicerecorder.domain.model.SessionStatus
import com.twinmind.voicerecorder.presentation.viewmodel.DashboardViewModel
import com.twinmind.voicerecorder.presentation.viewmodel.SessionDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    onNavigateBack: () -> Unit,
    onNavigateToTranscript: () -> Unit = { /* Default empty implementation */ },
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    detailViewModel: SessionDetailViewModel = hiltViewModel()
) {
    val sessions by dashboardViewModel.sessions.collectAsStateWithLifecycle()
    val session = sessions.find { it.id == sessionId }
    
    val playbackState by detailViewModel.playbackState.collectAsStateWithLifecycle()
    val isLoading by detailViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by detailViewModel.errorMessage.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recording Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (session != null) {
                        IconButton(
                            onClick = { 
                                detailViewModel.deleteSession(sessionId)
                                onNavigateBack()
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (session == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Recording not found",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            SessionDetailContent(
                session = session,
                playbackState = playbackState,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onPlayClick = { detailViewModel.playRecording(sessionId) },
                onPauseClick = { detailViewModel.pausePlayback() },
                onResumeClick = { detailViewModel.resumePlayback() },
                onStopClick = { detailViewModel.stopPlayback() },
                onClearError = { detailViewModel.clearError() },
                onNavigateToTranscript = onNavigateToTranscript,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun SessionDetailContent(
    session: RecordingSession,
    playbackState: PlaybackState,
    isLoading: Boolean,
    errorMessage: String?,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit,
    onClearError: () -> Unit,
    onNavigateToTranscript: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Error message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClearError) {
                        Text("Dismiss")
                    }
                }
            }
        }
        // Title Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                StatusChip(status = session.status)
            }
        }
        
        // Recording Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Recording Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                InfoRow(label = "Duration", value = formatDuration(session.duration))
                InfoRow(label = "Created", value = formatDate(session.createdAt))
                InfoRow(label = "Total Chunks", value = session.totalChunks.toString())
                
                if (session.endTime != null) {
                    InfoRow(label = "Completed", value = formatDate(session.endTime))
                }
            }
        }
        
        // Actions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Play/Pause/Stop buttons
                when (playbackState) {
                    PlaybackState.IDLE, PlaybackState.COMPLETED, PlaybackState.ERROR -> {
                        Button(
                            onClick = onPlayClick,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = session.status == SessionStatus.COMPLETED && !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Loading...")
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Play Recording")
                            }
                        }
                    }
                    PlaybackState.PLAYING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onPauseClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("⏸", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pause")
                            }
                            OutlinedButton(
                                onClick = onStopClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("⏹", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop")
                            }
                        }
                    }
                    PlaybackState.PAUSED -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onResumeClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume")
                            }
                            OutlinedButton(
                                onClick = onStopClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("⏹", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop")
                            }
                        }
                    }
                }
                
                // Transcript button
                OutlinedButton(
                    onClick = { onNavigateToTranscript() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Transcript")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatusChip(status: SessionStatus) {
    val color = when (status) {
        SessionStatus.RECORDING -> MaterialTheme.colorScheme.error
        SessionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        SessionStatus.TRANSCRIBING -> MaterialTheme.colorScheme.tertiary
        SessionStatus.SUMMARIZING -> MaterialTheme.colorScheme.secondary
        SessionStatus.READY -> MaterialTheme.colorScheme.primary
        SessionStatus.FAILED -> MaterialTheme.colorScheme.error
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.name.lowercase().replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatDuration(seconds: Long): String {
    if (seconds == 0L) return "0:00"
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}