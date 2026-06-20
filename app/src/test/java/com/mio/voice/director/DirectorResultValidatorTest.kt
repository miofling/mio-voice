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

    @Test
    fun allowTagsAcceptsLegalTagsAndReconstructsOriginal() {
        // 标记紧贴正文插入（无额外空格），剥离后必须与原文逐字一致。
        val result = DirectorResultValidator.parseAndValidate(
            json = """{"segments":[{"text":"他笑了(laughs)，然后说。<#0.5#>","presetId":"happy"}]}""",
            originalText = "他笑了，然后说。",
            voiceProfile = voice(),
            allowTags = true
        )

        assertTrue(result is DirectorValidationResult.Valid)
        result as DirectorValidationResult.Valid
        // 带标记的文本原样保留，发给 MiniMax。
        assertEquals("他笑了(laughs)，然后说。<#0.5#>", result.segments.first().text)
    }

    @Test
    fun allowTagsRejectsUnknownTag() {
        val result = DirectorResultValidator.parseAndValidate(
            json = """{"segments":[{"text":"他笑了 (giggle) 然后说。","presetId":"happy"}]}""",
            originalText = "他笑了 然后说。",
            voiceProfile = voice(),
            allowTags = true
        )

        assertTrue(result is DirectorValidationResult.Invalid)
    }

    @Test
    fun allowTagsStillRejectsModifiedBodyText() {
        // 即便开启标记，改了正文（！换。）仍判废——防幻觉护栏仍在。
        val result = DirectorResultValidator.parseAndValidate(
            json = """{"segments":[{"text":"开心！(laughs)","presetId":"happy"}]}""",
            originalText = "开心。",
            voiceProfile = voice(),
            allowTags = true
        )

        assertTrue(result is DirectorValidationResult.Invalid)
    }

    @Test
    fun defaultDisallowsTagsSoTaggedTextFailsReconstruction() {
        // 默认 allowTags=false：插入的标记会让拼接文本 != 原文 → 判废（旧行为保持）。
        val result = DirectorResultValidator.parseAndValidate(
            json = """{"segments":[{"text":"开心。(laughs)","presetId":"happy"}]}""",
            originalText = "开心。",
            voiceProfile = voice()
        )

        assertTrue(result is DirectorValidationResult.Invalid)
    }

    @Test
    fun toleratesWhitespaceDifferenceAndRebindsToOriginal() {
        // 小模型在段内多/少了空格，但非空白字符顺序一致 → 容忍空白差异，回镀回原文切片。
        val result = DirectorResultValidator.parseAndValidate(
            json = """{"segments":[{"text":"他说： 你好。","presetId":"happy"},{"text":"世界。","presetId":"sad"}]}""",
            originalText = "他说：你好。世界。",
            voiceProfile = voice()
        )

        assertTrue(result is DirectorValidationResult.Valid)
        result as DirectorValidationResult.Valid
        assertEquals(2, result.segments.size)
        // 回镀后用的是原文切片（不含 AI 多加的空格），拼接等于原文。
        assertEquals("他说：你好。", result.segments[0].text)
        assertEquals("世界。", result.segments[1].text)
        assertEquals("他说：你好。世界。", result.segments.joinToString("") { it.text })
    }

    @Test
    fun rebindKeepsOriginalPunctuationWhenModelDropsSpaces() {
        // AI 段去掉了原文里的空格，回镀必须用原文的空格/标点版本而非 AI 版本。
        val result = DirectorResultValidator.parseAndValidate(
            json = """{"segments":[{"text":"Hello,world.","presetId":"happy"}]}""",
            originalText = "Hello, world.",
            voiceProfile = voice()
        )

        assertTrue(result is DirectorValidationResult.Valid)
        result as DirectorValidationResult.Valid
        assertEquals("Hello, world.", result.segments.first().text)
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

