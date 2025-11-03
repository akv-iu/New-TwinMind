package com.twinmind.voicerecorder.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AudioChunk(
    val id: String,
    val filePath: String,
    val sessionId: String,
    val sequenceNumber: Int,
    val startTimeMs: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable