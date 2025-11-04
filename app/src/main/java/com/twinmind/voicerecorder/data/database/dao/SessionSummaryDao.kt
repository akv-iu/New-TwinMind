package com.twinmind.voicerecorder.data.database.dao

import androidx.room.*
import com.twinmind.voicerecorder.data.database.entity.SessionSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionSummaryDao {
    
    @Query("SELECT * FROM session_summaries WHERE sessionId = :sessionId")
    fun getSummaryForSession(sessionId: String): Flow<SessionSummaryEntity?>
    
    @Query("SELECT * FROM session_summaries WHERE sessionId = :sessionId")
    suspend fun getSummaryForSessionSync(sessionId: String): SessionSummaryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: SessionSummaryEntity)
    
    @Update
    suspend fun updateSummary(summary: SessionSummaryEntity)
    
    @Query("UPDATE session_summaries SET status = :status, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun updateSummaryStatus(sessionId: String, status: String, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE session_summaries SET status = :status, errorMessage = :errorMessage, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun updateSummaryError(sessionId: String, status: String, errorMessage: String, updatedAt: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM session_summaries WHERE sessionId = :sessionId")
    suspend fun deleteSummary(sessionId: String)
    
    @Query("DELETE FROM session_summaries")
    suspend fun deleteAllSummaries()
}