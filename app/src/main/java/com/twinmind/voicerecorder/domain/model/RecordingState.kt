package com.twinmind.voicerecorder.domain.model

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    PAUSED_FOCUS_LOSS,  // Paused due to audio focus loss
    PAUSED_PHONE_CALL,  // Paused due to phone call
    RECORDING_SOURCE_CHANGING,  // Brief state during microphone source switch
    SILENT_DETECTED,    // Recording but no audio input detected for 10+ seconds
    STOPPED,
    STOPPED_LOW_STORAGE, // Recording stopped due to insufficient storage
    ERROR
}