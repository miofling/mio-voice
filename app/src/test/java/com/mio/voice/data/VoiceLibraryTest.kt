package com.mio.voice.data

import com.mio.voice.core.GenerationFingerprint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceLibraryTest {
    @Test
    fun newVoiceAlwaysHasDefaultPreset() {
        val voice = VoiceLibrary.createVoice("A", "voice-a")

        assertEquals(1, voice.presets.size)
        assertEquals(voice.defaultPresetId, voice.presets.first().id)
        assertEquals("neutral", voice.presets.first().emotion)
        assertEquals(1.0f, voice.presets.first().speed)
        assertEquals(0, voice.presets.first().pitch)
    }

    @Test
    fun defaultPresetCannotBeDeletedDirectly() {
        val voice = VoiceLibrary.createVoice("A", "voice-a")
        val result = VoiceLibrary.deletePreset(voice, voice.defaultPresetId)

        assertFalse(result.deleted)
        assertEquals(1, result.voice.presets.size)
    }

    @Test
    fun changingDefaultPresetWorks() {
        val voice = VoiceLibrary.createVoice("A", "voice-a")
        val second = EmotionPreset("p2", "开心", "happy", 1.1f, 2)
        val updated = VoiceLibrary.upsertPreset(voice, second, makeDefault = true)

        assertEquals("p2", updated.defaultPresetId)
        assertEquals("happy", VoiceLibrary.resolvePreset(listOf(updated), updated.id, null)?.emotion)
    }

    @Test
    fun samePresetLabelsOnDifferentVoicesAreIndependent() {
        val a = VoiceLibrary.upsertPreset(
            VoiceLibrary.createVoice("A", "voice-a", id = "a"),
            EmotionPreset("a-happy", "开心", "happy", 1.0f, 0),
            makeDefault = false
        )
        val b = VoiceLibrary.upsertPreset(
            VoiceLibrary.createVoice("B", "voice-b", id = "b"),
            EmotionPreset("b-happy", "开心", "sad", 0.9f, -1),
            makeDefault = false
        )

        assertEquals("happy", VoiceLibrary.resolvePreset(listOf(a, b), "a", "a-happy")?.emotion)
        assertEquals("sad", VoiceLibrary.resolvePreset(listOf(a, b), "b", "b-happy")?.emotion)
    }

    @Test
    fun missingPresetIdFallsBackToDefaultPreset() {
        val voice = VoiceLibrary.createVoice("A", "voice-a")

        val resolved = VoiceLibrary.resolvePreset(listOf(voice), voice.id, "missing")

        assertEquals(voice.defaultPresetId, resolved?.presetId)
    }

    @Test
    fun oldVoiceDataMigratesToDefaultPreset() {
        val legacy = listOf("old-id", "Old name", "old-voice").joinToString("\t") {
            java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray())
        }

        val voices = VoiceLibrary.deserializeVoices(legacy, fallbackEmotion = "happy", fallbackSpeed = 1.2f)

        assertEquals(1, voices.size)
        assertEquals("Old name", voices.first().displayName)
        assertEquals("old-voice", voices.first().voiceId)
        assertEquals("happy", voices.first().presets.first().emotion)
        assertEquals(1.2f, voices.first().presets.first().speed)
    }

    @Test
    fun resolvedPresetConvertsToTtsParameters() {
        val voice = VoiceLibrary.upsertPreset(
            VoiceLibrary.createVoice("A", "voice-a", id = "a"),
            EmotionPreset("soft", "低沉", "sad", 0.8f, -3),
            makeDefault = true
        )

        val resolved = VoiceLibrary.resolvePreset(listOf(voice), "a", "soft")

        assertEquals("voice-a", resolved?.voiceId)
        assertEquals("sad", resolved?.emotion)
        assertEquals(0.8f, resolved?.speed)
        assertEquals(-3, resolved?.pitch)
    }

    @Test
    fun cacheKeyChangesWhenSynthesisParametersChange() {
        val a = request("happy", 1.0f, 0)
        val b = request("sad", 1.0f, 0)
        val c = request("happy", 1.1f, 0)
        val d = request("happy", 1.0f, 2)

        assertNotEquals(GenerationFingerprint.fromRequest(a).sha256(), GenerationFingerprint.fromRequest(b).sha256())
        assertNotEquals(GenerationFingerprint.fromRequest(a).sha256(), GenerationFingerprint.fromRequest(c).sha256())
        assertNotEquals(GenerationFingerprint.fromRequest(a).sha256(), GenerationFingerprint.fromRequest(d).sha256())
    }

    @Test
    fun labelOnlyChangesDoNotChangeCacheKey() {
        val voice = VoiceLibrary.createVoice("A", "voice-a")
        val first = EmotionPreset("p", "开心", "happy", 1.0f, 0)
        val second = first.copy(label = "高兴")

        assertTrue(first.label != second.label)
        assertEquals(
            GenerationFingerprint.fromRequest(request(first.emotion, first.speed, first.pitch)).sha256(),
            GenerationFingerprint.fromRequest(request(second.emotion, second.speed, second.pitch)).sha256()
        )
        assertTrue(voice.presets.isNotEmpty())
    }

    private fun request(emotion: String, speed: Float, pitch: Int) = TtsRequest(
        providerProfileId = "minimax",
        config = ProviderConfig(baseUrl = "https://example.test", endpointPath = "/v1/t2a_v2"),
        text = "hello",
        voiceId = "voice-a",
        model = "speech-2.8-hd",
        speed = speed,
        emotion = emotion,
        pitch = pitch,
        audioFormat = "wav"
    )
}
