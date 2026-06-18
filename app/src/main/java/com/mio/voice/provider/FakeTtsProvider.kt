package com.mio.voice.provider

import com.mio.voice.cache.WavTone
import com.mio.voice.data.ProviderConfig
import com.mio.voice.data.TtsRequest
import com.mio.voice.data.TtsResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlin.math.absoluteValue

class FakeTtsProvider(
    private val delayMs: Long = 350L
) : TtsProvider {
    override suspend fun generate(request: TtsRequest): TtsResult {
        delay(delayMs)
        coroutineContext.ensureActive()
        if (request.text.contains("[fail]", ignoreCase = true)) {
            error("Fake Provider 模拟此片段生成失败。")
        }
        val frequency = 360.0 + (request.text.hashCode().absoluteValue % 320)
        val duration = (420 + request.text.length * 12).coerceIn(420, 1800)
        return TtsResult(
            audioBytes = WavTone.tone(durationMs = duration, frequencyHz = frequency),
            audioFormat = "wav",
            contentType = "audio/wav"
        )
    }

    override suspend fun testConnection(config: ProviderConfig): TtsResult {
        delay(200)
        coroutineContext.ensureActive()
        return TtsResult(
            audioBytes = WavTone.tone(durationMs = 500, frequencyHz = 523.25),
            audioFormat = "wav",
            contentType = "audio/wav"
        )
    }
}
