package com.twinmind.voicerecorder.domain.repository

import com.twinmind.voicerecorder.domain.model.SessionSummary
import kotlinx.coroutines.flow.Flow

interface SummaryRepository {
    
    /**
     * Generate summary for a session using full transcript
     */
    suspend fun generateSummary(sessionId: String): Result<SessionSummary>
    
    /**
     * Get summary for a specific session
     */
    fun getSummaryForSession(sessionId: String): Flow<SessionSummary?>
    
    /**
     * Get summary for a specific session (sync)
     */
    suspend fun getSummaryForSessionSync(sessionId: String): SessionSummary?
    
    /**
     * Save summary to database
     */
    suspend fun saveSummary(summary: SessionSummary)
    
    /**
     * Update summary status
     */
    suspend fun updateSummaryStatus(sessionId: String, status: String)
    
    /**
     * Update summary with error
     */
    suspend fun updateSummaryError(sessionId: String, status: String, errorMessage: String)
    
    /**
     * Delete summary for session
     */
    suspend fun deleteSummary(sessionId: String)
}