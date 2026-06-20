package com.mio.voice.director

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformanceTagsTest {
    @Test
    fun stripRemovesInterjectionsAndKeepsPlainText() {
        val input = "你好 (laughs) 世界 (sighs)。"
        assertEquals("你好  世界 。", PerformanceTags.stripTags(input))
    }

    @Test
    fun stripRemovesValidPause() {
        assertEquals("第一句。第二句。", PerformanceTags.stripTags("第一句。<#0.8#>第二句。"))
    }

    @Test
    fun stripKeepsPlainTextUnchanged() {
        val plain = "这是一段没有任何标记的普通文本。"
        assertEquals(plain, PerformanceTags.stripTags(plain))
    }

    @Test
    fun stripDoesNotRemoveUnknownTags() {
        // 清单外的 (foo) 与超范围停顿不被剥离，从而在比对原文时暴露出来。
        assertEquals("a(foo)b", PerformanceTags.stripTags("a(foo)b"))
        assertEquals("x<#999#>y", PerformanceTags.stripTags("x<#999#>y"))
    }

    @Test
    fun extractUnknownCatchesIllegalParensTag() {
        val unknown = PerformanceTags.extractUnknownTags("好 (laughs) 坏 (giggle)")
        assertEquals(listOf("(giggle)"), unknown)
    }

    @Test
    fun extractUnknownCatchesOutOfRangeAndMalformedPause() {
        assertTrue(PerformanceTags.hasUnknownTags("a<#0#>b"))       // 0 < 0.01 下限
        assertTrue(PerformanceTags.hasUnknownTags("a<#100#>b"))     // > 99.99 上限
        assertTrue(PerformanceTags.hasUnknownTags("a<#1.234#>b"))   // 超过两位小数
    }

    @Test
    fun validPauseBoundariesAccepted() {
        assertFalse(PerformanceTags.hasUnknownTags("a<#0.01#>b"))
        assertFalse(PerformanceTags.hasUnknownTags("a<#99.99#>b"))
        assertFalse(PerformanceTags.hasUnknownTags("a<#0.5#>b"))
    }

    @Test
    fun allOfficialInterjectionsAreStrippable() {
        PerformanceTags.INTERJECTIONS.forEach { tag ->
            assertEquals("AB", PerformanceTags.stripTags("A${tag}B"))
        }
        assertEquals(19, PerformanceTags.INTERJECTIONS.size)
    }

    @Test
    fun plainTextHasNoUnknownTags() {
        assertFalse(PerformanceTags.hasUnknownTags("普通文本，没有标记。"))
    }
}
