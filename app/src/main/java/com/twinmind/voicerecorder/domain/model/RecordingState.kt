package com.twinmind.voicerecorder.domain.model

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    PAUSED_FOCUS_LOSS,  // Paused due to audio focus loss
    STOPPED,
    ERROR
}