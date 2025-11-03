package com.twinmind.voicerecorder.domain.audio

import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val playbackState: StateFlow<PlaybackState>
    val currentPosition: StateFlow<Int>
    val duration: StateFlow<Int>
    
    fun playAudio(filePath: String): Result<Unit>
    fun pauseAudio()
    fun resumeAudio()
    fun stopAudio()
    fun seekTo(position: Int)
    fun getCurrentPosition(): Int
    fun getDuration(): Int
    fun release()
}