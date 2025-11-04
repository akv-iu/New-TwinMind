package com.twinmind.voicerecorder.data.database.dao

import androidx.room.*
import com.twinmind.voicerecorder.data.database.entity.TranscriptChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptChunkDao {
    
    @Query("SELECT * FROM transcript_chunks WHERE sessionId = :sessionId ORDER BY chunkSequence ASC")
    fun getTranscriptChunksForSession(sessionId: String): Flow<List<TranscriptChunkEntity>>
    
    @Query("SELECT * FROM transcript_chunks WHERE sessionId = :sessionId ORDER BY chunkSequence ASC")
    suspend fun getTranscriptChunksForSessionSync(sessionId: String): List<TranscriptChunkEntity>
    
    @Query("SELECT * FROM transcript_chunks WHERE chunkId = :chunkId")
    suspend fun getTranscriptForChunk(chunkId: String): TranscriptChunkEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscriptChunk(transcriptChunk: TranscriptChunkEntity)
    
    @Update
    suspend fun updateTranscriptChunk(transcriptChunk: TranscriptChunkEntity)
    
    @Query("UPDATE transcript_chunks SET status = :status WHERE chunkId = :chunkId")
    suspend fun updateTranscriptChunkStatus(chunkId: String, status: String)
    
    @Query("DELETE FROM transcript_chunks WHERE sessionId = :sessionId")
    suspend fun deleteTranscriptChunksForSession(sessionId: String)
}