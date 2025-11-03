package com.twinmind.voicerecorder.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.twinmind.voicerecorder.R
import com.twinmind.voicerecorder.domain.model.RecordingState
import com.twinmind.voicerecorder.domain.repository.AudioRecorder
import com.twinmind.voicerecorder.domain.repository.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {
    
    @Inject
    lateinit var audioRecorder: AudioRecorder
    
    @Inject
    lateinit var sessionRepository: SessionRepository
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var currentSessionId: String? = null
    private var notificationManager: NotificationManager? = null
    
    // Binder for service connection
    private val binder = RecordingBinder()
    
    // Timer state
    private val _recordingTime = MutableStateFlow(0L)
    private val recordingTime: StateFlow<Long> = _recordingTime.asStateFlow()
    
    // Recording state
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    private val recordingStateFlow: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    private var startTime = 0L
    private var timerJob: Job? = null
    
    inner class RecordingBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }
    
    fun getRecordingState(): StateFlow<RecordingState> = recordingStateFlow
    fun getRecordingTime(): StateFlow<Long> = recordingTime
    
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "recording_channel"
        
        const val ACTION_START_RECORDING = "action_start_recording"
        const val ACTION_STOP_RECORDING = "action_stop_recording"
        const val ACTION_PAUSE_RECORDING = "action_pause_recording"
        const val ACTION_RESUME_RECORDING = "action_resume_recording"
    }
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        observeRecordingState()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording()
            ACTION_PAUSE_RECORDING -> pauseRecording()
            ACTION_RESUME_RECORDING -> resumeRecording()
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    private fun startRecording() {
        try {
            currentSessionId = UUID.randomUUID().toString()
            startForeground(NOTIFICATION_ID, createRecordingNotification("Starting..."))
            
            currentSessionId?.let { sessionId ->
                serviceScope.launch {
                    // Create session in database with the same ID
                    val title = "Recording ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}"
                    sessionRepository.createSessionWithId(sessionId, title)
                }
                
                val result = audioRecorder.startRecording(sessionId)
                if (result.isSuccess) {
                    startTimer()
                } else {
                    updateNotification("Error: ${result.exceptionOrNull()?.message}")
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted, stop service
        }
    }
    
    private fun stopRecording() {
        currentSessionId?.let { sessionId ->
            audioRecorder.stopRecording()
            
            // Save duration BEFORE resetting timer
            val duration = _recordingTime.value
            serviceScope.launch {
                sessionRepository.completeSession(sessionId, duration)
            }
        }
        stopTimer()
        currentSessionId = null
        // Don't call stopSelf() - keep service alive for next recording
    }
    
    private fun pauseRecording() {
        audioRecorder.pauseRecording()
        timerJob?.cancel() // Just pause timer without resetting
        _recordingState.value = RecordingState.PAUSED
    }
    
    private fun resumeRecording() {
        audioRecorder.resumeRecording()
        _recordingState.value = RecordingState.RECORDING
        startTimer() // Resume timer from current value
    }
    
    private fun startTimer() {
        startTime = System.currentTimeMillis() - (_recordingTime.value * 1000)
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                _recordingTime.value = elapsed
                delay(1000)
            }
        }
    }
    
    private fun stopTimer() {
        timerJob?.cancel()
        _recordingTime.value = 0L
    }
    
    private fun observeRecordingState() {
        serviceScope.launch {
            audioRecorder.getRecordingState().collectLatest { state ->
                _recordingState.value = state
                when (state) {
                    RecordingState.RECORDING -> {
                        updateNotification("Recording...")
                    }
                    RecordingState.PAUSED -> {
                        updateNotification("Paused")
                    }
                    RecordingState.STOPPED -> {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        // Keep service alive for next recording
                        // The AudioRecorderImpl will reset to IDLE automatically
                    }
                    RecordingState.ERROR -> {
                        updateNotification("Error occurred")
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        // Reset to IDLE instead of destroying service
                        _recordingState.value = RecordingState.IDLE
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing voice recording notifications"
                setSound(null, null)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun createRecordingNotification(status: String): Notification {
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Recording")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Stop",
                stopPendingIntent
            )
            .build()
    }
    
    private fun updateNotification(status: String) {
        val notification = createRecordingNotification(status)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        audioRecorder.release()
        serviceScope.cancel()
    }
}