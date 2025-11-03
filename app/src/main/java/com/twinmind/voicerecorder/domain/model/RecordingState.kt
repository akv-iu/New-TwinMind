package com.twinmind.voicerecorder.domain.model

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    PAUSED_FOCUS_LOSS,  // Paused due to audio focus loss
    PAUSED_PHONE_CALL,  // Paused due to phone call
    RECORDING_SOURCE_CHANGING,  // Brief state during microphone source switch
    STOPPED,
    ERROR
}