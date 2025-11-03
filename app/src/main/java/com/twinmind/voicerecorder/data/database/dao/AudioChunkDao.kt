package com.twinmind.voicerecorder.data.database.dao

import androidx.room.*
import com.twinmind.voicerecorder.data.database.entity.AudioChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioChunkDao {
    
    @Query("SELECT * FROM audio_chunks WHERE sessionId = :sessionId ORDER BY sequenceNumber ASC")
    fun getChunksForSession(sessionId: String): Flow<List<AudioChunkEntity>>
    
    @Query("SELECT * FROM audio_chunks WHERE sessionId = :sessionId ORDER BY sequenceNumber ASC")
    suspend fun getChunksForSessionSync(sessionId: String): List<AudioChunkEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: AudioChunkEntity)
    
    @Delete
    suspend fun deleteChunk(chunk: AudioChunkEntity)
    
    @Query("DELETE FROM audio_chunks WHERE sessionId = :sessionId")
    suspend fun deleteChunksForSession(sessionId: String)
}