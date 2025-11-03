package com.twinmind.voicerecorder.domain.repository

import com.twinmind.voicerecorder.domain.model.AudioChunk
import kotlinx.coroutines.flow.Flow

interface AudioChunkRepository {
    
    suspend fun saveChunk(chunk: AudioChunk)
    
    suspend fun getChunksForSession(sessionId: String): List<AudioChunk>
    
    fun getChunksForSessionFlow(sessionId: String): Flow<List<AudioChunk>>
    
    suspend fun deleteChunksForSession(sessionId: String)
    
    suspend fun updateSessionChunkCount(sessionId: String)
}