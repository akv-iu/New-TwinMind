package com.twinmind.voicerecorder.data.database.dao

import androidx.room.*
import com.twinmind.voicerecorder.data.database.entity.TranscriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {
    
    @Query("SELECT * FROM transcripts WHERE sessionId = :sessionId")
    fun getTranscriptForSession(sessionId: String): Flow<TranscriptEntity?>
    
    @Query("SELECT * FROM transcripts WHERE sessionId = :sessionId")
    suspend fun getTranscriptForSessionSync(sessionId: String): TranscriptEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscript(transcript: TranscriptEntity)
    
    @Update
    suspend fun updateTranscript(transcript: TranscriptEntity)
    
    @Query("UPDATE transcripts SET status = :status WHERE sessionId = :sessionId")
    suspend fun updateTranscriptStatus(sessionId: String, status: String)
}