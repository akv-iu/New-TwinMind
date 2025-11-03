package com.twinmind.voicerecorder.data.database.dao

import androidx.room.*
import com.twinmind.voicerecorder.data.database.entity.RecordingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingSessionDao {
    
    @Query("SELECT * FROM recording_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<RecordingSessionEntity>>
    
    @Query("SELECT * FROM recording_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): RecordingSessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: RecordingSessionEntity)
    
    @Update
    suspend fun updateSession(session: RecordingSessionEntity)
    
    @Delete
    suspend fun deleteSession(session: RecordingSessionEntity)
    
    @Query("UPDATE recording_sessions SET status = :status WHERE id = :sessionId")
    suspend fun updateSessionStatus(sessionId: String, status: String)
    
    @Query("UPDATE recording_sessions SET duration = :duration, endTime = :endTime WHERE id = :sessionId")
    suspend fun updateSessionDuration(sessionId: String, duration: Long, endTime: Long)
}