package com.twinmind.voicerecorder.di

import com.twinmind.voicerecorder.data.audio.AudioPlayerImpl
import com.twinmind.voicerecorder.data.repository.AudioChunkRepositoryImpl
import com.twinmind.voicerecorder.data.repository.AudioRecorderImpl
import com.twinmind.voicerecorder.data.repository.AudioStorageImpl
import com.twinmind.voicerecorder.data.repository.SessionRepositoryImpl
import com.twinmind.voicerecorder.data.repository.SummaryRepositoryImpl
import com.twinmind.voicerecorder.data.repository.TranscriptionRepositoryImpl
import com.twinmind.voicerecorder.domain.audio.AudioPlayer
import com.twinmind.voicerecorder.domain.repository.AudioChunkRepository
import com.twinmind.voicerecorder.domain.repository.AudioRecorder
import com.twinmind.voicerecorder.domain.repository.AudioStorage
import com.twinmind.voicerecorder.domain.repository.SessionRepository
import com.twinmind.voicerecorder.domain.repository.SummaryRepository
import com.twinmind.voicerecorder.domain.repository.TranscriptionRepository
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
    
    @Binds
    @Singleton
    abstract fun bindAudioPlayer(audioPlayerImpl: AudioPlayerImpl): AudioPlayer
    
    @Binds
    @Singleton
    abstract fun bindAudioChunkRepository(audioChunkRepositoryImpl: AudioChunkRepositoryImpl): AudioChunkRepository
    
    @Binds
    @Singleton
    abstract fun bindTranscriptionRepository(transcriptionRepositoryImpl: TranscriptionRepositoryImpl): TranscriptionRepository
    
    @Binds
    @Singleton
    abstract fun bindSummaryRepository(summaryRepositoryImpl: SummaryRepositoryImpl): SummaryRepository
}