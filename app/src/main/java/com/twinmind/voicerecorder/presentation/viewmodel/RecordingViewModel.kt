package com.twinmind.voicerecorder.presentation.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.twinmind.voicerecorder.data.service.RecordingService
import com.twinmind.voicerecorder.domain.model.RecordingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val application: Application
) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    
    // UI State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _recordingTime = MutableStateFlow("00:00")
    val recordingTime: StateFlow<String> = _recordingTime.asStateFlow()
    
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    private var recordingService: RecordingService? = null
    private var serviceBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RecordingService.RecordingBinder
            recordingService = binder.getService()
            serviceBound = true
            
            // Start observing service state
            observeServiceState()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            recordingService = null
            serviceBound = false
            // Automatically try to reconnect after a short delay
            viewModelScope.launch {
                kotlinx.coroutines.delay(1000)
                if (!serviceBound) {
                    bindToService()
                }
            }
        }
    }
    
    init {
        bindToService()
    }
    
    private fun bindToService() {
        val intent = Intent(context, RecordingService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun observeServiceState() {
        recordingService?.let { service ->
            viewModelScope.launch {
                service.getRecordingState().collect { state ->
                    _recordingState.value = state
                    _isRecording.value = (state == RecordingState.RECORDING)
                }
            }
            
            viewModelScope.launch {
                service.getRecordingTime().collect { time ->
                    _recordingTime.value = formatTime(time)
                }
            }
        }
    }
    
    fun startRecording() {
        // Ensure service is bound before starting
        if (!serviceBound) {
            bindToService()
        }
        
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START_RECORDING
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    
    fun stopRecording() {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP_RECORDING
        }
        context.startService(intent)
    }
    
    fun pauseRecording() {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_PAUSE_RECORDING
        }
        context.startService(intent)
    }
    
    fun resumeRecording() {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_RESUME_RECORDING
        }
        context.startService(intent)
    }
    
    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
    
    override fun onCleared() {
        super.onCleared()
        if (serviceBound) {
            context.unbindService(serviceConnection)
            serviceBound = false
        }
    }
}