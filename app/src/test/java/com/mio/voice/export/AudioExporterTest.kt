package com.mio.voice.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File

class AudioExporterTest {

    @Test
    fun sanitizeReplacesIllegalChars() {
        assertEquals("a_b_c", AudioExporter.sanitizeFileName("a/b\\c"))
        assertEquals("hello world", AudioExporter.sanitizeFileName("  hello   world  "))
        assertEquals("name", AudioExporter.sanitizeFileName("name?"))
    }

    @Test
    fun sanitizeBlankFallsBackToAudio() {
        assertEquals("audio", AudioExporter.sanitizeFileName("   "))
        assertEquals("audio", AudioExporter.sanitizeFileName("..."))
    }

    @Test
    fun exportFileNameBuildsBaseAndExt() {
        assertEquals("你好世界.mp3", AudioExporter.exportFileName("你好世界", "mp3"))
        assertEquals("title.wav", AudioExporter.exportFileName("title", "WAV"))
        // 非法字符清洗 + 扩展名归一化（带点前缀）
        assertEquals("a_b.mp3", AudioExporter.exportFileName("a:b", ".MP3"))
    }

    @Test
    fun exportFileNameTruncatesLongTitle() {
        val long = "x".repeat(100)
        val name = AudioExporter.exportFileName(long, "mp3", maxBaseLength = 10)
        assertEquals("xxxxxxxxxx.mp3", name)
    }

    @Test
    fun exportFileNameBlankTitleFallsBack() {
        assertEquals("audio.mp3", AudioExporter.exportFileName("   ", "mp3"))
    }

    @Test
    fun normalizeExtDefaultsToMp3() {
        assertEquals("mp3", AudioExporter.normalizeExt(""))
        assertEquals("wav", AudioExporter.normalizeExt(".WAV"))
        assertEquals("mp3", AudioExporter.normalizeExt("MP3"))
    }

    @Test
    fun mimeForFormatMapsKnownTypes() {
        assertEquals("audio/mpeg", AudioExporter.mimeForFormat("mp3"))
        assertEquals("audio/wav", AudioExporter.mimeForFormat("wav"))
        assertEquals("audio/aac", AudioExporter.mimeForFormat("m4a"))
        assertEquals("audio/flac", AudioExporter.mimeForFormat("flac"))
        assertEquals("audio/ogg", AudioExporter.mimeForFormat("opus"))
        assertEquals("audio/*", AudioExporter.mimeForFormat("xyz"))
    }

    @Test
    fun copyToStreamWritesBytes() {
        val tmp = File.createTempFile("export-test", ".mp3")
        try {
            val payload = "fake-audio-bytes".toByteArray()
            tmp.writeBytes(payload)
            val out = ByteArrayOutputStream()
            val written = AudioExporter.copyToStream(tmp, out)
            assertEquals(payload.size.toLong(), written)
            assertTrue(out.toByteArray().contentEquals(payload))
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun copyToStreamMissingFileReturnsMinusOne() {
        val missing = File("definitely_does_not_exist_12345.mp3")
        val out = ByteArrayOutputStream()
        assertEquals(-1L, AudioExporter.copyToStream(missing, out))
        assertEquals(0, out.size())
    }
}
