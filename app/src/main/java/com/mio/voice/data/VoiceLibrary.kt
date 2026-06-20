package com.mio.voice.data

import java.util.UUID

object VoiceLibrary {
    // MiniMax T2A v2 合法 emotion 值（speech-2.8 系列支持的子集）。
    // 中性对应官方的 "calm"；旧数据里历史遗留的 "neutral" 在 normalizeEmotion 中迁移为 "calm"。
    // 注意：whisper/fluent 仅 2.6 系列支持，2.8 不支持，故不纳入。
    val allowedEmotions = setOf("calm", "happy", "sad", "angry", "fearful", "disgusted", "surprised")
    const val DEFAULT_PREVIEW_TEXT = "你好，这是 Mio Voice 的音色试听。"

    fun defaultPreset(
        emotion: String? = null,
        speed: Float = 1.0f,
        pitch: Int = 0
    ): EmotionPreset = EmotionPreset(
        id = UUID.randomUUID().toString(),
        label = "默认",
        emotion = normalizeEmotion(emotion),
        speed = normalizeSpeed(speed),
        pitch = normalizePitch(pitch),
        previewText = DEFAULT_PREVIEW_TEXT,
        description = ""
    )

    fun createVoice(
        displayName: String,
        voiceId: String,
        id: String = UUID.randomUUID().toString(),
        defaultEmotion: String? = null,
        defaultSpeed: Float = 1.0f,
        defaultPitch: Int = 0,
        description: String = "",
        language: String = "",
        style: String = "",
        createdAt: Long = System.currentTimeMillis(),
        avatarPath: String? = null
    ): VoiceProfile {
        val preset = defaultPreset(defaultEmotion, defaultSpeed, defaultPitch)
        return VoiceProfile(
            id = id.ifBlank { UUID.randomUUID().toString() },
            displayName = displayName.ifBlank { voiceId },
            voiceId = voiceId,
            defaultPresetId = preset.id,
            presets = listOf(preset),
            description = description.trim(),
            language = language.trim(),
            style = style.trim(),
            createdAt = if (createdAt > 0L) createdAt else System.currentTimeMillis(),
            avatarPath = avatarPath?.takeIf { it.isNotBlank() }
        )
    }

    /**
     * 是否存在与给定 voice_id 重复的父音色。
     * @param excludeProfileId 编辑场景下排除音色自身（避免把自己判为冲突）。
     */
    fun isVoiceIdTaken(
        voices: List<VoiceProfile>,
        voiceId: String,
        excludeProfileId: String? = null
    ): Boolean {
        val target = voiceId.trim()
        if (target.isEmpty()) return false
        return voices.any { it.voiceId.trim() == target && it.id != excludeProfileId }
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
            presets = normalizedPresets,
            description = profile.description.trim(),
            language = profile.language.trim(),
            style = profile.style.trim(),
            // 旧数据无创建时间（0）时按“首次读取即补当前时间”的策略补齐，避免显示无意义的 0。
            createdAt = if (profile.createdAt > 0L) profile.createdAt else System.currentTimeMillis(),
            // 空串归一为 null；路径有效性由渲染层容错，这里不做文件存在性校验。
            avatarPath = profile.avatarPath?.takeIf { it.isNotBlank() }
        )
    }

    fun normalizePreset(preset: EmotionPreset): EmotionPreset =
        preset.copy(
            id = preset.id.ifBlank { UUID.randomUUID().toString() },
            label = preset.label.ifBlank { "默认" },
            emotion = normalizeEmotion(preset.emotion),
            speed = normalizeSpeed(preset.speed),
            pitch = normalizePitch(preset.pitch),
            previewText = preset.previewText.ifBlank { DEFAULT_PREVIEW_TEXT },
            description = preset.description,
            // 私有袋原样透传；未来若需按 provider 维度做键白名单/范围校验，可在此处加。
            providerExtras = preset.providerExtras
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
            pitch = preset.pitch,
            providerExtras = preset.providerExtras
        )
    }

    fun serializeVoices(voices: List<VoiceProfile>): String =
        voices.joinToString("\n") { profile ->
            val normalized = normalizeVoice(profile)
            // v6：顶层字段与 v5 同构（私有袋藏在预设子格式里，不占顶层位）；版本号自增以示语义变化。
            listOf(
                "6",
                encodePart(normalized.id),
                encodePart(normalized.displayName),
                encodePart(normalized.voiceId),
                encodePart(normalized.defaultPresetId),
                encodePart(serializePresets(normalized.presets)),
                encodePart(normalized.description),
                encodePart(normalized.language),
                encodePart(normalized.style),
                encodePart(normalized.createdAt.toString()),
                encodePart(normalized.avatarPath.orEmpty())
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
                        parts.size >= 6 && parts[0] in setOf("2", "3", "4", "5", "6") -> normalizeVoice(
                            VoiceProfile(
                                id = decodePart(parts[1]),
                                displayName = decodePart(parts[2]),
                                voiceId = decodePart(parts[3]),
                                defaultPresetId = decodePart(parts[4]),
                                presets = deserializePresets(decodePart(parts[5])),
                                // v4 追加字段；v2/v3 缺失时用默认值（向后兼容，不致解析失败）。
                                description = parts.getOrNull(6)?.let(::decodePart).orEmpty(),
                                language = parts.getOrNull(7)?.let(::decodePart).orEmpty(),
                                style = parts.getOrNull(8)?.let(::decodePart).orEmpty(),
                                createdAt = parts.getOrNull(9)?.let(::decodePart)?.toLongOrNull() ?: 0L,
                                // v5 追加字段；旧版本缺失或空串 → null（向后兼容）。
                                avatarPath = parts.getOrNull(10)?.let(::decodePart)?.takeIf { it.isNotBlank() }
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
                normalized.previewText,
                normalized.description,
                // 第 8 位（index 7）：provider 私有袋，整体再 Base64 一次避免内部分隔符与外层逗号冲突。
                encodeProviderExtras(normalized.providerExtras)
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
                        previewText = decodePart(parts[5]),
                        description = parts.getOrNull(6)?.let(::decodePart).orEmpty(),
                        // 第 8 位（index 7）；v6 前的旧预设无此位 → 空袋（向后兼容）。
                        providerExtras = parts.getOrNull(7)?.let(::decodePart)?.let(::decodeProviderExtras).orEmpty()
                    )
                }.getOrNull()
            }
            .filterNotNull()

    private fun normalizeEmotion(value: String?): String =
        when {
            // 旧数据兼容：早期把中性存为 "neutral"（MiniMax 非法值），统一迁移为官方的 "calm"。
            value == "neutral" -> "calm"
            value != null && value in allowedEmotions -> value
            else -> "calm"
        }

    private fun normalizeSpeed(value: Float): Float =
        value.coerceIn(0.5f, 2.0f)

    private fun normalizePitch(value: Int): Int =
        value.coerceIn(-12, 12)

    private fun encodePart(value: String): String =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decodePart(value: String): String =
        String(java.util.Base64.getUrlDecoder().decode(value), Charsets.UTF_8)

    // 私有袋编码：每个 kv 的 key/value 各自 Base64，用 '=' 连接，条目间用 ';' 分隔。
    // key/value 已 Base64 故不含 '='/';'，分隔安全。空袋编码为空串。
    private fun encodeProviderExtras(extras: Map<String, String>): String =
        extras.entries
            .sortedBy { it.key }
            .joinToString(";") { (k, v) -> "${encodePart(k)}=${encodePart(v)}" }

    private fun decodeProviderExtras(value: String): Map<String, String> {
        if (value.isBlank()) return emptyMap()
        return value.split(';')
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val idx = entry.indexOf('=')
                if (idx <= 0) return@mapNotNull null
                runCatching {
                    decodePart(entry.substring(0, idx)) to decodePart(entry.substring(idx + 1))
                }.getOrNull()
            }
            .toMap()
    }
}

data class PresetDeleteResult(
    val voice: VoiceProfile,
    val deleted: Boolean,
    val message: String?
)
