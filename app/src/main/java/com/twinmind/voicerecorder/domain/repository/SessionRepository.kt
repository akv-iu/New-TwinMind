package com.twinmind.voicerecorder.domain.repository

import com.twinmind.voicerecorder.domain.model.RecordingSession
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    
    fun getAllSessions(): Flow<List<RecordingSession>>
    
    suspend fun getSessionById(sessionId: String): RecordingSession?
    
    suspend fun createSession(title: String): RecordingSession
    
    suspend fun createSessionWithId(sessionId: String, title: String): RecordingSession
    
    suspend fun updateSession(session: RecordingSession)
    
    suspend fun deleteSession(sessionId: String)
    
    suspend fun updateSessionStatus(sessionId: String, status: String)
    
    suspend fun completeSession(sessionId: String, duration: Long)
}