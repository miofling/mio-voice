package com.mio.voice.director

import com.mio.voice.core.GenerationFingerprint
import com.mio.voice.data.EmotionPreset
import com.mio.voice.data.ProviderConfig
import com.mio.voice.data.TtsRequest
import com.mio.voice.data.VoiceLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectorResultValidatorTest {
    @Test
    fun parsesAndValidatesNormalJson() {
        val voice = voice()
        val result = DirectorResultValidator.parseAndValidate(
            json = """{"segments":[{"text":"开心。","presetId":"happy"},{"text":"难过。","presetId":"sad"}]}""",
            originalText = "开心。难过。",
            voiceProfile = voice
        )

        assertTrue(result is DirectorValidationResult.Valid)
        result as DirectorValidationResult.Valid
        assertEquals(listOf("happy", "sad"), result.segments.map { it.presetId })
    }

    @Test
    fun rejectsInvalidJson() {
        val result = DirectorResultValidator.parseAndValidate("not json", "原文", voice())

        assertTrue(result is DirectorValidationResult.Invalid)
    }

    @Test
    fun rejectsEmptySegments() {
        val result = DirectorResultValidator.parseAndValidate("""{"segments":[]}""", "原文", voice())

        assertTrue(result is DirectorValidationResult.Invalid)
    }

    @Test
    fun invalidPresetFallsBackToDefaultPresetWithWarning() {
        val voice = voice()
        val result = DirectorResultValidator.parseAndValidate(
            json = """{"segments":[{"text":"开心。","presetId":"missing"}]}""",
            originalText = "开心。",
            voiceProfile = voice
        )

        assertTrue(result is DirectorValidationResult.Valid)
        result as DirectorValidationResult.Valid
        assertEquals(voice.defaultPresetId, result.segments.first().presetId)
        assertTrue(result.segments.first().warnings.isNotEmpty())
    }

    @Test
    fun adjacentSamePresetIsMerged() {
        val result = DirectorResultValidator.parseAndValidate(
            json = """{"segments":[{"text":"开心。","presetId":"happy"},{"text":"继续开心。","presetId":"happy"}]}""",
            originalText = "开心。继续开心。",
            voiceProfile = voice()
        )

        assertTrue(result is DirectorValidationResult.Valid)
        result as DirectorValidationResult.Valid
        assertEquals(1, result.segments.size)
        assertEquals("开心。继续开心。", result.segments.first().text)
    }

    @Test
    fun rejectsModifiedText() {
        val result = DirectorResultValidator.parseAndValidate(
            json = """{"segments":[{"text":"开心！","presetId":"happy"}]}""",
            originalText = "开心。",
            voiceProfile = voice()
        )

        assertTrue(result is DirectorValidationResult.Invalid)
    }

    @Test
    fun rejectsReorderedText() {
        val result = DirectorResultValidator.parseAndValidate(
            json = """{"segments":[{"text":"B","presetId":"happy"},{"text":"A","presetId":"sad"}]}""",
            originalText = "AB",
            voiceProfile = voice()
        )

        assertTrue(result is DirectorValidationResult.Invalid)
    }

    @Test
    fun presetIdResolvesToMiniMaxParameters() {
        val voice = voice()
        val resolved = VoiceLibrary.resolvePreset(listOf(voice), "voice", "sad")

        assertEquals("voice-id", resolved?.voiceId)
        assertEquals("sad", resolved?.emotion)
        assertEquals(0.85f, resolved?.speed)
        assertEquals(-2, resolved?.pitch)
    }

    @Test
    fun differentPresetsProduceDifferentCacheKeys() {
        val happy = request("happy", 1.1f, 1)
        val sad = request("sad", 0.85f, -2)

        assertNotEquals(
            GenerationFingerprint.fromRequest(happy).sha256(),
            GenerationFingerprint.fromRequest(sad).sha256()
        )
    }

    private fun voice() =
        VoiceLibrary.upsertPreset(
            VoiceLibrary.upsertPreset(
                VoiceLibrary.createVoice("Voice", "voice-id", id = "voice"),
                EmotionPreset("happy", "开心", "happy", 1.1f, 1, description = "愉快明亮。"),
                makeDefault = true
            ),
            EmotionPreset("sad", "难过", "sad", 0.85f, -2, description = "低落委屈。"),
            makeDefault = false
        )

    private fun request(emotion: String, speed: Float, pitch: Int) = TtsRequest(
        providerProfileId = "minimax",
        config = ProviderConfig(baseUrl = "https://example.test", endpointPath = "/v1/t2a_v2"),
        text = "hello",
        voiceId = "voice-id",
        model = "speech-2.8-hd",
        speed = speed,
        emotion = emotion,
        pitch = pitch,
        audioFormat = "wav"
    )
}

