package com.twinmind.voicerecorder.domain.model

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    STOPPED,
    ERROR
}