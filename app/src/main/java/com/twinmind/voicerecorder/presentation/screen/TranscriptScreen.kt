package com.twinmind.voicerecorder.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinmind.voicerecorder.domain.model.TranscriptChunk
import com.twinmind.voicerecorder.domain.model.TranscriptionStatus
import com.twinmind.voicerecorder.presentation.viewmodel.TranscriptViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptScreen(
    sessionId: String,
    onNavigateBack: () -> Unit,
    viewModel: TranscriptViewModel = hiltViewModel()
) {
    val transcriptChunks by viewModel.transcriptChunks.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val fullTranscript by viewModel.fullTranscript.collectAsStateWithLifecycle()
    
    LaunchedEffect(sessionId) {
        viewModel.loadTranscripts(sessionId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Transcript") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Full transcript summary card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                "Full Transcript",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (fullTranscript.isNotBlank()) {
                                Text(
                                    fullTranscript,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                Text(
                                    "No transcription available yet. Transcripts will appear here as audio chunks are processed.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                
                // Individual chunk transcripts
                item {
                    Text(
                        "Chunk Details (${transcriptChunks.size} chunks)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(transcriptChunks) { chunk ->
                    TranscriptChunkItem(chunk = chunk)
                }
                
                if (transcriptChunks.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "🎤",
                                    style = MaterialTheme.typography.headlineLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No transcripts yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Start recording to see transcripts appear here automatically every 30 seconds.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TranscriptChunkItem(
    chunk: TranscriptChunk
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Chunk ${chunk.chunkSequence + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                TranscriptStatusChip(status = chunk.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                chunk.transcriptText.ifBlank { "Processing..." },
                style = MaterialTheme.typography.bodyMedium,
                color = if (chunk.status == TranscriptionStatus.FAILED) 
                    MaterialTheme.colorScheme.error 
                else MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
            Text(
                "Created: ${dateFormat.format(Date(chunk.createdAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun TranscriptStatusChip(status: TranscriptionStatus) {
    val (text, color) = when (status) {
        TranscriptionStatus.PENDING -> "Pending" to MaterialTheme.colorScheme.outline
        TranscriptionStatus.IN_PROGRESS -> "Processing" to MaterialTheme.colorScheme.primary
        TranscriptionStatus.COMPLETED -> "Completed" to MaterialTheme.colorScheme.tertiary
        TranscriptionStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
    }
    
    AssistChip(
        onClick = { /* No action for status chip */ },
        label = { 
            Text(
                text, 
                style = MaterialTheme.typography.labelSmall
            ) 
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color
        )
    )
}