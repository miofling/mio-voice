package com.mio.voice.provider

import com.mio.voice.data.ProviderConfig
import com.mio.voice.data.TtsRequest
import com.mio.voice.data.TtsResult
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MiniMaxTtsProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : TtsProvider {
    override suspend fun generate(request: TtsRequest): TtsResult {
        val apiKey = request.config.apiKey?.takeIf { it.isNotBlank() }
            ?: throw MiniMaxTtsException("API key is required.")
        val voiceId = request.voiceId.takeIf { it.isNotBlank() }
            ?: throw MiniMaxTtsException("voice_id is required.")
        val model = request.model.takeIf { it.isNotBlank() }
            ?: throw MiniMaxTtsException("Model is required.")

        val body = JSONObject()
            .put("model", model)
            .put("text", request.text)
            .put("stream", false)
            .put("language_boost", request.extraParams["language_boost"] ?: "auto")
            .put("output_format", request.extraParams["output_format"] ?: "hex")
            .put(
                "voice_setting",
                JSONObject()
                    .put("voice_id", voiceId)
                    .put("speed", request.speed.toDouble())
                    .put("vol", request.extraParams["vol"]?.toDoubleOrNull() ?: 1.0)
                    .put("pitch", request.extraParams["pitch"]?.toDoubleOrNull() ?: 0.0)
                    .also { voice ->
                        request.emotion?.takeIf { it.isNotBlank() }?.let { voice.put("emotion", it) }
                    }
            )
            .put(
                "audio_setting",
                JSONObject()
                    .put("sample_rate", request.extraParams["sample_rate"]?.toIntOrNull() ?: 32_000)
                    .put("bitrate", request.extraParams["bitrate"]?.toIntOrNull() ?: 128_000)
                    .put("format", request.audioFormat)
                    .put("channel", request.extraParams["channel"]?.toIntOrNull() ?: 1)
            )

        val httpRequest = Request.Builder()
            .url(endpointUrl(request.config))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON))
            .build()

        val responseText = client.newCall(httpRequest).awaitBody()
        val parsed = parseTtsResponse(responseText, request.audioFormat)
        return if (parsed.isRemoteUrl) {
            TtsResult(
                audioBytes = downloadAudio(parsed.payload),
                audioFormat = parsed.audioFormat,
                contentType = "audio/${parsed.audioFormat}"
            )
        } else {
            TtsResult(
                audioBytes = parsed.payload.decodeAudioPayload(),
                audioFormat = parsed.audioFormat,
                contentType = "audio/${parsed.audioFormat}"
            )
        }
    }

    override suspend fun testConnection(config: ProviderConfig): TtsResult {
        val request = TtsRequest(
            providerProfileId = "minimax",
            config = config,
            text = "Mio Voice connection test.",
            voiceId = config.defaultVoiceId,
            model = config.model,
            speed = config.defaultSpeed,
            emotion = config.defaultEmotion,
            audioFormat = config.audioFormat
        )
        return generate(request)
    }

    private fun endpointUrl(config: ProviderConfig): String {
        val endpoint = config.endpointPath.ifBlank { "/v1/t2a_v2" }
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) return endpoint
        val base = config.baseUrl.ifBlank { "https://api.minimax.io" }.trimEnd('/')
        val path = if (endpoint.startsWith('/')) endpoint else "/$endpoint"
        return base + path
    }

    private suspend fun downloadAudio(url: String): ByteArray {
        val request = Request.Builder().url(url).get().build()
        return client.newCall(request).awaitBytes()
    }

    private fun parseTtsResponse(json: String, requestedFormat: String): ParsedAudio {
        if (json.isBlank()) throw MiniMaxTtsException("MiniMax returned an empty response.")
        val root = JSONObject(json)
        val baseResp = root.optJSONObject("base_resp")
        val statusCode = baseResp?.optInt("status_code", 0) ?: 0
        if (statusCode != 0) {
            throw MiniMaxTtsException(mapBusinessError(statusCode, baseResp?.optString("status_msg").orEmpty()))
        }
        val data = root.optJSONObject("data")
            ?: throw MiniMaxTtsException("MiniMax response did not include audio data.")
        val payload = listOf("audio", "audio_url", "url")
            .firstNotNullOfOrNull { key -> data.optString(key).takeIf { it.isNotBlank() } }
            ?: throw MiniMaxTtsException("MiniMax response audio was empty.")
        val format = root.optJSONObject("extra_info")
            ?.optString("audio_format")
            ?.takeIf { it.isNotBlank() }
            ?: requestedFormat
        return ParsedAudio(
            payload = payload,
            audioFormat = format,
            isRemoteUrl = payload.startsWith("http://") || payload.startsWith("https://")
        )
    }

    private suspend fun Call.awaitBody(): String =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { cancel() }
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(MiniMaxTtsException(sanitizeNetworkError(e)))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val body = it.body?.string().orEmpty()
                        if (!it.isSuccessful) {
                            continuation.resumeWithException(MiniMaxTtsException(httpError(it.code, body)))
                        } else {
                            continuation.resume(body)
                        }
                    }
                }
            })
        }

    private suspend fun Call.awaitBytes(): ByteArray =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { cancel() }
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(MiniMaxTtsException(sanitizeNetworkError(e)))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val bytes = it.body?.bytes() ?: ByteArray(0)
                        if (!it.isSuccessful) {
                            continuation.resumeWithException(MiniMaxTtsException("Audio URL download failed with HTTP ${it.code}."))
                        } else if (bytes.isEmpty()) {
                            continuation.resumeWithException(MiniMaxTtsException("Audio URL returned an empty file."))
                        } else {
                            continuation.resume(bytes)
                        }
                    }
                }
            })
        }

    private fun String.decodeAudioPayload(): ByteArray {
        val compact = trim()
        if (compact.length % 2 == 0 && compact.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            return ByteArray(compact.length / 2) { index ->
                compact.substring(index * 2, index * 2 + 2).toInt(16).toByte()
            }
        }
        return Base64.getDecoder().decode(compact)
    }

    private fun httpError(code: Int, body: String): String {
        val message = runCatching {
            JSONObject(body).optJSONObject("base_resp")?.optString("status_msg")
        }.getOrNull()?.takeIf { it.isNotBlank() }
        val hint = when (code) {
            401, 403 -> " Check API key, model access, and voice_id permissions."
            408 -> " Request timed out."
            429 -> " Rate limited. Retry later."
            in 500..599 -> " MiniMax server error. Retry later."
            else -> ""
        }
        return "MiniMax HTTP $code.${message?.let { " $it." } ?: ""}$hint"
    }

    private fun mapBusinessError(code: Int, message: String): String {
        val detail = when (code) {
            1001 -> "Request timed out."
            1002, 2045, 2056 -> "Rate or usage limit reached."
            1004, 2049 -> "API key is invalid or unauthorized."
            1008 -> "Insufficient balance."
            1042 -> "Input contains too many invisible or illegal characters."
            2013 -> "Invalid request parameters."
            20132 -> "Invalid samples or voice_id."
            2042 -> "No access to this voice_id."
            else -> message.ifBlank { "MiniMax business error." }
        }
        return "MiniMax error $code: $detail"
    }

    private fun sanitizeNetworkError(error: IOException): String =
        error.message?.replace(Regex("Bearer\\s+[^\\s]+", RegexOption.IGNORE_CASE), "Bearer ***")
            ?: "Network request failed."

    private data class ParsedAudio(
        val payload: String,
        val audioFormat: String,
        val isRemoteUrl: Boolean
    )

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

class MiniMaxTtsException(message: String) : Exception(message)
