package com.twinmind.voicerecorder.data.repository

import com.twinmind.voicerecorder.data.database.dao.RecordingSessionDao
import com.twinmind.voicerecorder.data.database.entity.RecordingSessionEntity
import com.twinmind.voicerecorder.domain.model.RecordingSession
import com.twinmind.voicerecorder.domain.model.SessionStatus
import com.twinmind.voicerecorder.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: RecordingSessionDao
) : SessionRepository {
    
    override fun getAllSessions(): Flow<List<RecordingSession>> {
        return sessionDao.getAllSessions().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun getSessionById(sessionId: String): RecordingSession? {
        return sessionDao.getSessionById(sessionId)?.toDomainModel()
    }
    
    override suspend fun createSession(title: String): RecordingSession {
        val session = RecordingSessionEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            startTime = System.currentTimeMillis(),
            status = SessionStatus.RECORDING.name
        )
        sessionDao.insertSession(session)
        return session.toDomainModel()
    }
    
    override suspend fun updateSession(session: RecordingSession) {
        sessionDao.updateSession(session.toEntity())
    }
    
    override suspend fun deleteSession(sessionId: String) {
        val session = sessionDao.getSessionById(sessionId)
        if (session != null) {
            sessionDao.deleteSession(session)
        }
    }
    
    override suspend fun updateSessionStatus(sessionId: String, status: String) {
        sessionDao.updateSessionStatus(sessionId, status)
    }
    
    override suspend fun completeSession(sessionId: String, duration: Long) {
        val endTime = System.currentTimeMillis()
        sessionDao.updateSessionDuration(sessionId, duration, endTime)
        sessionDao.updateSessionStatus(sessionId, SessionStatus.COMPLETED.name)
    }
    
    override suspend fun createSessionWithId(sessionId: String, title: String): RecordingSession {
        val session = RecordingSessionEntity(
            id = sessionId,
            title = title,
            startTime = System.currentTimeMillis(),
            status = SessionStatus.RECORDING.name
        )
        sessionDao.insertSession(session)
        return session.toDomainModel()
    }
    
    private fun RecordingSessionEntity.toDomainModel(): RecordingSession {
        return RecordingSession(
            id = id,
            title = title,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            status = SessionStatus.valueOf(status),
            totalChunks = totalChunks,
            createdAt = createdAt
        )
    }
    
    private fun RecordingSession.toEntity(): RecordingSessionEntity {
        return RecordingSessionEntity(
            id = id,
            title = title,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            status = status.name,
            totalChunks = totalChunks,
            createdAt = createdAt
        )
    }
}