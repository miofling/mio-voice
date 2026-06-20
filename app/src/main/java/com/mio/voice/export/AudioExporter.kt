package com.mio.voice.export

import java.io.File
import java.io.OutputStream

/**
 * 导出生成的音频文件到用户选择的本地位置。
 *
 * 音频存在 app 私有目录（filesDir/generated_audio），其他 app / 文件管理器无法直接访问，
 * 因此用 SAF（系统「保存到...」文件选择器，ACTION_CREATE_DOCUMENT）让用户自选保存位置，
 * 再把私有文件的字节拷进目标位置。不需要任何存储权限。
 *
 * 纯逻辑（文件名清洗 / 扩展名 / MIME 推断）拆成可单测的函数；拷贝部分只依赖标准 IO。
 */
object AudioExporter {

    /** 文件名非法字符（Windows/Android 通用），统一替换为下划线，并压缩空白。 */
    fun sanitizeFileName(raw: String): String {
        val cleaned = raw
            .replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.', '_', ' ')
        return cleaned.ifBlank { "audio" }
    }

    /**
     * 友好导出文件名：`{清洗后的标题(截断)}.{ext}`。
     * @param title 记录标题（自定义标题/原文预览等）
     * @param format 音频格式（mp3/wav...），用于扩展名
     * @param maxBaseLength 标题主体最大长度（避免文件名过长）
     */
    fun exportFileName(title: String, format: String, maxBaseLength: Int = 40): String {
        val ext = normalizeExt(format)
        val base = sanitizeFileName(title).take(maxBaseLength).trim().trim('.', '_', ' ').ifBlank { "audio" }
        return "$base.$ext"
    }

    /** 归一化扩展名：剥掉前缀点、转小写，空则回退 mp3。 */
    fun normalizeExt(format: String): String {
        val ext = format.trim().removePrefix(".").lowercase()
        return ext.ifBlank { "mp3" }
    }

    /** 根据格式推断音频 MIME 类型（供 SAF 创建文档时声明）。 */
    fun mimeForFormat(format: String): String = when (normalizeExt(format)) {
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "m4a", "aac" -> "audio/aac"
        "flac" -> "audio/flac"
        "ogg", "opus" -> "audio/ogg"
        else -> "audio/*"
    }

    /**
     * 把源音频文件的字节写入目标输出流（用户通过 SAF 选定的位置）。
     * @return 写入的字节数；源文件不存在/为空返回 -1。
     */
    fun copyToStream(source: File, out: OutputStream): Long {
        if (!source.isFile || source.length() <= 0L) return -1L
        return source.inputStream().use { input -> input.copyTo(out) }
    }
}
