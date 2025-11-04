package com.twinmind.voicerecorder.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinmind.voicerecorder.domain.model.TranscriptChunk
import com.twinmind.voicerecorder.domain.repository.TranscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TranscriptViewModel @Inject constructor(
    private val transcriptionRepository: TranscriptionRepository
) : ViewModel() {
    
    private val _transcriptChunks = MutableStateFlow<List<TranscriptChunk>>(emptyList())
    val transcriptChunks: StateFlow<List<TranscriptChunk>> = _transcriptChunks.asStateFlow()
    
    private val _fullTranscript = MutableStateFlow("")
    val fullTranscript: StateFlow<String> = _fullTranscript.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun loadTranscripts(sessionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Collect real-time updates from the repository
                transcriptionRepository.getTranscriptChunksForSession(sessionId).collect { chunks ->
                    _transcriptChunks.value = chunks.sortedBy { it.chunkSequence }
                    
                    // Update full transcript
                    _fullTranscript.value = transcriptionRepository.getFullSessionTranscript(sessionId)
                    
                    // Stop loading after first data load
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                android.util.Log.e("TranscriptViewModel", "Error loading transcripts", e)
                _isLoading.value = false
            }
        }
    }
    
    fun refreshTranscripts(sessionId: String) {
        viewModelScope.launch {
            try {
                val chunks = transcriptionRepository.getTranscriptChunksForSessionSync(sessionId)
                _transcriptChunks.value = chunks.sortedBy { it.chunkSequence }
                
                val fullText = transcriptionRepository.getFullSessionTranscript(sessionId)
                _fullTranscript.value = fullText
            } catch (e: Exception) {
                android.util.Log.e("TranscriptViewModel", "Error refreshing transcripts", e)
            }
        }
    }
}