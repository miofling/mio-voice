package com.mio.voice.data.generation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.coroutines.coroutineContext

data class SavedAudioFile(
    val file: File,
    val format: String
)

class GeneratedAudioFileStore(
    private val audioDirectory: File,
    private val writer: (File, ByteArray) -> Unit = ::writeAndSync,
    private val mover: (File, File) -> Unit = ::atomicMove
) {
    constructor(context: Context) : this(File(context.applicationContext.filesDir, DIRECTORY_NAME))

    suspend fun save(
        id: String,
        createdAt: Long,
        audioBytes: ByteArray,
        audioFormat: String
    ): SavedAudioFile = withContext(Dispatchers.IO) {
        require(audioBytes.isNotEmpty()) { "TTS Provider 返回了空音频。" }
        check(audioDirectory.exists() || audioDirectory.mkdirs()) { "无法创建本地音频目录。" }

        val normalizedFormat = normalizeFormat(audioFormat)
        val baseName = "${createdAt}_${id}"
        val finalFile = File(audioDirectory, "$baseName.$normalizedFormat")
        val temporaryFile = File(audioDirectory, "$baseName.$normalizedFormat.tmp")
        var promoted = false
        try {
            temporaryFile.delete()
            coroutineContext.ensureActive()
            writer(temporaryFile, audioBytes)
            coroutineContext.ensureActive()
            check(temporaryFile.isFile && temporaryFile.length() > 0L) {
                "本地临时音频文件为空。"
            }
            check(!finalFile.exists()) { "目标音频文件已存在。" }
            mover(temporaryFile, finalFile)
            promoted = true
            check(finalFile.isFile && finalFile.length() > 0L) {
                "本地音频文件保存失败。"
            }
            coroutineContext.ensureActive()
            SavedAudioFile(finalFile, normalizedFormat)
        } catch (error: Throwable) {
            temporaryFile.delete()
            if (promoted || finalFile.exists()) finalFile.delete()
            throw error
        }
    }

    fun exists(path: String): Boolean = runCatching {
        if (path.isBlank()) return@runCatching false
        val file = File(path)
        isOwned(file) && file.isFile && file.length() > 0L
    }.getOrDefault(false)

    fun delete(path: String): Boolean = runCatching {
        if (path.isBlank()) return@runCatching true
        val file = File(path)
        if (!isOwned(file) || !file.exists()) return@runCatching !file.exists()
        file.delete()
    }.getOrDefault(false)

    suspend fun cleanupExpiredTemporaryFiles(
        now: Long = System.currentTimeMillis(),
        olderThanMs: Long = DEFAULT_TMP_MAX_AGE_MS
    ): Int = withContext(Dispatchers.IO) {
        if (!audioDirectory.exists()) return@withContext 0
        audioDirectory.listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isFile && it.name.endsWith(".tmp") }
            .filter { now - it.lastModified() >= olderThanMs }
            .count { it.delete() }
    }

    private fun isOwned(file: File): Boolean = runCatching {
        file.canonicalFile.parentFile == audioDirectory.canonicalFile
    }.getOrDefault(false)

    private fun normalizeFormat(value: String): String = when (
        value.trim().lowercase().removePrefix("audio/").removePrefix(".")
    ) {
        "mpeg", "mp3" -> "mp3"
        "wave", "x-wav", "wav" -> "wav"
        "x-m4a", "m4a" -> "m4a"
        "aac" -> "aac"
        "flac", "x-flac" -> "flac"
        "ogg" -> "ogg"
        "opus" -> "opus"
        else -> throw IllegalArgumentException("不支持的音频格式：$value")
    }

    companion object {
        const val DIRECTORY_NAME = "generated_audio"
        const val DEFAULT_TMP_MAX_AGE_MS = 24L * 60L * 60L * 1000L

        private fun writeAndSync(file: File, bytes: ByteArray) {
            FileOutputStream(file).use { output ->
                output.write(bytes)
                output.flush()
                output.fd.sync()
            }
        }

        private fun atomicMove(source: File, target: File) {
            try {
                Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                check(source.renameTo(target)) { "文件系统不支持原子重命名。" }
            }
        }
    }
}
