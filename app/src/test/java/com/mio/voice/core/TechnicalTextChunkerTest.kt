package com.mio.voice.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnicalTextChunkerTest {
    @Test
    fun shortTextIsKeptAsOneOriginalRequest() {
        val text = "Gemini 3.5 Pro is at https://example.com/a.b?x=1.2. Do not split."
        assertEquals(listOf(text), TechnicalTextChunker(maxChars = 500).split(text))
    }

    @Test
    fun punctuationDoesNotCreateChunks() {
        val text = "First sentence. Second sentence! Third sentence? Version 1.2.3."
        assertEquals(listOf(text), TechnicalTextChunker(maxChars = 500).split(text))
    }

    @Test
    fun longTextUsesParagraphsAndMinimizesChunkCount() {
        val text = "a".repeat(30) + "\n\n" + "b".repeat(30) + "\n\n" + "c".repeat(30)
        val chunks = TechnicalTextChunker(maxChars = 70).split(text)
        assertEquals(2, chunks.size)
        assertEquals(text, chunks.joinToString(""))
        assertTrue(chunks.all { it.length <= 70 })
    }

    @Test
    fun oversizedParagraphFallsBackToSafeLength() {
        val text = "x".repeat(95)
        val chunks = TechnicalTextChunker(maxChars = 30).split(text)
        assertEquals(listOf(30, 30, 30, 5), chunks.map { it.length })
        assertEquals(text, chunks.joinToString(""))
        assertFalse(chunks.any { it.isBlank() })
    }
}
