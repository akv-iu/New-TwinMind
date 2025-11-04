package com.twinmind.voicerecorder.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinmind.voicerecorder.domain.model.SessionSummary
import com.twinmind.voicerecorder.domain.repository.SummaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val summaryRepository: SummaryRepository
) : ViewModel() {
    
    private val _summary = MutableStateFlow<SessionSummary?>(null)
    val summary: StateFlow<SessionSummary?> = _summary.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun loadSummary(sessionId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                // Check if summary already exists
                val existingSummary = summaryRepository.getSummaryForSessionSync(sessionId)
                
                if (existingSummary != null) {
                    android.util.Log.d("SummaryViewModel", "Found existing summary with status: ${existingSummary.status}")
                    _summary.value = existingSummary
                    _isLoading.value = false
                    
                    // If it's still generating, start listening for updates
                    if (existingSummary.status.name == "GENERATING") {
                        listenForSummaryUpdates(sessionId)
                    }
                } else {
                    android.util.Log.d("SummaryViewModel", "No existing summary found for session: $sessionId")
                    _summary.value = null
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                android.util.Log.e("SummaryViewModel", "Error loading summary", e)
                _errorMessage.value = "Failed to load summary: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun generateSummary(sessionId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("SummaryViewModel", "Starting summary generation for session: $sessionId")
                _isLoading.value = true
                _errorMessage.value = null
                
                // Start listening for real-time updates
                listenForSummaryUpdates(sessionId)
                
                // Generate summary in background
                val result = summaryRepository.generateSummary(sessionId)
                
                result.fold(
                    onSuccess = { generatedSummary ->
                        android.util.Log.d("SummaryViewModel", "✅ Summary generated successfully")
                        _summary.value = generatedSummary
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        android.util.Log.e("SummaryViewModel", "❌ Summary generation failed", error)
                        _errorMessage.value = error.message ?: "Failed to generate summary"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("SummaryViewModel", "💥 Unexpected error during summary generation", e)
                _errorMessage.value = "Unexpected error: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    private fun listenForSummaryUpdates(sessionId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("SummaryViewModel", "🔄 Starting real-time summary updates for session: $sessionId")
                
                summaryRepository.getSummaryForSession(sessionId).collect { updatedSummary ->
                    if (updatedSummary != null) {
                        android.util.Log.d("SummaryViewModel", "📝 Summary update received - Status: ${updatedSummary.status}")
                        _summary.value = updatedSummary
                        
                        // Stop loading when summary is complete or failed
                        when (updatedSummary.status.name) {
                            "COMPLETED" -> {
                                _isLoading.value = false
                                _errorMessage.value = null
                                android.util.Log.d("SummaryViewModel", "✅ Summary completed")
                            }
                            "FAILED" -> {
                                _isLoading.value = false
                                _errorMessage.value = updatedSummary.errorMessage ?: "Summary generation failed"
                                android.util.Log.d("SummaryViewModel", "❌ Summary failed: ${updatedSummary.errorMessage}")
                            }
                            "GENERATING" -> {
                                _isLoading.value = true
                                _errorMessage.value = null
                                android.util.Log.d("SummaryViewModel", "🔄 Summary generating...")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SummaryViewModel", "Error listening for summary updates", e)
                _errorMessage.value = "Failed to monitor summary progress"
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}