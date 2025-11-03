package com.twinmind.voicerecorder.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinmind.voicerecorder.domain.model.RecordingState
import com.twinmind.voicerecorder.presentation.viewmodel.RecordingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    onNavigateBack: () -> Unit,
    onStartRecording: () -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    // Observe ViewModel state
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingTime by viewModel.recordingTime.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Recording") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // Recording Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (recordingState) {
                            RecordingState.RECORDING -> "Recording..."
                            RecordingState.PAUSED -> "Paused"
                            RecordingState.STOPPED -> "Recording Complete"
                            RecordingState.ERROR -> "Error Occurred"
                            else -> "Ready to Record"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (isRecording) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = recordingTime,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (isRecording) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Control Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                
                if (isRecording && recordingState == RecordingState.RECORDING) {
                    // Pause Button
                    OutlinedButton(
                        onClick = { viewModel.pauseRecording() },
                        modifier = Modifier.size(width = 120.dp, height = 56.dp)
                    ) {
                        Text("PAUSE")
                    }
                } else if (recordingState == RecordingState.PAUSED) {
                    // Resume Button
                    OutlinedButton(
                        onClick = { viewModel.resumeRecording() },
                        modifier = Modifier.size(width = 120.dp, height = 56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("RESUME")
                    }
                }
                
                // Main Record/Stop Button
                Button(
                    onClick = {
                        if (isRecording || recordingState == RecordingState.PAUSED) {
                            viewModel.stopRecording()
                        } else {
                            onStartRecording()
                        }
                    },
                    modifier = Modifier.size(width = 140.dp, height = 56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording || recordingState == RecordingState.PAUSED) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isRecording || recordingState == RecordingState.PAUSED) "STOP" else "START",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Status Text
            Text(
                text = when {
                    recordingState == RecordingState.PAUSED -> "Recording paused. Tap RESUME to continue or STOP to finish."
                    isRecording -> "Recording in progress. Tap PAUSE or STOP when needed."
                    recordingState == RecordingState.STOPPED -> "Recording saved successfully!"
                    else -> "Tap START to begin recording your voice."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}