package com.twinmind.voicerecorder.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TranscriptChunk(
    val id: String,
    val sessionId: String,
    val chunkId: String,
    val chunkSequence: Int,
    val transcriptText: String,
    val status: TranscriptionStatus,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

enum class TranscriptionStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}