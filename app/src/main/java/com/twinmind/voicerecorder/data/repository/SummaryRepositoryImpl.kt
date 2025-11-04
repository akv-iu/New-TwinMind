package com.twinmind.voicerecorder.data.repository

import android.util.Log
import com.twinmind.voicerecorder.data.database.dao.SessionSummaryDao
import com.twinmind.voicerecorder.data.database.entity.SessionSummaryEntity
import com.twinmind.voicerecorder.data.transcription.TranscriptionService
import com.twinmind.voicerecorder.domain.model.SessionSummary
import com.twinmind.voicerecorder.domain.model.SummaryStatus
import com.twinmind.voicerecorder.domain.repository.SummaryRepository
import com.twinmind.voicerecorder.domain.repository.TranscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryRepositoryImpl @Inject constructor(
    private val sessionSummaryDao: SessionSummaryDao,
    private val transcriptionRepository: TranscriptionRepository
) : SummaryRepository {
    
    companion object {
        private const val TAG = "SummaryRepository"
    }
    
    override suspend fun generateSummary(sessionId: String): Result<SessionSummary> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting summary generation for session: $sessionId")
                
                // Create initial summary with GENERATING status
                val initialSummary = SessionSummary(
                    sessionId = sessionId,
                    title = "",
                    summary = "",
                    actionItems = "",
                    keyPoints = "",
                    status = SummaryStatus.GENERATING
                )
                
                // Save to database with GENERATING status
                sessionSummaryDao.insertSummary(initialSummary.toEntity())
                Log.d(TAG, "Saved generating summary status to database")
                
                // Get full transcript from transcription repository
                val fullTranscript = transcriptionRepository.getFullSessionTranscript(sessionId)
                
                if (fullTranscript.isBlank()) {
                    Log.w(TAG, "❌ No transcript found for session: $sessionId")
                    val errorSummary = initialSummary.copy(
                        status = SummaryStatus.FAILED,
                        errorMessage = "No transcript available for this session"
                    )
                    sessionSummaryDao.insertSummary(errorSummary.toEntity())
                    return@withContext Result.failure(Exception("No transcript available"))
                }
                
                Log.d(TAG, "📝 Got transcript for summary (${fullTranscript.length} characters)")
                
                // Call Gemini API for summary generation
                val summaryResponse = TranscriptionService.generateSummary(fullTranscript)
                
                val finalSummary = if (summaryResponse != null && summaryResponse.isNotBlank()) {
                    // Parse the structured response
                    val parsedSummary = parseSummaryResponse(summaryResponse)
                    
                    // Success - update with generated content
                    Log.d(TAG, "✅ Summary generated successfully")
                    val successSummary = initialSummary.copy(
                        title = parsedSummary.title,
                        summary = parsedSummary.summary,
                        actionItems = parsedSummary.actionItems,
                        keyPoints = parsedSummary.keyPoints,
                        status = SummaryStatus.COMPLETED,
                        updatedAt = System.currentTimeMillis()
                    )
                    sessionSummaryDao.insertSummary(successSummary.toEntity())
                    successSummary
                } else {
                    // Failed - update with error status
                    Log.w(TAG, "❌ Summary generation failed for session: $sessionId")
                    val failedSummary = initialSummary.copy(
                        title = "Summary Generation Failed",
                        summary = "Unable to generate summary at this time",
                        actionItems = "• Please try again later",
                        keyPoints = "• Check your internet connection",
                        status = SummaryStatus.FAILED,
                        errorMessage = "Failed to generate summary from transcript",
                        updatedAt = System.currentTimeMillis()
                    )
                    sessionSummaryDao.insertSummary(failedSummary.toEntity())
                    failedSummary
                }
                
                Log.d(TAG, "Final summary saved with status: ${finalSummary.status}")
                Result.success(finalSummary)
                
            } catch (e: Exception) {
                Log.e(TAG, "💥 Error generating summary", e)
                
                // Update status to FAILED in database
                try {
                    sessionSummaryDao.updateSummaryError(
                        sessionId = sessionId,
                        status = SummaryStatus.FAILED.name,
                        errorMessage = e.message ?: "Unknown error occurred"
                    )
                } catch (dbException: Exception) {
                    Log.e(TAG, "Failed to update summary error status", dbException)
                }
                
                Result.failure(e)
            }
        }
    }
    
    override fun getSummaryForSession(sessionId: String): Flow<SessionSummary?> {
        return sessionSummaryDao.getSummaryForSession(sessionId).map { entity ->
            entity?.toDomainModel()
        }
    }
    
    override suspend fun getSummaryForSessionSync(sessionId: String): SessionSummary? {
        return sessionSummaryDao.getSummaryForSessionSync(sessionId)?.toDomainModel()
    }
    
    override suspend fun saveSummary(summary: SessionSummary) {
        sessionSummaryDao.insertSummary(summary.toEntity())
    }
    
    override suspend fun updateSummaryStatus(sessionId: String, status: String) {
        sessionSummaryDao.updateSummaryStatus(sessionId, status)
    }
    
    override suspend fun updateSummaryError(sessionId: String, status: String, errorMessage: String) {
        sessionSummaryDao.updateSummaryError(sessionId, status, errorMessage)
    }
    
    override suspend fun deleteSummary(sessionId: String) {
        sessionSummaryDao.deleteSummary(sessionId)
    }
    
    /**
     * Parse the structured summary response from Gemini API
     */
    private fun parseSummaryResponse(response: String): SessionSummary {
        try {
            val lines = response.lines()
            var title = ""
            var summary = ""
            val actionItems = mutableListOf<String>()
            val keyPoints = mutableListOf<String>()
            
            var currentSection = ""
            
            for (line in lines) {
                val trimmedLine = line.trim()
                when {
                    trimmedLine.startsWith("TITLE:") -> {
                        title = trimmedLine.substringAfter("TITLE:").trim()
                        currentSection = "title"
                    }
                    trimmedLine.startsWith("SUMMARY:") -> {
                        summary = trimmedLine.substringAfter("SUMMARY:").trim()
                        currentSection = "summary"
                    }
                    trimmedLine.startsWith("ACTION ITEMS:") -> {
                        currentSection = "actions"
                    }
                    trimmedLine.startsWith("KEY POINTS:") -> {
                        currentSection = "keypoints"
                    }
                    trimmedLine.startsWith("•") -> {
                        val item = trimmedLine.substringAfter("•").trim()
                        when (currentSection) {
                            "actions" -> actionItems.add(item)
                            "keypoints" -> keyPoints.add(item)
                        }
                    }
                    trimmedLine.isNotEmpty() && currentSection == "summary" && !trimmedLine.startsWith("ACTION") -> {
                        // Continue multi-line summary
                        summary += " $trimmedLine"
                    }
                }
            }
            
            // Fallback values if parsing fails
            if (title.isBlank()) title = "Meeting Summary"
            if (summary.isBlank()) summary = "Summary could not be generated from the transcript."
            
            return SessionSummary(
                sessionId = "",  // Will be set by caller
                title = title,
                summary = summary.trim(),
                actionItems = if (actionItems.isNotEmpty()) actionItems.joinToString("\n• ", "• ") else "• None identified",
                keyPoints = if (keyPoints.isNotEmpty()) keyPoints.joinToString("\n• ", "• ") else "• None identified",
                status = SummaryStatus.COMPLETED
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing summary response", e)
            return SessionSummary(
                sessionId = "",
                title = "Parsing Error",
                summary = "Could not parse the generated summary.",
                actionItems = "• Please try generating the summary again",
                keyPoints = "• Check the transcript quality",
                status = SummaryStatus.FAILED,
                errorMessage = "Failed to parse summary response"
            )
        }
    }
    
    // Extension functions for entity conversion
    private fun SessionSummary.toEntity(): SessionSummaryEntity {
        return SessionSummaryEntity(
            sessionId = sessionId,
            title = title,
            summary = summary,
            actionItems = actionItems,
            keyPoints = keyPoints,
            status = status.name,
            errorMessage = errorMessage,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    private fun SessionSummaryEntity.toDomainModel(): SessionSummary {
        return SessionSummary(
            sessionId = sessionId,
            title = title,
            summary = summary,
            actionItems = actionItems,
            keyPoints = keyPoints,
            status = SummaryStatus.valueOf(status),
            errorMessage = errorMessage,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}