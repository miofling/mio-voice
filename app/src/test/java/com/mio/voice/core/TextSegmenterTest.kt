package com.mio.voice.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TextSegmenterTest {
    @Test
    fun splitsChineseEnglishAndJapaneseSentenceEnds() {
        val segments = TextSegmenter().split("你好。Hello! 大丈夫？")
        assertEquals(listOf("你好。", "Hello!", "大丈夫？"), segments)
    }

    @Test
    fun keepsClosingQuotesWithPreviousSegment() {
        val segments = TextSegmenter().split("她说：“可以。”然后离开。")
        assertEquals(listOf("她说：“可以。”", "然后离开。"), segments)
    }

    @Test
    fun handlesEllipsis() {
        val segments = TextSegmenter().split("等等……继续...结束。")
        assertEquals(listOf("等等……", "继续...", "结束。"), segments)
    }

    @Test
    fun handlesBlankLinesWithoutEmptySegments() {
        val segments = TextSegmenter().split("第一段\n\n\n第二段")
        assertEquals(listOf("第一段", "第二段"), segments)
    }

    @Test
    fun splitsLongTextByPreferredDelimiters() {
        val text = "一".repeat(30) + "，" + "二".repeat(30) + "，" + "三".repeat(30)
        val segments = TextSegmenter(maxChars = 40).split(text)
        assertEquals(3, segments.size)
        assertFalse(segments.any { it.isBlank() })
        assertEquals(text, segments.joinToString(" ").replace(" ", ""))
    }

    @Test
    fun safelySplitsLongTextWithoutPunctuation() {
        val text = "a".repeat(95)
        val segments = TextSegmenter(maxChars = 30).split(text)
        assertEquals(listOf(30, 30, 30, 5), segments.map { it.length })
    }
}
