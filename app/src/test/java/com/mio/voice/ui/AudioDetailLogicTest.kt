package com.mio.voice.ui

import com.mio.voice.data.generation.GenerationType
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioDetailLogicTest {

    @Test
    fun missingFieldsFallBackToUnknown() {
        // 验收点 13：缺失旧字段时显示“未知”而不崩溃。
        assertEquals("未知", AudioDetailLogic.orUnknown(null))
        assertEquals("未知", AudioDetailLogic.orUnknown("   "))
        assertEquals("speech-2.8-hd", AudioDetailLogic.orUnknown("speech-2.8-hd"))
    }

    @Test
    fun fileSizeFormatsHumanReadable() {
        assertEquals("未知", AudioDetailLogic.formatFileSize(0L))
        assertEquals("512 B", AudioDetailLogic.formatFileSize(512L))
        assertEquals("1.0 KB", AudioDetailLogic.formatFileSize(1024L))
        assertEquals("1.0 MB", AudioDetailLogic.formatFileSize(1024L * 1024L))
        assertEquals("2.5 MB", AudioDetailLogic.formatFileSize((2.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun speedFormatting() {
        assertEquals("1.0", AudioDetailLogic.formatSpeed(1.0f))
        assertEquals("1.25", AudioDetailLogic.formatSpeed(1.25f))
    }

    @Test
    fun generationTypeLabels() {
        assertEquals("普通文本", AudioDetailLogic.generationTypeLabel(GenerationType.PlainText))
        assertEquals("单词列表", AudioDetailLogic.generationTypeLabel(GenerationType.Words))
        assertEquals("AI 导演", AudioDetailLogic.generationTypeLabel(GenerationType.AiDirector))
        assertEquals("历史记录", AudioDetailLogic.generationTypeLabel(GenerationType.Legacy))
    }

    @Test
    fun localFilesLabel() {
        assertEquals("完整", AudioDetailLogic.localFilesLabel(true))
        assertEquals("缺失", AudioDetailLogic.localFilesLabel(false))
    }
}
