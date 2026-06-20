package com.mio.voice.core

import com.mio.voice.data.TtsRequest
import java.security.MessageDigest

data class GenerationFingerprint(
    val providerProfileId: String,
    val baseUrl: String,
    val endpoint: String,
    val text: String,
    val voiceId: String,
    val model: String,
    val speed: Float,
    val emotion: String?,
    val pitch: Int,
    val audioFormat: String,
    val extraParams: Map<String, String>,
    // v2：引入 provider 私有袋（extraParams 现可来自预设持久化），升版以失效语义已变的旧缓存。
    val cacheVersion: Int = 2
) {
    fun sha256(): String {
        val bytes = stableSerialize().toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }

    fun stableSerialize(): String = buildString {
        appendField("cacheVersion", cacheVersion.toString())
        appendField("providerProfileId", providerProfileId)
        appendField("baseUrl", baseUrl)
        appendField("endpoint", endpoint)
        appendField("text", text)
        appendField("voiceId", voiceId)
        appendField("model", model)
        appendField("speed", speed.toString())
        appendField("emotion", emotion.orEmpty())
        appendField("pitch", pitch.toString())
        appendField("audioFormat", audioFormat)
        extraParams.toSortedMap().forEach { (key, value) ->
            appendField("extra:$key", value)
        }
    }

    private fun StringBuilder.appendField(name: String, value: String) {
        append(name.length).append(':').append(name)
        append('=')
        append(value.length).append(':').append(value)
        append('\n')
    }

    companion object {
        fun fromRequest(request: TtsRequest): GenerationFingerprint =
            GenerationFingerprint(
                providerProfileId = request.providerProfileId,
                baseUrl = request.config.baseUrl.trimEnd('/'),
                endpoint = request.config.endpointPath,
                text = request.text,
                voiceId = request.voiceId,
                model = request.model,
                speed = request.speed,
                emotion = request.emotion,
                pitch = request.pitch,
                audioFormat = request.audioFormat,
                extraParams = request.extraParams
            )
    }
}
