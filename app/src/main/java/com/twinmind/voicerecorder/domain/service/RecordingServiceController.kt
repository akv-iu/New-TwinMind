package com.twinmind.voicerecorder.domain.service

interface RecordingServiceController {
    
    fun startRecording()
    
    fun stopRecording() 
    
    fun pauseRecording()
    
    fun resumeRecording()
    
    fun isRecording(): Boolean
    
    fun getCurrentSessionId(): String?
}