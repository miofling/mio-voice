package com.mio.voice.cache

import com.mio.voice.core.GenerationFingerprint
import com.mio.voice.data.ProviderConfig
import com.mio.voice.data.TtsRequest
import com.mio.voice.data.TtsResult
import com.mio.voice.provider.TtsProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AudioCacheTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun request(text: String = "你好") = TtsRequest(
        providerProfileId = "minimax",
        config = ProviderConfig(
            baseUrl = "https://example.test",
            endpointPath = "/tts",
            apiKey = "secret",
            model = "speech-01",
            defaultVoiceId = "voice"
        ),
        text = text,
        voiceId = "voice",
        model = "speech-01",
        speed = 1.0f,
        emotion = "neutral",
        pitch = 0,
        audioFormat = "wav"
    )

    /** 计数型 fake provider：返回固定音频并统计调用次数（用于命中验证）。 */
    private class CountingProvider(
        private val bytes: ByteArray = ByteArray(64) { 1 }
    ) : TtsProvider {
        var calls = 0
            private set

        override suspend fun generate(request: TtsRequest): TtsResult {
            calls++
            return TtsResult(audioBytes = bytes, audioFormat = "wav", contentType = "audio/wav")
        }

        override suspend fun testConnection(config: ProviderConfig): TtsResult =
            TtsResult(audioBytes = bytes, audioFormat = "wav")
    }

    private class EmptyAudioProvider : TtsProvider {
        override suspend fun generate(request: TtsRequest): TtsResult =
            TtsResult(audioBytes = ByteArray(0), audioFormat = "wav")

        override suspend fun testConnection(config: ProviderConfig): TtsResult =
            TtsResult(audioBytes = ByteArray(0), audioFormat = "wav")
    }

    @Test
    fun generatesThenReusesCachedFile() = runTest {
        val cache = AudioCache.forDir(tempFolder.newFolder("c1"))
        val provider = CountingProvider()

        val first = cache.getOrGenerate(request(), provider)
        assertTrue(first.exists())
        assertTrue(first.length() > 0)
        assertEquals(1, provider.calls)

        // 同参数二次请求：命中缓存，不再调用 provider。
        val second = cache.getOrGenerate(request(), provider)
        assertEquals(first.absolutePath, second.absolutePath)
        assertEquals(1, provider.calls)
    }

    @Test
    fun emptyAudioThrowsAndLeavesNoFile() {
        val dir = tempFolder.newFolder("c2")
        val cache = AudioCache.forDir(dir)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { cache.getOrGenerate(request(), EmptyAudioProvider()) }
        }
        // 空音频不得留下任何文件（含 .part）。
        assertTrue(dir.listFiles()?.none { it.isFile } ?: true)
    }

    @Test
    fun trimRemovesAgedFilesOnly() = runTest {
        val dir = tempFolder.newFolder("c3")
        val cache = AudioCache.forDir(dir)
        val fresh = File(dir, "fresh.wav").apply { writeBytes(ByteArray(8)) }
        val old = File(dir, "old.wav").apply {
            writeBytes(ByteArray(8))
            setLastModified(System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000)
        }

        cache.trimPreviewCache(maxFiles = 100, maxAgeMs = 7L * 24 * 60 * 60 * 1000)

        assertTrue(fresh.exists())
        assertFalse(old.exists())
    }

    @Test
    fun trimEnforcesMaxFilesByOldest() = runTest {
        val dir = tempFolder.newFolder("c4")
        val cache = AudioCache.forDir(dir)
        val files = (0 until 5).map { i ->
            File(dir, "f$i.wav").apply {
                writeBytes(ByteArray(8))
                setLastModified(1_000_000L + i * 1000L)
            }
        }

        cache.trimPreviewCache(maxFiles = 2, maxAgeMs = Long.MAX_VALUE)

        assertFalse(files[0].exists())
        assertFalse(files[1].exists())
        assertFalse(files[2].exists())
        assertTrue(files[3].exists())
        assertTrue(files[4].exists())
    }

    @Test
    fun cacheKeyChangesWhenPreviewParamChanges() {
        val base = request(text = "甲")
        val baseHash = GenerationFingerprint.fromRequest(base).sha256()

        assertEquals(baseHash, GenerationFingerprint.fromRequest(request(text = "甲")).sha256())
        assertFalse(baseHash == GenerationFingerprint.fromRequest(request(text = "乙")).sha256())
        assertFalse(baseHash == GenerationFingerprint.fromRequest(base.copy(emotion = "happy")).sha256())
        assertFalse(baseHash == GenerationFingerprint.fromRequest(base.copy(speed = 1.5f)).sha256())
        assertFalse(baseHash == GenerationFingerprint.fromRequest(base.copy(pitch = 3)).sha256())
        assertFalse(baseHash == GenerationFingerprint.fromRequest(base.copy(voiceId = "other")).sha256())
    }
}
