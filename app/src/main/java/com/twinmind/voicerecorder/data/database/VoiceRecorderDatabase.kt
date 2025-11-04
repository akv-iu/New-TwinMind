package com.twinmind.voicerecorder.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.twinmind.voicerecorder.data.database.dao.*
import com.twinmind.voicerecorder.data.database.entity.*

@Database(
    entities = [
        RecordingSessionEntity::class,
        AudioChunkEntity::class,
        TranscriptEntity::class,
        SummaryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VoiceRecorderDatabase : RoomDatabase() {
    
    abstract fun recordingSessionDao(): RecordingSessionDao
    abstract fun audioChunkDao(): AudioChunkDao
    abstract fun transcriptDao(): TranscriptDao
    abstract fun summaryDao(): SummaryDao
    
    companion object {
        @Volatile
        private var INSTANCE: VoiceRecorderDatabase? = null
        
        fun getDatabase(context: Context): VoiceRecorderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoiceRecorderDatabase::class.java,
                    "voice_recorder_database"
                )
                .fallbackToDestructiveMigration() // Use destructive migration during development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}