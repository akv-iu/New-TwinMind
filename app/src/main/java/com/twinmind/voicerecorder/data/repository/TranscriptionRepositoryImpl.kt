package com.twinmind.voicerecorder.data.repository

import android.util.Log
import com.twinmind.voicerecorder.data.database.dao.TranscriptChunkDao
import com.twinmind.voicerecorder.data.database.entity.TranscriptChunkEntity
import com.twinmind.voicerecorder.data.transcription.TranscriptionService
import com.twinmind.voicerecorder.domain.model.TranscriptChunk
import com.twinmind.voicerecorder.domain.model.TranscriptionStatus
import com.twinmind.voicerecorder.domain.repository.TranscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionRepositoryImpl @Inject constructor(
    private val transcriptChunkDao: TranscriptChunkDao
) : TranscriptionRepository {
    
    companion object {
        private const val TAG = "TranscriptionRepository"
    }
    
    override suspend fun transcribeAudioChunk(
        chunkId: String,
        sessionId: String,
        chunkSequence: Int,
        audioFilePath: String
    ): Result<TranscriptChunk> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting transcription for chunk: $chunkId, sequence: $chunkSequence")
                
                // Create initial transcript chunk with PENDING status
                val transcriptChunkId = UUID.randomUUID().toString()
                val initialChunk = TranscriptChunk(
                    id = transcriptChunkId,
                    sessionId = sessionId,
                    chunkId = chunkId,
                    chunkSequence = chunkSequence,
                    transcriptText = "",
                    status = TranscriptionStatus.PENDING
                )
                
                // Save to database with PENDING status
                transcriptChunkDao.insertTranscriptChunk(initialChunk.toEntity())
                Log.d(TAG, "Saved pending transcript chunk to database")
                
                // Update status to IN_PROGRESS
                transcriptChunkDao.updateTranscriptChunkStatus(chunkId, TranscriptionStatus.IN_PROGRESS.name)
                
                // Call Gemini API for transcription
                val transcribedText = TranscriptionService.transcribeAudio(audioFilePath)
                
                val finalChunk = if (transcribedText != null && transcribedText.isNotBlank()) {
                    // Success - update with transcribed text
                    Log.d(TAG, "✅ Transcription successful: $transcribedText")
                    val successChunk = initialChunk.copy(
                        transcriptText = transcribedText,
                        status = TranscriptionStatus.COMPLETED
                    )
                    transcriptChunkDao.insertTranscriptChunk(successChunk.toEntity())
                    successChunk
                } else {
                    // Failed - update with error status
                    Log.w(TAG, "❌ Transcription failed for chunk: $chunkId")
                    val failedChunk = initialChunk.copy(
                        transcriptText = "[Transcription failed]",
                        status = TranscriptionStatus.FAILED
                    )
                    transcriptChunkDao.insertTranscriptChunk(failedChunk.toEntity())
                    failedChunk
                }
                
                Log.d(TAG, "Final transcript chunk saved: ${finalChunk.status}")
                Result.success(finalChunk)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during transcription", e)
                // Update status to FAILED in database
                try {
                    transcriptChunkDao.updateTranscriptChunkStatus(chunkId, TranscriptionStatus.FAILED.name)
                } catch (dbException: Exception) {
                    Log.e(TAG, "Failed to update status to FAILED", dbException)
                }
                Result.failure(e)
            }
        }
    }
    
    override fun getTranscriptChunksForSession(sessionId: String): Flow<List<TranscriptChunk>> {
        return transcriptChunkDao.getTranscriptChunksForSession(sessionId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun getTranscriptChunksForSessionSync(sessionId: String): List<TranscriptChunk> {
        return transcriptChunkDao.getTranscriptChunksForSessionSync(sessionId).map { it.toDomainModel() }
    }
    
    override suspend fun getTranscriptForChunk(chunkId: String): TranscriptChunk? {
        return transcriptChunkDao.getTranscriptForChunk(chunkId)?.toDomainModel()
    }
    
    override suspend fun updateTranscriptStatus(chunkId: String, status: String) {
        transcriptChunkDao.updateTranscriptChunkStatus(chunkId, status)
    }
    
    override suspend fun getFullSessionTranscript(sessionId: String): String {
        val chunks = getTranscriptChunksForSessionSync(sessionId)
            .filter { it.status == TranscriptionStatus.COMPLETED }
            .sortedBy { it.chunkSequence }
        
        return chunks.joinToString(" ") { it.transcriptText }
    }
    
    // Extension functions for entity conversion
    private fun TranscriptChunk.toEntity(): TranscriptChunkEntity {
        return TranscriptChunkEntity(
            id = id,
            sessionId = sessionId,
            chunkId = chunkId,
            chunkSequence = chunkSequence,
            transcriptText = transcriptText,
            status = status.name,
            createdAt = createdAt
        )
    }
    
    private fun TranscriptChunkEntity.toDomainModel(): TranscriptChunk {
        return TranscriptChunk(
            id = id,
            sessionId = sessionId,
            chunkId = chunkId,
            chunkSequence = chunkSequence,
            transcriptText = transcriptText,
            status = TranscriptionStatus.valueOf(status),
            createdAt = createdAt
        )
    }
}