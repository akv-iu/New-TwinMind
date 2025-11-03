package com.twinmind.voicerecorder.data.repository

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.twinmind.voicerecorder.domain.model.AudioChunk
import com.twinmind.voicerecorder.domain.model.RecordingState
import com.twinmind.voicerecorder.domain.repository.AudioChunkRepository
import com.twinmind.voicerecorder.domain.repository.AudioRecorder
import com.twinmind.voicerecorder.domain.repository.AudioStorage
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
    private val chunkRepository: AudioChunkRepository
) : AudioRecorder {
    
    private val recordingScope = CoroutineScope(Dispatchers.IO + Job())
    
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    private val _audioChunks = MutableStateFlow<List<AudioChunk>>(emptyList())
    
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var currentSessionId: String? = null
    private var chunkSequenceNumber = 0
    
    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val CHUNK_DURATION_MS = 30_000L // 30 seconds
        private const val BUFFER_SIZE_MULTIPLIER = 2
    }
    
    override fun startRecording(sessionId: String): Result<Unit> {
        return try {
            if (_recordingState.value != RecordingState.IDLE) {
                return Result.failure(IllegalStateException("Recording already in progress"))
            }
            
            currentSessionId = sessionId
            chunkSequenceNumber = 0
            
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
            
            recordingJob?.cancel()
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
            audioRecord = null
            currentSessionId = null
            
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
            
            // Track paused time
            if (_recordingState.value == RecordingState.PAUSED) {
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
            if (_recordingState.value == RecordingState.RECORDING) {
                val bytesRead = audioRecord.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    chunkData.addAll(buffer.take(bytesRead))
                    
                    // Calculate actual recording time (excluding pauses)
                    val actualRecordingTime = currentTime - recordingStartTime - totalPausedTime
                    
                    // Check if we've reached 30 seconds of actual recording time
                    if (actualRecordingTime >= CHUNK_DURATION_MS) {
                        android.util.Log.d("AudioRecorder", "Saving 30-second chunk - Sequence: $chunkSequenceNumber, Size: ${chunkData.size}, ActualTime: ${actualRecordingTime}ms")
                        saveChunk(sessionId, chunkData.toByteArray(), lastChunkTime, CHUNK_DURATION_MS)
                        chunkData.clear()
                        lastChunkTime = currentTime
                        recordingStartTime = currentTime  // Reset start time for next chunk
                        totalPausedTime = 0L  // Reset paused time for next chunk
                        chunkSequenceNumber++
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
            android.util.Log.d("AudioRecorder", "Saving final chunk - Size: ${chunkData.size}, Duration: ${finalChunkDuration}ms")
            saveChunk(sessionId, chunkData.toByteArray(), lastChunkTime, finalChunkDuration)
            android.util.Log.d("AudioRecorder", "Final chunk saved successfully")
        } else {
            android.util.Log.d("AudioRecorder", "No remaining data to save")
        }
        
        android.util.Log.d("AudioRecorder", "recordInChunks loop exited for session: $sessionId")
    }
    
    private suspend fun saveChunk(
        sessionId: String,
        audioData: ByteArray,
        startTime: Long,
        durationMs: Long
    ) {
        val chunk = AudioChunk(
            id = UUID.randomUUID().toString(),
            filePath = "", // Will be set after saving
            sessionId = sessionId,
            sequenceNumber = chunkSequenceNumber,
            startTimeMs = startTime,
            durationMs = durationMs,
            sizeBytes = audioData.size.toLong()
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
}