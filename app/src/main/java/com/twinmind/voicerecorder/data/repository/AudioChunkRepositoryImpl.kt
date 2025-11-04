package com.twinmind.voicerecorder.data.repository

import com.twinmind.voicerecorder.data.database.dao.AudioChunkDao
import com.twinmind.voicerecorder.data.database.dao.RecordingSessionDao
import com.twinmind.voicerecorder.data.database.entity.AudioChunkEntity
import com.twinmind.voicerecorder.domain.model.AudioChunk
import com.twinmind.voicerecorder.domain.repository.AudioChunkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioChunkRepositoryImpl @Inject constructor(
    private val audioChunkDao: AudioChunkDao,
    private val sessionDao: RecordingSessionDao
) : AudioChunkRepository {
    
    override suspend fun saveChunk(chunk: AudioChunk) {
        android.util.Log.d("AudioChunkRepository", "Saving chunk: ${chunk.id}, session: ${chunk.sessionId}, sequence: ${chunk.sequenceNumber}")
        val entity = chunk.toEntity()
        audioChunkDao.insertChunk(entity)
        android.util.Log.d("AudioChunkRepository", "Chunk inserted to DAO successfully")
        // Update session's total chunk count
        updateSessionChunkCount(chunk.sessionId)
        android.util.Log.d("AudioChunkRepository", "Session chunk count updated")
    }
    
    override suspend fun getChunksForSession(sessionId: String): List<AudioChunk> {
        return audioChunkDao.getChunksForSessionSync(sessionId).map { it.toDomainModel() }
    }
    
    override fun getChunksForSessionFlow(sessionId: String): Flow<List<AudioChunk>> {
        return audioChunkDao.getChunksForSession(sessionId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun deleteChunksForSession(sessionId: String) {
        audioChunkDao.deleteChunksForSession(sessionId)
        // Reset session's chunk count
        updateSessionChunkCount(sessionId)
    }
    
    override suspend fun updateSessionChunkCount(sessionId: String) {
        val chunkCount = audioChunkDao.getChunksForSessionSync(sessionId).size
        // Update the session's totalChunks field
        val session = sessionDao.getSessionById(sessionId)
        if (session != null) {
            val updatedSession = session.copy(totalChunks = chunkCount)
            sessionDao.updateSession(updatedSession)
        }
    }
    
    private fun AudioChunk.toEntity(): AudioChunkEntity {
        return AudioChunkEntity(
            id = id,
            filePath = filePath,
            sessionId = sessionId,
            sequenceNumber = sequenceNumber,
            startTimeMs = startTimeMs,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            createdAt = System.currentTimeMillis()
        )
    }
    
    private fun AudioChunkEntity.toDomainModel(): AudioChunk {
        return AudioChunk(
            id = id,
            filePath = filePath,
            sessionId = sessionId,
            sequenceNumber = sequenceNumber,
            startTimeMs = startTimeMs,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            createdAt = createdAt
        )
    }
}
