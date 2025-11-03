package com.twinmind.voicerecorder.di

import android.content.Context
import androidx.room.Room
import com.twinmind.voicerecorder.data.database.VoiceRecorderDatabase
import com.twinmind.voicerecorder.data.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideVoiceRecorderDatabase(
        @ApplicationContext context: Context
    ): VoiceRecorderDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            VoiceRecorderDatabase::class.java,
            "voice_recorder_database"
        ).build()
    }
    
    @Provides
    fun provideRecordingSessionDao(database: VoiceRecorderDatabase): RecordingSessionDao {
        return database.recordingSessionDao()
    }
    
    @Provides
    fun provideAudioChunkDao(database: VoiceRecorderDatabase): AudioChunkDao {
        return database.audioChunkDao()
    }
    
    @Provides
    fun provideTranscriptDao(database: VoiceRecorderDatabase): TranscriptDao {
        return database.transcriptDao()
    }
    
    @Provides
    fun provideSummaryDao(database: VoiceRecorderDatabase): SummaryDao {
        return database.summaryDao()
    }
}