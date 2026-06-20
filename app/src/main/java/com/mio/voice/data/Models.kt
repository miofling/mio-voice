package com.mio.voice.data

import java.io.File

data class VoiceProfile(
    val id: String,
    val displayName: String,
    val voiceId: String,
    val defaultPresetId: String,
    val presets: List<EmotionPreset>,
    /** 音色简介，可空（默认空串）。 */
    val description: String = "",
    /** 语言，例如“中文（普通话）”，可空。 */
    val language: String = "",
    /** 音色风格，例如“少女声”，可空。 */
    val style: String = "",
    /** 创建时间（毫秒时间戳）；0 表示未知（多见于旧数据，反序列化时按策略补齐）。 */
    val createdAt: Long = 0L,
    /** 自定义头像在 App 私有目录下的绝对路径，可空（默认 null 表示无自定义头像）。 */
    val avatarPath: String? = null
)

data class EmotionPreset(
    val id: String,
    val label: String,
    val emotion: String,
    val speed: Float,
    val pitch: Int,
    val previewText: String = "",
    val description: String = "",
    /**
     * Provider 私有参数袋（"笼子"）：只放某个 provider 独有、通用层没有对应概念的参数。
     * key 统一加 provider 前缀防撞键，例如 "minimax.voice_modify.pitch" -> "30"。
     * 其他 provider 读不懂自己前缀以外的键时直接忽略，不污染通用层（emotion/speed/pitch）。
     */
    val providerExtras: Map<String, String> = emptyMap()
)

data class ResolvedVoiceSettings(
    val voiceProfileId: String,
    val presetId: String,
    val voiceId: String,
    val emotion: String,
    val speed: Float,
    val pitch: Int,
    /** 从预设透传的 provider 私有参数袋，最终进入 TtsRequest.extraParams。 */
    val providerExtras: Map<String, String> = emptyMap()
)

data class ProviderConfig(
    val baseUrl: String = "",
    val endpointPath: String = "",
    val apiKey: String? = null,
    val model: String = "",
    val defaultVoiceId: String = "",
    val defaultSpeed: Float = 1.0f,
    val defaultEmotion: String? = null,
    val audioFormat: String = "mp3",
    val maxCharsPerRequest: Int = 2_000
)

data class DirectorConfig(
    val baseUrl: String = "",
    val endpointPath: String = "/v1/chat/completions",
    val apiKey: String? = null,
    val model: String = ""
)

data class TtsRequest(
    val providerProfileId: String,
    val config: ProviderConfig,
    val text: String,
    val voiceId: String,
    val model: String,
    val speed: Float,
    val emotion: String?,
    val pitch: Int = 0,
    val audioFormat: String = "mp3",
    val extraParams: Map<String, String> = emptyMap()
)

data class TtsResult(
    val audioBytes: ByteArray,
    val audioFormat: String,
    val contentType: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TtsResult) return false
        return audioBytes.contentEquals(other.audioBytes) &&
            audioFormat == other.audioFormat &&
            contentType == other.contentType
    }

    override fun hashCode(): Int {
        var result = audioBytes.contentHashCode()
        result = 31 * result + audioFormat.hashCode()
        result = 31 * result + (contentType?.hashCode() ?: 0)
        return result
    }
}

data class QueueSegment(
    val id: String,
    val text: String,
    val audioFile: File?,
    val status: SegmentStatus,
    val errorMessage: String? = null,
    val request: TtsRequest? = null
)

enum class SegmentStatus {
    Pending,
    Generating,
    Ready,
    Failed,
    Skipped
}
