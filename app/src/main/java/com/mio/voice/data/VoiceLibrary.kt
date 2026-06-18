package com.mio.voice.data

import java.util.UUID

object VoiceLibrary {
    val allowedEmotions = setOf("neutral", "happy", "sad", "angry", "fearful", "disgusted", "surprised")

    fun defaultPreset(
        emotion: String? = null,
        speed: Float = 1.0f,
        pitch: Int = 0
    ): EmotionPreset = EmotionPreset(
        id = UUID.randomUUID().toString(),
        label = "默认",
        emotion = normalizeEmotion(emotion),
        speed = normalizeSpeed(speed),
        pitch = normalizePitch(pitch)
    )

    fun createVoice(
        displayName: String,
        voiceId: String,
        id: String = UUID.randomUUID().toString(),
        defaultEmotion: String? = null,
        defaultSpeed: Float = 1.0f,
        defaultPitch: Int = 0
    ): VoiceProfile {
        val preset = defaultPreset(defaultEmotion, defaultSpeed, defaultPitch)
        return VoiceProfile(
            id = id.ifBlank { UUID.randomUUID().toString() },
            displayName = displayName.ifBlank { voiceId },
            voiceId = voiceId,
            defaultPresetId = preset.id,
            presets = listOf(preset)
        )
    }

    fun normalizeVoice(
        profile: VoiceProfile,
        fallbackEmotion: String? = null,
        fallbackSpeed: Float = 1.0f,
        fallbackPitch: Int = 0
    ): VoiceProfile {
        val normalizedPresets = profile.presets
            .map { normalizePreset(it) }
            .ifEmpty { listOf(defaultPreset(fallbackEmotion, fallbackSpeed, fallbackPitch)) }
        val defaultId = normalizedPresets.firstOrNull { it.id == profile.defaultPresetId }?.id
            ?: normalizedPresets.first().id
        return profile.copy(
            id = profile.id.ifBlank { UUID.randomUUID().toString() },
            displayName = profile.displayName.ifBlank { profile.voiceId },
            defaultPresetId = defaultId,
            presets = normalizedPresets
        )
    }

    fun normalizePreset(preset: EmotionPreset): EmotionPreset =
        preset.copy(
            id = preset.id.ifBlank { UUID.randomUUID().toString() },
            label = preset.label.ifBlank { "默认" },
            emotion = normalizeEmotion(preset.emotion),
            speed = normalizeSpeed(preset.speed),
            pitch = normalizePitch(preset.pitch)
        )

    fun upsertPreset(profile: VoiceProfile, preset: EmotionPreset, makeDefault: Boolean): VoiceProfile {
        val normalized = normalizePreset(preset)
        val existing = profile.presets.map { normalizePreset(it) }
        val index = existing.indexOfFirst { it.id == normalized.id }
        val updated = if (index >= 0) {
            existing.toMutableList().also { it[index] = normalized }
        } else {
            existing + normalized
        }
        return normalizeVoice(
            profile.copy(
                defaultPresetId = if (makeDefault) normalized.id else profile.defaultPresetId,
                presets = updated
            )
        )
    }

    fun deletePreset(profile: VoiceProfile, presetId: String): PresetDeleteResult {
        val normalized = normalizeVoice(profile)
        if (normalized.defaultPresetId == presetId) {
            return PresetDeleteResult(normalized, false, "默认预设不能直接删除，请先设置另一个默认预设。")
        }
        val updated = normalized.presets.filterNot { it.id == presetId }
        return PresetDeleteResult(normalizeVoice(normalized.copy(presets = updated)), true, null)
    }

    fun setDefaultPreset(profile: VoiceProfile, presetId: String): VoiceProfile =
        normalizeVoice(
            profile.copy(defaultPresetId = profile.presets.firstOrNull { it.id == presetId }?.id ?: profile.defaultPresetId)
        )

    fun getAvailablePresets(voices: List<VoiceProfile>, voiceProfileId: String): List<EmotionPreset> =
        voices.firstOrNull { it.id == voiceProfileId }?.let { normalizeVoice(it).presets }.orEmpty()

    fun resolvePreset(
        voices: List<VoiceProfile>,
        voiceProfileId: String,
        presetId: String?
    ): ResolvedVoiceSettings? {
        val voice = voices.firstOrNull { it.id == voiceProfileId }?.let { normalizeVoice(it) } ?: return null
        val preset = voice.presets.firstOrNull { it.id == presetId }
            ?: voice.presets.firstOrNull { it.id == voice.defaultPresetId }
            ?: voice.presets.first()
        return ResolvedVoiceSettings(
            voiceProfileId = voice.id,
            presetId = preset.id,
            voiceId = voice.voiceId,
            emotion = preset.emotion,
            speed = preset.speed,
            pitch = preset.pitch
        )
    }

    fun serializeVoices(voices: List<VoiceProfile>): String =
        voices.joinToString("\n") { profile ->
            val normalized = normalizeVoice(profile)
            listOf(
                "2",
                encodePart(normalized.id),
                encodePart(normalized.displayName),
                encodePart(normalized.voiceId),
                encodePart(normalized.defaultPresetId),
                encodePart(serializePresets(normalized.presets))
            ).joinToString("\t")
        }

    fun deserializeVoices(
        value: String,
        fallbackEmotion: String? = null,
        fallbackSpeed: Float = 1.0f,
        fallbackPitch: Int = 0
    ): List<VoiceProfile> =
        value.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                runCatching {
                    val parts = line.split('\t')
                    when {
                        parts.size == 3 -> createVoice(
                            id = decodePart(parts[0]),
                            displayName = decodePart(parts[1]),
                            voiceId = decodePart(parts[2]),
                            defaultEmotion = fallbackEmotion,
                            defaultSpeed = fallbackSpeed,
                            defaultPitch = fallbackPitch
                        )
                        parts.size >= 6 && parts[0] == "2" -> normalizeVoice(
                            VoiceProfile(
                                id = decodePart(parts[1]),
                                displayName = decodePart(parts[2]),
                                voiceId = decodePart(parts[3]),
                                defaultPresetId = decodePart(parts[4]),
                                presets = deserializePresets(decodePart(parts[5]))
                            ),
                            fallbackEmotion,
                            fallbackSpeed,
                            fallbackPitch
                        )
                        else -> null
                    }
                }.getOrNull()
            }

    private fun serializePresets(presets: List<EmotionPreset>): String =
        presets.joinToString("|") { preset ->
            val normalized = normalizePreset(preset)
            listOf(
                normalized.id,
                normalized.label,
                normalized.emotion,
                normalized.speed.toString(),
                normalized.pitch.toString(),
                normalized.previewText
            ).joinToString(",") { encodePart(it) }
        }

    private fun deserializePresets(value: String): List<EmotionPreset> =
        value.split('|')
            .filter { it.isNotBlank() }
            .mapNotNull { row ->
                runCatching {
                    val parts = row.split(',')
                    if (parts.size < 6) return@runCatching null
                    EmotionPreset(
                        id = decodePart(parts[0]),
                        label = decodePart(parts[1]),
                        emotion = decodePart(parts[2]),
                        speed = decodePart(parts[3]).toFloatOrNull() ?: 1.0f,
                        pitch = decodePart(parts[4]).toIntOrNull() ?: 0,
                        previewText = decodePart(parts[5])
                    )
                }.getOrNull()
            }
            .filterNotNull()

    private fun normalizeEmotion(value: String?): String =
        value?.takeIf { it in allowedEmotions } ?: "neutral"

    private fun normalizeSpeed(value: Float): Float =
        value.coerceIn(0.5f, 2.0f)

    private fun normalizePitch(value: Int): Int =
        value.coerceIn(-12, 12)

    private fun encodePart(value: String): String =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decodePart(value: String): String =
        String(java.util.Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
}

data class PresetDeleteResult(
    val voice: VoiceProfile,
    val deleted: Boolean,
    val message: String?
)
