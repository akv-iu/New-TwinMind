package com.twinmind.voicerecorder.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_summaries",
    foreignKeys = [
        ForeignKey(
            entity = RecordingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class SessionSummaryEntity(
    @PrimaryKey
    val sessionId: String,           // One summary per session
    val title: String,
    val summary: String,
    val actionItems: String,         // Simple text format, not JSON
    val keyPoints: String,           // Simple text format, not JSON
    val status: String,              // GENERATING, COMPLETED, FAILED
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)