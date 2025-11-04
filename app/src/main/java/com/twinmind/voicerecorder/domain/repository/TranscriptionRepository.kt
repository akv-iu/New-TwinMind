package com.twinmind.voicerecorder.domain.repository

import com.twinmind.voicerecorder.domain.model.TranscriptChunk
import kotlinx.coroutines.flow.Flow

interface TranscriptionRepository {
    
    /**
     * Transcribe an audio chunk and save to database
     */
    suspend fun transcribeAudioChunk(
        chunkId: String,
        sessionId: String,
        chunkSequence: Int,
        audioFilePath: String
    ): Result<TranscriptChunk>
    
    /**
     * Get all transcript chunks for a session
     */
    fun getTranscriptChunksForSession(sessionId: String): Flow<List<TranscriptChunk>>
    
    /**
     * Get transcript chunks for a session (sync)
     */
    suspend fun getTranscriptChunksForSessionSync(sessionId: String): List<TranscriptChunk>
    
    /**
     * Get transcript for a specific chunk
     */
    suspend fun getTranscriptForChunk(chunkId: String): TranscriptChunk?
    
    /**
     * Update transcript chunk status
     */
    suspend fun updateTranscriptStatus(chunkId: String, status: String)
    
    /**
     * Get full session transcript (concatenated chunks in order)
     */
    suspend fun getFullSessionTranscript(sessionId: String): String
}