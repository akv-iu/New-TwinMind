package com.twinmind.voicerecorder.domain.repository

import com.twinmind.voicerecorder.domain.model.AudioChunk
import com.twinmind.voicerecorder.domain.model.RecordingState
import kotlinx.coroutines.flow.Flow

interface AudioRecorder {
    
    fun startRecording(sessionId: String): Result<Unit>
    
    fun stopRecording(): Result<Unit>
    
    fun pauseRecording(): Result<Unit>
    
    fun resumeRecording(): Result<Unit>
    
    fun getRecordingState(): Flow<RecordingState>
    
    fun getAudioChunks(): Flow<List<AudioChunk>>
    
    fun getCurrentSessionId(): String?
    
    fun release()
}