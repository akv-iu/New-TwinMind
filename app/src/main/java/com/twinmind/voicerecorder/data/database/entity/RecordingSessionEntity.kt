package com.twinmind.voicerecorder.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recording_sessions")
data class RecordingSessionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long = 0,
    val status: String, // RECORDING, COMPLETED, TRANSCRIBING, SUMMARIZING, READY
    val totalChunks: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)