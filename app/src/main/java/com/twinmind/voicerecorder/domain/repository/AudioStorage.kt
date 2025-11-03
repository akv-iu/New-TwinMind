package com.twinmind.voicerecorder.domain.repository

import com.twinmind.voicerecorder.domain.model.AudioChunk
import java.io.File

interface AudioStorage {
    
    suspend fun saveAudioChunk(audioData: ByteArray, chunk: AudioChunk): Result<File>
    
    suspend fun getAudioChunk(chunkId: String): Result<File>
    
    suspend fun deleteAudioChunk(chunkId: String): Result<Unit>
    
    suspend fun getAvailableStorageBytes(): Long
    
    suspend fun hasMinimumStorage(requiredBytes: Long): Boolean
    
    suspend fun cleanup(olderThanMs: Long): Result<Unit>
    
    suspend fun getSessionAudioFiles(sessionId: String): Result<List<File>>
}