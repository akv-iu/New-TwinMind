package com.twinmind.voicerecorder.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcript_chunks",
    foreignKeys = [
        ForeignKey(
            entity = RecordingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AudioChunkEntity::class,
            parentColumns = ["id"],
            childColumns = ["chunkId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["chunkId"]),
        Index(value = ["sessionId", "chunkSequence"])
    ]
)
data class TranscriptChunkEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val chunkId: String,
    val chunkSequence: Int,
    val transcriptText: String,
    val status: String, // PENDING, IN_PROGRESS, COMPLETED, FAILED
    val createdAt: Long = System.currentTimeMillis()
)