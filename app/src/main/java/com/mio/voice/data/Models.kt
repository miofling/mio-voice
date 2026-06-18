package com.mio.voice.data

import java.io.File

data class VoiceProfile(
    val id: String,
    val displayName: String,
    val voiceId: String,
    val defaultPresetId: String,
    val presets: List<EmotionPreset>
)

data class EmotionPreset(
    val id: String,
    val label: String,
    val emotion: String,
    val speed: Float,
    val pitch: Int,
    val previewText: String = ""
)

data class ResolvedVoiceSettings(
    val voiceProfileId: String,
    val presetId: String,
    val voiceId: String,
    val emotion: String,
    val speed: Float,
    val pitch: Int
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
