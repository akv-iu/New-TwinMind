package com.twinmind.voicerecorder.domain.model

data class RecordingSession(
    val id: String,
    val title: String,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long = 0,
    val status: SessionStatus,
    val totalChunks: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SessionStatus {
    RECORDING,
    COMPLETED,
    TRANSCRIBING,
    SUMMARIZING,
    READY,
    FAILED
}