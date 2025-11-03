package com.twinmind.voicerecorder

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.twinmind.voicerecorder.presentation.navigation.VoiceRecorderNavigation
import com.twinmind.voicerecorder.presentation.viewmodel.RecordingViewModel
import com.twinmind.voicerecorder.ui.theme.VoiceRecorderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: RecordingViewModel
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.startRecording()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            VoiceRecorderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val recordingViewModel: RecordingViewModel = hiltViewModel()
                    viewModel = recordingViewModel
                    
                    VoiceRecorderNavigation(
                        navController = navController,
                        onStartRecordingWithPermissions = { startRecordingWithPermissions() }
                    )
                }
            }
        }
    }
    
    private fun startRecordingWithPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
        
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PermissionChecker.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isEmpty()) {
            viewModel.startRecording()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }
}

