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

class AudioCache private constructor(private val audioDir: File) {
    init {
        audioDir.mkdirs()
    }

    constructor(context: Context) : this(File(context.cacheDir, "tts_audio"))

    companion object {
        /** 测试 / 内部使用：直接指定音频缓存目录（避免依赖 Android Context）。 */
        internal fun forDir(dir: File): AudioCache = AudioCache(dir)
    }

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
                require(result.audioBytes.isNotEmpty()) { "TTS Provider 返回了空音频。" }
                partFile.writeBytes(result.audioBytes)
                require(partFile.length() > 0) { "生成的音频文件为空。" }
                if (finalFile.exists()) finalFile.delete()
                check(partFile.renameTo(finalFile)) { "无法写入最终音频缓存文件。" }
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

    /**
     * 修剪试听 / 生成音频缓存目录，避免无限增长：先删超龄文件，若仍超量再按最旧顺序删除，
     * 直到文件数 <= [maxFiles]。仅作用于本目录，不影响其它缓存。
     * @param maxFiles 保留的最大文件数。
     * @param maxAgeMs 超过该时长（毫秒）的文件视为过期并删除。
     */
    suspend fun trimPreviewCache(
        maxFiles: Int = 40,
        maxAgeMs: Long = 7L * 24 * 60 * 60 * 1000
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val files = audioDir.listFiles()?.filter { it.isFile }?.toMutableList() ?: return@withContext
        // 1) 删除超龄文件。
        val iterator = files.iterator()
        while (iterator.hasNext()) {
            val file = iterator.next()
            if (now - file.lastModified() > maxAgeMs) {
                if (file.delete()) iterator.remove()
            }
        }
        // 2) 仍超量时按最旧顺序删除。
        if (files.size > maxFiles) {
            files.sortBy { it.lastModified() }
            val removeCount = files.size - maxFiles
            for (i in 0 until removeCount) {
                files[i].delete()
            }
        }
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
