package com.twinmind.voicerecorder.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SessionSummary(
    val sessionId: String,
    val title: String,
    val summary: String,
    val actionItems: String,         // Simple text format for dev level
    val keyPoints: String,           // Simple text format for dev level
    val status: SummaryStatus,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable

enum class SummaryStatus {
    GENERATING,
    COMPLETED,
    FAILED
}