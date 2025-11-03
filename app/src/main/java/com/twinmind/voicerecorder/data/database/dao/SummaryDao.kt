package com.twinmind.voicerecorder.data.database.dao

import androidx.room.*
import com.twinmind.voicerecorder.data.database.entity.SummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    
    @Query("SELECT * FROM summaries WHERE sessionId = :sessionId")
    fun getSummaryForSession(sessionId: String): Flow<SummaryEntity?>
    
    @Query("SELECT * FROM summaries WHERE sessionId = :sessionId")
    suspend fun getSummaryForSessionSync(sessionId: String): SummaryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: SummaryEntity)
    
    @Update
    suspend fun updateSummary(summary: SummaryEntity)
    
    @Query("UPDATE summaries SET status = :status WHERE sessionId = :sessionId")
    suspend fun updateSummaryStatus(sessionId: String, status: String)
}