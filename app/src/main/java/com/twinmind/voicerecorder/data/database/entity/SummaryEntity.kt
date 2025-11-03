package com.twinmind.voicerecorder.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "summaries",
    foreignKeys = [
        ForeignKey(
            entity = RecordingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["sessionId"])]
)
data class SummaryEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val title: String,
    val summary: String,
    val actionItems: String, // JSON string
    val keyPoints: String,   // JSON string
    val status: String, // PENDING, PROCESSING, COMPLETED, FAILED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)