package com.twinmind.voicerecorder.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinmind.voicerecorder.domain.audio.PlaybackState
import com.twinmind.voicerecorder.domain.audio.AudioPlayer
import com.twinmind.voicerecorder.domain.repository.AudioChunkRepository
import com.twinmind.voicerecorder.domain.repository.AudioStorage
import com.twinmind.voicerecorder.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val audioPlayer: AudioPlayer,
    private val audioStorage: AudioStorage,
    private val chunkRepository: AudioChunkRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    val playbackState = audioPlayer.playbackState
    val currentPosition = audioPlayer.currentPosition
    val duration = audioPlayer.duration
    
    fun playRecording(sessionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Debug: Log session ID being searched
                android.util.Log.d("SessionDetailViewModel", "Looking for audio chunks for session: $sessionId")
                
                // Get audio chunks from database for this session
                val chunks = chunkRepository.getChunksForSession(sessionId)
                android.util.Log.d("SessionDetailViewModel", "Found ${chunks.size} audio chunks in database")
                
                if (chunks.isNotEmpty()) {
                    // Sort chunks by sequence number and get the first one
                    val firstChunk = chunks.sortedBy { it.sequenceNumber }.first()
                    android.util.Log.d("SessionDetailViewModel", "Playing first chunk: ${firstChunk.filePath}")
                    
                    // Check if the file actually exists
                    val file = java.io.File(firstChunk.filePath)
                    if (file.exists()) {
                        val playResult = audioPlayer.playAudio(firstChunk.filePath)
                        
                        if (playResult.isFailure) {
                            _errorMessage.value = "Failed to play recording: ${playResult.exceptionOrNull()?.message}"
                        }
                    } else {
                        android.util.Log.e("SessionDetailViewModel", "Audio file does not exist: ${firstChunk.filePath}")
                        _errorMessage.value = "Audio file not found: ${firstChunk.filePath}"
                    }
                } else {
                    android.util.Log.w("SessionDetailViewModel", "No audio chunks found in database for session: $sessionId")
                    _errorMessage.value = "Audio too short!!"
                }
            } catch (e: Exception) {
                android.util.Log.e("SessionDetailViewModel", "Error playing recording", e)
                _errorMessage.value = "Error playing recording: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun pausePlayback() {
        audioPlayer.pauseAudio()
    }
    
    fun resumePlayback() {
        audioPlayer.resumeAudio()
    }
    
    fun stopPlayback() {
        audioPlayer.stopAudio()
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                sessionRepository.deleteSession(sessionId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete session: ${e.message}"
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}