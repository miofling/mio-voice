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
        // 默认情绪为 MiniMax 合法的中性值 "calm"（不再是非法的 "neutral"）。
        assertEquals("calm", voice.presets.first().emotion)
        assertEquals(1.0f, voice.presets.first().speed)
        assertEquals(0, voice.presets.first().pitch)
        assertEquals(VoiceLibrary.DEFAULT_PREVIEW_TEXT, voice.presets.first().previewText)
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
    fun oldPresetDataMigratesDescriptionToEmptyString() {
        val presetRow = listOf("p1", "默认", "neutral", "1.0", "0", "试听").joinToString(",") { encode(it) }
        val legacyV2 = listOf("2", encode("v1"), encode("Voice"), encode("voice-a"), encode("p1"), encode(presetRow))
            .joinToString("\t")

        val voices = VoiceLibrary.deserializeVoices(legacyV2)

        assertEquals("", voices.first().presets.first().description)
    }

    @Test
    fun legacyNeutralEmotionMigratesToCalm() {
        // 旧数据把中性存为非法的 "neutral"，反序列化时应迁移为 MiniMax 合法的 "calm"。
        val presetRow = listOf("p1", "默认", "neutral", "1.0", "0", "试听", "").joinToString(",") { encode(it) }
        val legacyV3 = listOf("3", encode("v1"), encode("Voice"), encode("voice-a"), encode("p1"), encode(presetRow))
            .joinToString("\t")

        val voices = VoiceLibrary.deserializeVoices(legacyV3)

        assertEquals("calm", voices.first().presets.first().emotion)
    }

    @Test
    fun newVoiceFieldsSurviveSerializationRoundTrip() {
        val voice = VoiceLibrary.createVoice(
            displayName = "测试音色",
            voiceId = "voice-x",
            id = "vx",
            description = "  简介内容  ",
            language = "中文（普通话）",
            style = "少女声",
            createdAt = 1_700_000_000_000L
        )

        val restored = VoiceLibrary.deserializeVoices(VoiceLibrary.serializeVoices(listOf(voice)))

        assertEquals(1, restored.size)
        val r = restored.first()
        assertEquals("简介内容", r.description) // 首尾空格被去除
        assertEquals("中文（普通话）", r.language)
        assertEquals("少女声", r.style)
        assertEquals(1_700_000_000_000L, r.createdAt)
    }

    @Test
    fun legacyV3DataDeserializesWithDefaultedNewFields() {
        // v3 仅有 6 段（无 description/language/style/createdAt）。
        val presetRow = listOf("p1", "默认", "neutral", "1.0", "0", "试听", "").joinToString(",") { encode(it) }
        val legacyV3 = listOf("3", encode("v1"), encode("Voice"), encode("voice-a"), encode("p1"), encode(presetRow))
            .joinToString("\t")

        val voices = VoiceLibrary.deserializeVoices(legacyV3)

        assertEquals(1, voices.size)
        val v = voices.first()
        assertEquals("voice-a", v.voiceId)
        assertEquals("", v.description)
        assertEquals("", v.language)
        assertEquals("", v.style)
        // 旧数据 createdAt 缺失 → 反序列化时按策略补齐为非 0。
        assertTrue(v.createdAt > 0L)
    }

    @Test
    fun duplicateVoiceIdIsDetectedAndSelfIsExcluded() {
        val a = VoiceLibrary.createVoice("A", "voice-a", id = "a")
        val b = VoiceLibrary.createVoice("B", "voice-b", id = "b")
        val voices = listOf(a, b)

        assertTrue(VoiceLibrary.isVoiceIdTaken(voices, "voice-a"))
        assertTrue(VoiceLibrary.isVoiceIdTaken(voices, "  voice-b  ")) // 去空格后比较
        assertFalse(VoiceLibrary.isVoiceIdTaken(voices, "voice-c"))
        // 编辑自身：相同 voice_id 不应判为冲突。
        assertFalse(VoiceLibrary.isVoiceIdTaken(voices, "voice-a", excludeProfileId = "a"))
        // 但若改成别的已存在 id，仍冲突。
        assertTrue(VoiceLibrary.isVoiceIdTaken(voices, "voice-b", excludeProfileId = "a"))
    }

    @Test
    fun deletePresetOnlyRemovesTargetAndKeepsOthers() {
        var voice = VoiceLibrary.createVoice("A", "voice-a", id = "a")
        voice = VoiceLibrary.upsertPreset(voice, EmotionPreset("p2", "开心", "happy", 1.0f, 0), makeDefault = false)
        voice = VoiceLibrary.upsertPreset(voice, EmotionPreset("p3", "悲伤", "sad", 0.9f, -1), makeDefault = false)
        assertEquals(3, voice.presets.size)

        val result = VoiceLibrary.deletePreset(voice, "p2")

        assertTrue(result.deleted)
        assertEquals(2, result.voice.presets.size)
        assertFalse(result.voice.presets.any { it.id == "p2" })
        assertTrue(result.voice.presets.any { it.id == "p3" })
    }

    @Test
    fun editPresetDoesNotCreateDuplicate() {
        var voice = VoiceLibrary.createVoice("A", "voice-a", id = "a")
        voice = VoiceLibrary.upsertPreset(voice, EmotionPreset("p2", "开心", "happy", 1.0f, 0), makeDefault = false)
        val before = voice.presets.size

        val edited = VoiceLibrary.upsertPreset(
            voice,
            EmotionPreset("p2", "更开心", "happy", 1.1f, 1),
            makeDefault = false
        )

        assertEquals(before, edited.presets.size) // 数量不变
        val p2 = edited.presets.first { it.id == "p2" }
        assertEquals("更开心", p2.label)
        assertEquals(1.1f, p2.speed)
        assertEquals(1, p2.pitch)
    }

    @Test
    fun avatarPathSurvivesV5SerializationRoundTrip() {
        val voice = VoiceLibrary.createVoice(
            displayName = "带头像",
            voiceId = "voice-av",
            id = "av",
            avatarPath = "/data/user/0/com.mio.voice/files/voice_avatars/voice_av_123.webp"
        )

        val restored = VoiceLibrary.deserializeVoices(VoiceLibrary.serializeVoices(listOf(voice)))

        assertEquals(1, restored.size)
        assertEquals(
            "/data/user/0/com.mio.voice/files/voice_avatars/voice_av_123.webp",
            restored.first().avatarPath
        )
    }

    @Test
    fun createVoiceWithoutAvatarHasNullPath() {
        val voice = VoiceLibrary.createVoice("无头像", "voice-na")
        assertEquals(null, voice.avatarPath)

        val restored = VoiceLibrary.deserializeVoices(VoiceLibrary.serializeVoices(listOf(voice)))
        assertEquals(null, restored.first().avatarPath)
    }

    @Test
    fun blankAvatarPathNormalizesToNull() {
        val voice = VoiceLibrary.createVoice("空白", "voice-blank", avatarPath = "   ")
        assertEquals(null, voice.avatarPath)
    }

    @Test
    fun editingVoiceUpdatesAvatarPath() {
        val original = VoiceLibrary.createVoice("编辑", "voice-edit", id = "e", avatarPath = "/old/path.webp")
        val edited = VoiceLibrary.normalizeVoice(original.copy(avatarPath = "/new/path.webp"))
        assertEquals("/new/path.webp", edited.avatarPath)

        val removed = VoiceLibrary.normalizeVoice(original.copy(avatarPath = null))
        assertEquals(null, removed.avatarPath)
    }

    @Test
    fun legacyV4DataDeserializesWithNullAvatarPath() {
        // v4 仅 10 段（无 avatarPath，index 10 缺失）。
        val presetRow = listOf("p1", "默认", "neutral", "1.0", "0", "试听", "").joinToString(",") { encode(it) }
        val legacyV4 = listOf(
            "4", encode("v1"), encode("Voice"), encode("voice-a"), encode("p1"),
            encode(presetRow), encode("简介"), encode("中文"), encode("少女声"), encode("1700000000000")
        ).joinToString("\t")

        val voices = VoiceLibrary.deserializeVoices(legacyV4)

        assertEquals(1, voices.size)
        assertEquals("voice-a", voices.first().voiceId)
        assertEquals(null, voices.first().avatarPath) // 旧数据无该字段 → null
        assertEquals("简介", voices.first().description)
    }

    @Test
    fun invalidAvatarPathDoesNotBreakDeserialization() {
        // 头像路径指向不存在的文件，序列化/反序列化层仅作字符串处理，不应失败。
        val voice = VoiceLibrary.createVoice("坏路径", "voice-bad", id = "b", avatarPath = "/nonexistent/missing.webp")
        val restored = VoiceLibrary.deserializeVoices(VoiceLibrary.serializeVoices(listOf(voice)))
        assertEquals(1, restored.size)
        assertEquals("/nonexistent/missing.webp", restored.first().avatarPath)
    }

    @Test
    fun version5IsRecognizedByDeserializer() {
        val presetRow = listOf("p1", "默认", "neutral", "1.0", "0", "试听", "").joinToString(",") { encode(it) }
        val v5 = listOf(
            "5", encode("v1"), encode("Voice"), encode("voice-a"), encode("p1"),
            encode(presetRow), encode(""), encode(""), encode(""), encode("0"), encode("/p.webp")
        ).joinToString("\t")

        val voices = VoiceLibrary.deserializeVoices(v5)

        assertEquals(1, voices.size)
        assertEquals("/p.webp", voices.first().avatarPath)
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

    @Test
    fun providerExtrasSurviveSerializationRoundTrip() {
        val extras = mapOf(
            "minimax.voice_modify.pitch" to "30",
            "minimax.voice_modify.sound_effects" to "lofi_telephone",
            // 含分隔符字符的值也要保真（验证二级 Base64 编码）。
            "minimax.note" to "a=b;c,d|e"
        )
        val voice = VoiceLibrary.upsertPreset(
            VoiceLibrary.createVoice("A", "voice-a", id = "a"),
            EmotionPreset("soft", "电话", "calm", 1.0f, 0, providerExtras = extras),
            makeDefault = true
        )

        val restored = VoiceLibrary.deserializeVoices(VoiceLibrary.serializeVoices(listOf(voice)))

        val preset = restored.first().presets.first { it.id == "soft" }
        assertEquals(extras, preset.providerExtras)
    }

    @Test
    fun legacyV5PresetDeserializesWithEmptyProviderExtras() {
        // v5 预设子格式只有 7 个逗号位（无私有袋第 8 位）→ 反序列化应得空袋。
        val presetRow = listOf("p1", "默认", "calm", "1.0", "0", "试听", "").joinToString(",") { encode(it) }
        val v5 = listOf(
            "5", encode("v1"), encode("Voice"), encode("voice-a"), encode("p1"),
            encode(presetRow), encode(""), encode(""), encode(""), encode("0"), encode("")
        ).joinToString("\t")

        val voices = VoiceLibrary.deserializeVoices(v5)

        assertEquals(1, voices.size)
        assertTrue(voices.first().presets.first().providerExtras.isEmpty())
    }

    @Test
    fun resolvePresetPassesThroughProviderExtras() {
        val extras = mapOf("minimax.voice_modify.timbre" to "-40")
        val voice = VoiceLibrary.upsertPreset(
            VoiceLibrary.createVoice("A", "voice-a", id = "a"),
            EmotionPreset("soft", "浑厚", "calm", 1.0f, 0, providerExtras = extras),
            makeDefault = true
        )

        val resolved = VoiceLibrary.resolvePreset(listOf(voice), "a", "soft")

        assertEquals(extras, resolved?.providerExtras)
    }

    @Test
    fun providerExtrasChangeCacheKey() {
        val base = request("calm", 1.0f, 0)
        val withExtra = base.copy(extraParams = mapOf("minimax.voice_modify.pitch" to "30"))

        assertNotEquals(
            GenerationFingerprint.fromRequest(base).sha256(),
            GenerationFingerprint.fromRequest(withExtra).sha256()
        )
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

    private fun encode(value: String): String =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
}
