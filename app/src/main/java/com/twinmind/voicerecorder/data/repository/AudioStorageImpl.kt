package com.twinmind.voicerecorder.data.repository

import android.content.Context
import android.os.StatFs
import com.twinmind.voicerecorder.domain.model.AudioChunk
import com.twinmind.voicerecorder.domain.repository.AudioStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioStorage {
    
    companion object {
        private const val AUDIO_DIR = "audio_chunks"
        private const val MIN_STORAGE_BYTES = 100 * 1024 * 1024L // 100MB minimum
    }
    
    private val audioDir: File by lazy {
        File(context.filesDir, AUDIO_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    override suspend fun saveAudioChunk(audioData: ByteArray, chunk: AudioChunk): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                // Check storage before saving
                if (!hasMinimumStorage(audioData.size.toLong())) {
                    return@withContext Result.failure(
                        IllegalStateException("Insufficient storage space")
                    )
                }
                
                val fileName = "${chunk.sessionId}_${chunk.sequenceNumber}_${chunk.id}.wav"
                val file = File(audioDir, fileName)
                
                FileOutputStream(file).use { fos ->
                    // Write WAV header
                    writeWavHeader(fos, audioData.size)
                    // Write audio data
                    fos.write(audioData)
                }
                
                Result.success(file)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun getAudioChunk(chunkId: String): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val file = audioDir.listFiles()?.find { it.name.contains(chunkId) }
                if (file?.exists() == true) {
                    Result.success(file)
                } else {
                    Result.failure(IllegalArgumentException("Audio chunk not found: $chunkId"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun deleteAudioChunk(chunkId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val file = audioDir.listFiles()?.find { it.name.contains(chunkId) }
                if (file?.exists() == true) {
                    file.delete()
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalArgumentException("Audio chunk not found: $chunkId"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun getAvailableStorageBytes(): Long {
        return withContext(Dispatchers.IO) {
            try {
                val stat = StatFs(audioDir.path)
                stat.availableBlocksLong * stat.blockSizeLong
            } catch (e: Exception) {
                0L
            }
        }
    }
    
    override suspend fun hasMinimumStorage(requiredBytes: Long): Boolean {
        val availableBytes = getAvailableStorageBytes()
        return availableBytes >= (requiredBytes + MIN_STORAGE_BYTES)
    }
    
    override suspend fun cleanup(olderThanMs: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()
                audioDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < (currentTime - olderThanMs)) {
                        file.delete()
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun getSessionAudioFiles(sessionId: String): Result<List<File>> {
        return withContext(Dispatchers.IO) {
            try {
                val sessionFiles = audioDir.listFiles()
                    ?.filter { it.name.startsWith("${sessionId}_") }
                    ?.sortedBy { fileName ->
                        // Extract sequence number for proper ordering
                        val parts = fileName.name.split("_")
                        if (parts.size >= 2) parts[1].toIntOrNull() ?: 0 else 0
                    } ?: emptyList()
                
                if (sessionFiles.isNotEmpty()) {
                    Result.success(sessionFiles)
                } else {
                    Result.failure(IllegalArgumentException("No audio files found for session: $sessionId"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun writeWavHeader(fos: FileOutputStream, audioDataSize: Int) {
        val totalSize = audioDataSize + 36
        val sampleRate = 44100
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        
        fos.write("RIFF".toByteArray())
        fos.write(intToByteArray(totalSize))
        fos.write("WAVE".toByteArray())
        fos.write("fmt ".toByteArray())
        fos.write(intToByteArray(16)) // Subchunk1Size
        fos.write(shortToByteArray(1)) // AudioFormat (PCM)
        fos.write(shortToByteArray(channels.toShort()))
        fos.write(intToByteArray(sampleRate))
        fos.write(intToByteArray(byteRate))
        fos.write(shortToByteArray(blockAlign.toShort()))
        fos.write(shortToByteArray(bitsPerSample.toShort()))
        fos.write("data".toByteArray())
        fos.write(intToByteArray(audioDataSize))
    }
    
    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }
    
    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }
}