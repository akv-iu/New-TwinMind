package com.twinmind.voicerecorder.data.repository

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.twinmind.voicerecorder.domain.model.AudioChunk
import com.twinmind.voicerecorder.domain.model.RecordingState
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
    private val audioStorage: AudioStorage
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
            recordingJob?.cancel()
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
            audioRecord = null
            currentSessionId = null
            _recordingState.value = RecordingState.STOPPED
            
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
            audioRecord?.stop()
            _recordingState.value = RecordingState.PAUSED
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun resumeRecording(): Result<Unit> {
        return try {
            audioRecord?.startRecording()
            _recordingState.value = RecordingState.RECORDING
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
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        
        val buffer = ByteArray(bufferSize)
        val chunkData = mutableListOf<Byte>()
        val chunkStartTime = System.currentTimeMillis()
        
        var lastChunkTime = chunkStartTime
        
        while (recordingScope.isActive && _recordingState.value == RecordingState.RECORDING) {
            val audioRecord = this.audioRecord ?: break
            
            val bytesRead = audioRecord.read(buffer, 0, buffer.size)
            if (bytesRead > 0) {
                chunkData.addAll(buffer.take(bytesRead))
                
                // Check if we've reached 30 seconds
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastChunkTime >= CHUNK_DURATION_MS) {
                    saveChunk(sessionId, chunkData.toByteArray(), lastChunkTime, currentTime - lastChunkTime)
                    chunkData.clear()
                    lastChunkTime = currentTime
                    chunkSequenceNumber++
                }
            }
            
            // Small delay to prevent tight loop
            delay(10)
        }
        
        // Save any remaining data
        if (chunkData.isNotEmpty()) {
            val currentTime = System.currentTimeMillis()
            saveChunk(sessionId, chunkData.toByteArray(), lastChunkTime, currentTime - lastChunkTime)
        }
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
        
        audioStorage.saveAudioChunk(audioData, chunk).onSuccess { file ->
            val updatedChunk = chunk.copy(filePath = file.absolutePath)
            val currentChunks = _audioChunks.value.toMutableList()
            currentChunks.add(updatedChunk)
            _audioChunks.value = currentChunks
        }.onFailure {
            _recordingState.value = RecordingState.ERROR
        }
    }
}