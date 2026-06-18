package com.mio.voice.core

import com.mio.voice.data.ProviderConfig
import com.mio.voice.data.TtsRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GenerationFingerprintTest {
    @Test
    fun fingerprintIsStableAndExcludesApiKey() {
        val first = request(apiKey = "secret-a")
        val second = request(apiKey = "secret-b")

        val a = GenerationFingerprint.fromRequest(first)
        val b = GenerationFingerprint.fromRequest(second)

        assertEquals(a.sha256(), b.sha256())
        assertFalse(a.stableSerialize().contains("secret"))
    }

    @Test
    fun fingerprintChangesWhenGenerationParameterChanges() {
        val a = GenerationFingerprint.fromRequest(request(text = "hello"))
        val b = GenerationFingerprint.fromRequest(request(text = "hello!"))
        assertNotEquals(a.sha256(), b.sha256())
    }

    private fun request(apiKey: String = "secret", text: String = "hello") = TtsRequest(
        providerProfileId = "fake",
        config = ProviderConfig(
            baseUrl = "https://example.test",
            endpointPath = "/tts",
            apiKey = apiKey,
            model = "speech-01",
            defaultVoiceId = "voice"
        ),
        text = text,
        voiceId = "voice",
        model = "speech-01",
        speed = 1.0f,
        emotion = null
    )
}
