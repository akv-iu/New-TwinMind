package com.twinmind.voicerecorder.data.audio

import android.media.MediaPlayer
import com.twinmind.voicerecorder.domain.audio.AudioPlayer
import com.twinmind.voicerecorder.domain.audio.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayerImpl @Inject constructor() : AudioPlayer {
    
    private var mediaPlayer: MediaPlayer? = null
    
    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0)
    override val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0)
    override val duration: StateFlow<Int> = _duration.asStateFlow()
    
    override fun playAudio(filePath: String): Result<Unit> {
        return try {
            // Release any existing player
            releasePlayer()
            
            // Check if file exists
            val file = File(filePath)
            if (!file.exists()) {
                _playbackState.value = PlaybackState.ERROR
                return Result.failure(Exception("Audio file not found: $filePath"))
            }
            
            // Create and configure MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                
                setOnCompletionListener {
                    _playbackState.value = PlaybackState.COMPLETED
                    _currentPosition.value = 0
                }
                
                setOnErrorListener { _, _, _ ->
                    _playbackState.value = PlaybackState.ERROR
                    true
                }
                
                // Set duration
                _duration.value = duration
                start()
                _playbackState.value = PlaybackState.PLAYING
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.ERROR
            Result.failure(e)
        }
    }
    
    override fun pauseAudio() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _currentPosition.value = player.currentPosition
                _playbackState.value = PlaybackState.PAUSED
            }
        }
    }
    
    override fun resumeAudio() {
        mediaPlayer?.let { player ->
            if (_playbackState.value == PlaybackState.PAUSED) {
                player.start()
                _playbackState.value = PlaybackState.PLAYING
            }
        }
    }
    
    override fun stopAudio() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            _currentPosition.value = 0
            _playbackState.value = PlaybackState.IDLE
        }
    }
    
    override fun seekTo(position: Int) {
        mediaPlayer?.let { player ->
            player.seekTo(position)
            _currentPosition.value = position
        }
    }
    
    override fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }
    
    override fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }
    
    private fun releasePlayer() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        _playbackState.value = PlaybackState.IDLE
        _currentPosition.value = 0
        _duration.value = 0
    }
    
    override fun release() {
        releasePlayer()
    }
}