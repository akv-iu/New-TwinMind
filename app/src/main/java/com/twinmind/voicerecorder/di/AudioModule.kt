package com.twinmind.voicerecorder.di

import com.twinmind.voicerecorder.data.repository.AudioRecorderImpl
import com.twinmind.voicerecorder.data.repository.AudioStorageImpl
import com.twinmind.voicerecorder.data.repository.SessionRepositoryImpl
import com.twinmind.voicerecorder.domain.repository.AudioRecorder
import com.twinmind.voicerecorder.domain.repository.AudioStorage
import com.twinmind.voicerecorder.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {
    
    @Binds
    @Singleton
    abstract fun bindAudioRecorder(audioRecorderImpl: AudioRecorderImpl): AudioRecorder
    
    @Binds
    @Singleton
    abstract fun bindAudioStorage(audioStorageImpl: AudioStorageImpl): AudioStorage
    
    @Binds
    @Singleton
    abstract fun bindSessionRepository(sessionRepositoryImpl: SessionRepositoryImpl): SessionRepository
}