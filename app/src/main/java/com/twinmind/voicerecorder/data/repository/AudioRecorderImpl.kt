package com.twinmind.voicerecorder.data.repository

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.twinmind.voicerecorder.domain.model.AudioChunk
import com.twinmind.voicerecorder.domain.model.RecordingState
import com.twinmind.voicerecorder.domain.repository.AudioChunkRepository
import com.twinmind.voicerecorder.domain.repository.AudioRecorder
import com.twinmind.voicerecorder.domain.repository.AudioStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorderImpl @Inject constructor(
    private val audioStorage: AudioStorage,
    private val chunkRepository: AudioChunkRepository,
    @ApplicationContext private val context: Context
) : AudioRecorder {
    
    private val recordingScope = CoroutineScope(Dispatchers.IO + Job())
    
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    private val _audioChunks = MutableStateFlow<List<AudioChunk>>(emptyList())
    
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var currentSessionId: String? = null
    private var chunkSequenceNumber = 0
    
    // Overlap buffer to preserve speech continuity between chunks
    private var overlapBuffer = mutableListOf<Byte>()
    
    // Audio Focus Management
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val telephonyManager: TelephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wasRecordingBeforeFocusLoss = false
    
    // Microphone Source Management
    private var currentMicrophoneSource: String = "Built-in Microphone"
    private var sourceSwitchStartTime = 0L
    private var totalSwitchTime = 0L
    private var isSourceChanging = false
    
    // Silent Audio Detection
    private var lastAudioTime = 0L
    private var silenceStartTime = 0L
    private var isSilentWarningShown = false
    private val silenceThresholdMs = 10_000L // 10 seconds
    private val audioLevelThreshold = 500 // Minimum audio level to consider as "sound"
    
    // Storage Management
    private val minStorageBytes = 50 * 1024 * 1024L // 50MB minimum storage
    private var lastStorageCheck = 0L
    private val storageCheckIntervalMs = 30_000L // Check every 30 seconds
    
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        android.util.Log.d("AudioRecorder", "Audio focus changed: $focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss - pause recording
                handleAudioFocusLoss(permanent = true)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporary loss - pause recording
                handleAudioFocusLoss(permanent = false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Can duck, but for recording we should pause
                handleAudioFocusLoss(permanent = false)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Regained focus - resume if was recording
                handleAudioFocusGain()
            }
        }
    }
    
    private val audioDeviceCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                Log.d(TAG, "Audio devices added: ${addedDevices.map { getDeviceName(it) }}")
                handleAudioDeviceChange()
            }
            
            override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
                Log.d(TAG, "Audio devices removed: ${removedDevices.map { getDeviceName(it) }}")
                handleAudioDeviceChange()
            }
        }
    } else null
    
    private fun getDeviceName(device: AudioDeviceInfo): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
                AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
                AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Built-in Microphone"
                else -> "Unknown Device (${device.type})"
            }
        } else {
            "Unknown Device"
        }
    }
    
    companion object {
        private const val TAG = "AudioRecorderImpl"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val CHUNK_DURATION_MS = 30_000L // 30 seconds
        private const val OVERLAP_DURATION_MS = 2_000L // 2 seconds
        private const val BUFFER_SIZE_MULTIPLIER = 2
    }
    
    override fun startRecording(sessionId: String): Result<Unit> {
        return try {
            if (_recordingState.value != RecordingState.IDLE) {
                return Result.failure(IllegalStateException("Recording already in progress"))
            }
            
            // Check storage before starting recording
            if (!hasEnoughStorage()) {
                return Result.failure(IllegalStateException("Insufficient storage space"))
            }
            
            // Request audio focus before starting recording
            if (!requestAudioFocus()) {
                return Result.failure(IllegalStateException("Could not obtain audio focus"))
            }
            
            // Register device callback for microphone source monitoring
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioDeviceCallback?.let { callback ->
                    audioManager.registerAudioDeviceCallback(callback, null)
                }
            }
            
            // Initialize microphone source
            currentMicrophoneSource = getCurrentMicrophoneSource()
            android.util.Log.d("AudioRecorder", "Starting recording with microphone: $currentMicrophoneSource")
            
            currentSessionId = sessionId
            chunkSequenceNumber = 0
            overlapBuffer.clear() // Clear overlap buffer for new session
            
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )
            
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                return Result.failure(IllegalStateException("Invalid buffer size"))
            }
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * BUFFER_SIZE_MULTIPLIER
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return Result.failure(SecurityException("AudioRecord not initialized - check permissions"))
            }
            
            audioRecord?.startRecording()
            
            if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                return Result.failure(SecurityException("Failed to start recording - check permissions"))
            }
            
            _recordingState.value = RecordingState.RECORDING
            
            // Initialize monitoring variables
            val currentTime = System.currentTimeMillis()
            lastAudioTime = currentTime
            silenceStartTime = 0L
            isSilentWarningShown = false
            lastStorageCheck = currentTime
            
            recordingJob = recordingScope.launch {
                recordInChunks()
            }
            
            Result.success(Unit)
        } catch (e: SecurityException) {
            _recordingState.value = RecordingState.ERROR
            Result.failure(e)
        } catch (e: Exception) {
            _recordingState.value = RecordingState.ERROR
            Result.failure(e)
        }
    }
    
    override fun stopRecording(): Result<Unit> {
        return try {
            // Set state to STOPPED so the recording loop exits and saves remaining data
            _recordingState.value = RecordingState.STOPPED
            
            // Abandon audio focus
            abandonAudioFocus()
            wasRecordingBeforeFocusLoss = false
            
            // Unregister audio device callback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to unregister audio device callback", e)
                }
            }
            
            recordingJob?.cancel()
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
            audioRecord = null
            currentSessionId = null
            overlapBuffer.clear() // Clear overlap buffer when stopping
            
            // Reset to IDLE after a brief moment so the recorder can be reused
            recordingScope.launch {
                kotlinx.coroutines.delay(100)
                _recordingState.value = RecordingState.IDLE
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun pauseRecording(): Result<Unit> {
        return try {
            if (_recordingState.value == RecordingState.RECORDING) {
                _recordingState.value = RecordingState.PAUSED
                // Don't stop AudioRecord, just change state to pause data collection
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun resumeRecording(): Result<Unit> {
        return try {
            if (_recordingState.value == RecordingState.PAUSED) {
                _recordingState.value = RecordingState.RECORDING
                // AudioRecord continues running, just resume data collection
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getRecordingState(): Flow<RecordingState> = _recordingState.asStateFlow()
    
    override fun getAudioChunks(): Flow<List<AudioChunk>> = _audioChunks.asStateFlow()
    
    override fun getCurrentSessionId(): String? = currentSessionId
    
    override fun release() {
        stopRecording()
        recordingJob?.cancel()
    }
    
    private suspend fun recordInChunks() {
        val sessionId = currentSessionId ?: return
        android.util.Log.d("AudioRecorder", "Starting recordInChunks for session: $sessionId")
        
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        
        val buffer = ByteArray(bufferSize)
        val chunkData = mutableListOf<Byte>()
        val chunkStartTime = System.currentTimeMillis()
        
        var lastChunkTime = chunkStartTime
        var recordingStartTime = System.currentTimeMillis()  // Track when recording actually started
        var totalPausedTime = 0L  // Track total time spent in paused state
        
        var pauseStartTime = 0L
        
        while (recordingScope.isActive && _recordingState.value != RecordingState.STOPPED && _recordingState.value != RecordingState.ERROR) {
            val audioRecord = this.audioRecord ?: break
            val currentTime = System.currentTimeMillis()
            
            // Track paused time (includes manual pause, audio focus loss, and phone calls)
            if (_recordingState.value == RecordingState.PAUSED || 
                _recordingState.value == RecordingState.PAUSED_FOCUS_LOSS || 
                _recordingState.value == RecordingState.PAUSED_PHONE_CALL) {
                if (pauseStartTime == 0L) {
                    pauseStartTime = currentTime
                }
                delay(100) // Longer delay when paused
                continue
            } else {
                // If we were paused, add the pause duration to total paused time
                if (pauseStartTime > 0L) {
                    totalPausedTime += (currentTime - pauseStartTime)
                    pauseStartTime = 0L
                }
            }
            
            // Only read data when actually recording (not paused)
            if (_recordingState.value == RecordingState.RECORDING || 
                _recordingState.value == RecordingState.RECORDING_SOURCE_CHANGING ||
                _recordingState.value == RecordingState.SILENT_DETECTED) {
                val bytesRead = audioRecord.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    chunkData.addAll(buffer.take(bytesRead))
                    
                    // Monitor audio levels for silence detection
                    val audioLevel = calculateAudioLevel(buffer, bytesRead)
                    checkForSilentAudio(audioLevel, currentTime)
                    
                    // Check storage periodically
                    if (currentTime - lastStorageCheck > storageCheckIntervalMs) {
                        if (!hasEnoughStorage()) {
                            _recordingState.value = RecordingState.STOPPED_LOW_STORAGE
                            break
                        }
                        lastStorageCheck = currentTime
                    }
                    
                    // Calculate actual recording time (excluding pauses)
                    val actualRecordingTime = currentTime - recordingStartTime - totalPausedTime
                    
                    // Check if we've reached 30 seconds of actual recording time
                    if (actualRecordingTime >= CHUNK_DURATION_MS) {
                        android.util.Log.d("AudioRecorder", "Saving 30-second chunk - Sequence: $chunkSequenceNumber, Size: ${chunkData.size}, ActualTime: ${actualRecordingTime}ms")
                        
                        // Calculate overlap buffer size (2 seconds of audio data)
                        val bytesPerSecond = SAMPLE_RATE * 2 // 16-bit = 2 bytes per sample, mono
                        val overlapBufferSize = (OVERLAP_DURATION_MS * bytesPerSecond / 1000).toInt()
                        
                        // Save the chunk with overlap from previous chunk
                        val chunkWithOverlap = if (overlapBuffer.isNotEmpty()) {
                            overlapBuffer + chunkData
                        } else {
                            chunkData.toList()
                        }
                        val hasOverlap = overlapBuffer.isNotEmpty()
                        val overlapDuration = if (hasOverlap) OVERLAP_DURATION_MS else 0L
                        saveChunk(
                            sessionId = sessionId, 
                            audioData = chunkWithOverlap.toByteArray(), 
                            startTime = lastChunkTime, 
                            durationMs = CHUNK_DURATION_MS,
                            hasOverlap = hasOverlap,
                            overlapDurationMs = overlapDuration
                        )
                        
                        // Extract last 2 seconds for next chunk's overlap
                        overlapBuffer.clear()
                        if (chunkData.size >= overlapBufferSize) {
                            val startIndex = chunkData.size - overlapBufferSize
                            overlapBuffer.addAll(chunkData.subList(startIndex, chunkData.size))
                        }
                        
                        chunkData.clear()
                        lastChunkTime = currentTime
                        recordingStartTime = currentTime  // Reset start time for next chunk
                        totalPausedTime = 0L  // Reset paused time for next chunk
                        chunkSequenceNumber++
                    } else {
                        android.util.Log.v("AudioRecorder", "Recording progress - ActualTime: ${actualRecordingTime}ms, ChunkSize: ${chunkData.size}, Sequence: $chunkSequenceNumber")
                    }
                }
            }
            
            // Small delay to prevent tight loop
            delay(10)
        }
        
        // Save any remaining data
        if (chunkData.isNotEmpty()) {
            val currentTime = System.currentTimeMillis()
            // Calculate final chunk duration
            val finalChunkDuration = currentTime - recordingStartTime - totalPausedTime
            android.util.Log.d("AudioRecorder", "Saving final chunk - Size: ${chunkData.size}, Duration: ${finalChunkDuration}ms, Sequence: $chunkSequenceNumber")
            // Final chunk may have overlap from previous chunk
            val finalChunkWithOverlap = if (overlapBuffer.isNotEmpty()) {
                overlapBuffer + chunkData
            } else {
                chunkData.toList()
            }
            val hasOverlap = overlapBuffer.isNotEmpty()
            val overlapDuration = if (hasOverlap) OVERLAP_DURATION_MS else 0L
            saveChunk(
                sessionId = sessionId,
                audioData = finalChunkWithOverlap.toByteArray(),
                startTime = lastChunkTime,
                durationMs = finalChunkDuration,
                hasOverlap = hasOverlap,
                overlapDurationMs = overlapDuration
            )
            android.util.Log.d("AudioRecorder", "Final chunk saved successfully - Total chunks created: ${chunkSequenceNumber + 1}")
        } else {
            android.util.Log.w("AudioRecorder", "No remaining data to save - Total chunks created: $chunkSequenceNumber")
        }
        
        android.util.Log.d("AudioRecorder", "recordInChunks loop exited for session: $sessionId")
    }
    
    private suspend fun saveChunk(
        sessionId: String,
        audioData: ByteArray,
        startTime: Long,
        durationMs: Long,
        hasOverlap: Boolean = false,
        overlapDurationMs: Long = 0L
    ) {
        val chunk = AudioChunk(
            id = UUID.randomUUID().toString(),
            filePath = "", // Will be set after saving
            sessionId = sessionId,
            sequenceNumber = chunkSequenceNumber,
            startTimeMs = startTime,
            durationMs = durationMs,
            sizeBytes = audioData.size.toLong(),
            hasOverlap = hasOverlap,
            overlapDurationMs = overlapDurationMs
        )
        
        android.util.Log.d("AudioRecorder", "Saving chunk for session: $sessionId, sequence: $chunkSequenceNumber, size: ${audioData.size}")
        
        audioStorage.saveAudioChunk(audioData, chunk).onSuccess { file ->
            android.util.Log.d("AudioRecorder", "Chunk saved successfully: ${file.name}")
            val updatedChunk = chunk.copy(filePath = file.absolutePath)
            
            // Save chunk to database synchronously in the current context
            try {
                kotlinx.coroutines.runBlocking {
                    chunkRepository.saveChunk(updatedChunk)
                }
                android.util.Log.d("AudioRecorder", "Chunk saved to database: ${updatedChunk.id}")
            } catch (e: Exception) {
                android.util.Log.e("AudioRecorder", "Failed to save chunk to database", e)
            }
            
            // Update in-memory list
            val currentChunks = _audioChunks.value.toMutableList()
            currentChunks.add(updatedChunk)
            _audioChunks.value = currentChunks
        }.onFailure { error ->
            android.util.Log.e("AudioRecorder", "Failed to save chunk", error)
            _recordingState.value = RecordingState.ERROR
        }
    }
    
    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ (API 26) - Use AudioFocusRequest
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
                
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
                
            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            // Below Android 8.0 - Use deprecated method
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }
    
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { request ->
                audioManager.abandonAudioFocusRequest(request)
                audioFocusRequest = null
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }
    
    private fun handleAudioFocusLoss(permanent: Boolean) {
        android.util.Log.d("AudioRecorder", "Audio focus lost (permanent: $permanent)")
        
        // Only pause if currently recording
        if (_recordingState.value == RecordingState.RECORDING) {
            wasRecordingBeforeFocusLoss = true
            
            // Check if this is due to a phone call with enhanced detection
            val isPhoneCall = try {
                val callState = telephonyManager.callState
                android.util.Log.d("AudioRecorder", "Current telephony call state: $callState")
                
                // Check current call state
                val currentCallActive = callState != TelephonyManager.CALL_STATE_IDLE
                
                // Also check AudioManager for call-related audio mode
                val audioMode = audioManager.mode
                android.util.Log.d("AudioRecorder", "Current audio mode: $audioMode")
                val audioModeCall = audioMode == AudioManager.MODE_IN_CALL || 
                                  audioMode == AudioManager.MODE_IN_COMMUNICATION ||
                                  audioMode == AudioManager.MODE_CALL_SCREENING
                
                val isCall = currentCallActive || audioModeCall
                android.util.Log.d("AudioRecorder", "Phone call detected - CallState: $currentCallActive, AudioMode: $audioModeCall, Final: $isCall")
                
                isCall
            } catch (e: SecurityException) {
                android.util.Log.w("AudioRecorder", "No permission to check phone state: ${e.message}")
                // Fallback: check audio mode only
                val audioMode = audioManager.mode
                val isCallMode = audioMode == AudioManager.MODE_IN_CALL || 
                               audioMode == AudioManager.MODE_IN_COMMUNICATION ||
                               audioMode == AudioManager.MODE_CALL_SCREENING
                android.util.Log.d("AudioRecorder", "Fallback audio mode check: $audioMode, isCall: $isCallMode")
                isCallMode
            }
            
            // Set appropriate state based on the cause
            if (isPhoneCall) {
                android.util.Log.d("AudioRecorder", "✅ Audio focus lost due to PHONE CALL")
                _recordingState.value = RecordingState.PAUSED_PHONE_CALL
            } else {
                android.util.Log.d("AudioRecorder", "⚠️ Audio focus lost due to OTHER APP - checking again in 500ms")
                _recordingState.value = RecordingState.PAUSED_FOCUS_LOSS
                
                // Check again after a short delay in case phone state changes
                recordingScope.launch {
                    kotlinx.coroutines.delay(500)
                    try {
                        val delayedCallState = telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE
                        val delayedAudioMode = audioManager.mode
                        val delayedCallMode = delayedAudioMode == AudioManager.MODE_IN_CALL || 
                                            delayedAudioMode == AudioManager.MODE_IN_COMMUNICATION ||
                                            delayedAudioMode == AudioManager.MODE_CALL_SCREENING
                        
                        if (delayedCallState || delayedCallMode) {
                            android.util.Log.d("AudioRecorder", "🔄 Delayed check: Phone call detected, updating state")
                            if (_recordingState.value == RecordingState.PAUSED_FOCUS_LOSS) {
                                _recordingState.value = RecordingState.PAUSED_PHONE_CALL
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("AudioRecorder", "Delayed phone state check failed: ${e.message}")
                    }
                }
            }
        }
    }
    
    private fun handleAudioFocusGain() {
        android.util.Log.d("AudioRecorder", "Audio focus gained")
        
        // Only resume if we were recording before focus loss and currently paused due to focus loss or phone call
        if (wasRecordingBeforeFocusLoss && (_recordingState.value == RecordingState.PAUSED_FOCUS_LOSS || _recordingState.value == RecordingState.PAUSED_PHONE_CALL)) {
            wasRecordingBeforeFocusLoss = false
            // Directly set the recording state instead of calling resumeRecording()
            _recordingState.value = RecordingState.RECORDING
        }
    }
    
    private fun handleAudioDeviceChange() {
        // Only handle device changes during recording
        if (_recordingState.value != RecordingState.RECORDING && 
            _recordingState.value != RecordingState.RECORDING_SOURCE_CHANGING) {
            return
        }
        
        val newMicrophoneSource = getCurrentMicrophoneSource()
        
        // If microphone source changed, handle the transition
        if (newMicrophoneSource != currentMicrophoneSource) {
            android.util.Log.d(TAG, "Microphone source changed from $currentMicrophoneSource to $newMicrophoneSource")
            
            val previousSource = currentMicrophoneSource
            currentMicrophoneSource = newMicrophoneSource
            
            // Briefly change state to indicate source switching
            if (_recordingState.value == RecordingState.RECORDING) {
                _recordingState.value = RecordingState.RECORDING_SOURCE_CHANGING
                sourceSwitchStartTime = System.currentTimeMillis()
                
                // Switch back to recording after a brief moment to show the change
                recordingScope.launch {
                    delay(1000) // 1 second indicator
                    if (_recordingState.value == RecordingState.RECORDING_SOURCE_CHANGING) {
                        _recordingState.value = RecordingState.RECORDING
                    }
                }
            }
            
            // Notify service about microphone source change
            notifyMicrophoneSourceChange(previousSource, newMicrophoneSource)
        }
    }
    
    private fun getCurrentMicrophoneSource(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            
            // Priority order: Bluetooth -> Wired -> Built-in
            devices.forEach { device ->
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> return "Bluetooth Headset"
                    AudioDeviceInfo.TYPE_WIRED_HEADSET -> return "Wired Headset" 
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> return "Wired Headphones"
                    AudioDeviceInfo.TYPE_USB_HEADSET -> return "USB Headset"
                }
            }
            
            // Check for built-in microphone
            devices.forEach { device ->
                when (device.type) {
                    AudioDeviceInfo.TYPE_BUILTIN_MIC -> return "Built-in Microphone"
                }
            }
            
            "Unknown Microphone"
        } else {
            // Fallback for older Android versions
            if (audioManager.isBluetoothScoOn) {
                "Bluetooth Headset"
            } else if (audioManager.isWiredHeadsetOn) {
                "Wired Headset"
            } else {
                "Built-in Microphone"
            }
        }
    }
    
    private fun notifyMicrophoneSourceChange(previousSource: String, newSource: String) {
        // Send broadcast to service to update notification
        try {
            val intent = Intent("com.twinmind.voicerecorder.MICROPHONE_SOURCE_CHANGED").apply {
                putExtra("previousSource", previousSource)
                putExtra("newSource", newSource)
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to send microphone source change broadcast", e)
        }
    }
    
    private fun calculateAudioLevel(buffer: ByteArray, bytesRead: Int): Int {
        var sum = 0.0
        var sampleCount = 0
        
        // Convert bytes to 16-bit samples and calculate RMS
        for (i in 0 until bytesRead - 1 step 2) {
            val sample = (buffer[i].toInt() and 0xFF) or ((buffer[i + 1].toInt() and 0xFF) shl 8)
            val signedSample = if (sample > 32767) sample - 65536 else sample
            sum += signedSample * signedSample
            sampleCount++
        }
        
        return if (sampleCount > 0) {
            kotlin.math.sqrt(sum / sampleCount).toInt()
        } else {
            0
        }
    }
    
    private fun checkForSilentAudio(audioLevel: Int, currentTime: Long) {
        if (audioLevel > audioLevelThreshold) {
            // Audio detected - reset silence tracking
            lastAudioTime = currentTime
            if (_recordingState.value == RecordingState.SILENT_DETECTED) {
                // Resume from silent state
                _recordingState.value = RecordingState.RECORDING
                isSilentWarningShown = false
                android.util.Log.d(TAG, "Audio detected - resuming from silent state")
            }
            silenceStartTime = 0L
        } else {
            // No significant audio detected
            if (silenceStartTime == 0L) {
                silenceStartTime = currentTime
            }
            
            val silenceDuration = currentTime - silenceStartTime
            if (silenceDuration >= silenceThresholdMs && !isSilentWarningShown) {
                // 10 seconds of silence detected
                _recordingState.value = RecordingState.SILENT_DETECTED
                isSilentWarningShown = true
                android.util.Log.d(TAG, "Silent audio detected after ${silenceDuration}ms")
                
                // Notify service about silent audio
                notifySilentAudioDetected()
            }
        }
    }
    
    private fun hasEnoughStorage(): Boolean {
        return try {
            val storageDir = context.getExternalFilesDir(null) ?: context.filesDir
            val availableBytes = storageDir.freeSpace
            val hasEnough = availableBytes >= minStorageBytes
            
            if (!hasEnough) {
                android.util.Log.w(TAG, "Low storage detected: ${availableBytes / (1024 * 1024)}MB available, ${minStorageBytes / (1024 * 1024)}MB required")
            }
            
            hasEnough
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to check storage", e)
            true // Assume we have storage if check fails
        }
    }
    
    private fun notifySilentAudioDetected() {
        // Send broadcast to service to show silent audio notification
        try {
            val intent = Intent("com.twinmind.voicerecorder.SILENT_AUDIO_DETECTED").apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to send silent audio broadcast", e)
        }
    }
}