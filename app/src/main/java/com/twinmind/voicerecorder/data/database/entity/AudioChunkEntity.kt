package com.twinmind.voicerecorder.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_chunks",
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
data class AudioChunkEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val sequenceNumber: Int,
    val filePath: String,
    val startTimeMs: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis()
)