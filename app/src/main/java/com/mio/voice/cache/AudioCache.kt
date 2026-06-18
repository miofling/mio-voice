package com.mio.voice.cache

import android.content.Context
import com.mio.voice.core.GenerationFingerprint
import com.mio.voice.data.TtsRequest
import com.mio.voice.provider.TtsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

class AudioCache(context: Context) {
    private val audioDir = File(context.cacheDir, "tts_audio").apply { mkdirs() }

    suspend fun getOrGenerate(request: TtsRequest, provider: TtsProvider): File =
        withContext(Dispatchers.IO) {
            val fingerprint = GenerationFingerprint.fromRequest(request)
            val hash = fingerprint.sha256()
            val finalFile = File(audioDir, "$hash.${request.audioFormat}")
            if (finalFile.exists() && finalFile.length() > 0) return@withContext finalFile

            val partFile = File(audioDir, "$hash.${request.audioFormat}.part")
            try {
                if (partFile.exists()) partFile.delete()
                val result = provider.generate(request)
                require(result.audioBytes.isNotEmpty()) { "TTS provider returned empty audio." }
                partFile.writeBytes(result.audioBytes)
                require(partFile.length() > 0) { "Generated audio file is empty." }
                if (finalFile.exists()) finalFile.delete()
                check(partFile.renameTo(finalFile)) { "Could not commit generated audio." }
                finalFile
            } catch (error: Throwable) {
                partFile.delete()
                throw error
            }
        }

    suspend fun silence(durationMs: Int): File = withContext(Dispatchers.IO) {
        val safeDuration = durationMs.coerceIn(0, 10_000)
        val file = File(audioDir, "silence_${safeDuration}ms.wav")
        if (!file.exists() || file.length() == 0L) {
            file.writeBytes(WavTone.silence(safeDuration))
        }
        file
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        audioDir.listFiles()?.forEach { it.delete() }
    }

    suspend fun writeTemporary(name: String, bytes: ByteArray, extension: String): File =
        withContext(Dispatchers.IO) {
            val safeName = name.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val file = File(audioDir, "$safeName.$extension")
            file.writeBytes(bytes)
            file
        }
}

object WavTone {
    fun tone(durationMs: Int = 500, frequencyHz: Double = 440.0, sampleRate: Int = 22_050): ByteArray {
        val sampleCount = sampleRate * durationMs / 1000
        val pcm = ByteArray(sampleCount * 2)
        val buffer = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until sampleCount) {
            val value = (sin(2.0 * PI * frequencyHz * i / sampleRate) * Short.MAX_VALUE * 0.22).toInt()
            buffer.putShort(value.toShort())
        }
        return wav(pcm, sampleRate)
    }

    fun silence(durationMs: Int, sampleRate: Int = 22_050): ByteArray {
        val sampleCount = sampleRate * durationMs / 1000
        return wav(ByteArray(sampleCount * 2), sampleRate)
    }

    private fun wav(pcm: ByteArray, sampleRate: Int): ByteArray {
        val byteRate = sampleRate * 2
        val totalSize = 36 + pcm.size
        val buffer = ByteBuffer.allocate(44 + pcm.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(totalSize)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)
        buffer.putShort(1.toShort())
        buffer.putShort(1.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(2.toShort())
        buffer.putShort(16.toShort())
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(pcm.size)
        buffer.put(pcm)
        return buffer.array()
    }
}
