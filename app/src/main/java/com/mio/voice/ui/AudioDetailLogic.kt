package com.mio.voice.ui

import com.mio.voice.data.generation.GenerationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 详情页纯逻辑：缺失字段回退“未知”、文件大小格式化、时间格式化、生成类型/语速文案。
 * 无 Android 依赖，便于 JVM 单元测试。
 */
object AudioDetailLogic {

    const val UNKNOWN = "未知"

    /** 缺失或空白字段回退为“未知”，避免显示 null。 */
    fun orUnknown(value: String?): String =
        value?.takeIf { it.isNotBlank() } ?: UNKNOWN

    /** 人类可读文件大小：B / KB / MB。0 或负数视为未知。 */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return UNKNOWN
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> "%.1f MB".format(mb)
            kb >= 1.0 -> "%.1f KB".format(kb)
            else -> "$bytes B"
        }
    }

    /** 生成时间格式化：yyyy-MM-dd HH:mm。 */
    fun formatDateTime(epochMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))

    /** 生成语速文案，例如 1.0。 */
    fun formatSpeed(speed: Float): String =
        if (speed % 1f == 0f) "%.1f".format(speed) else speed.toString()

    fun generationTypeLabel(type: GenerationType): String = when (type) {
        GenerationType.PlainText -> "普通文本"
        GenerationType.Words -> "单词列表"
        GenerationType.AiDirector -> "AI 导演"
        GenerationType.Legacy -> "历史记录"
    }

    fun localFilesLabel(ok: Boolean): String = if (ok) "完整" else "缺失"
}
