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
    suspend fun fetchModels(config: ProviderConfig): List<String> {
        val apiKey = config.apiKey?.takeIf { it.isNotBlank() }
            ?: throw MiniMaxTtsException("请先填写 API Key。")
        val httpRequest = Request.Builder()
            .url(modelsUrl(config))
            .header("Authorization", "Bearer $apiKey")
            .get()
            .build()
        val responseText = client.newCall(httpRequest).awaitBody()
        return parseModelsResponse(responseText)
    }

    override suspend fun generate(request: TtsRequest): TtsResult {
        val apiKey = request.config.apiKey?.takeIf { it.isNotBlank() }
            ?: throw MiniMaxTtsException("请先填写 API Key。")
        val voiceId = request.voiceId.takeIf { it.isNotBlank() }
            ?: throw MiniMaxTtsException("请先填写 voice_id。")
        val model = request.model.takeIf { it.isNotBlank() }
            ?: throw MiniMaxTtsException("请先填写模型名。")

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
                    .put("pitch", request.pitch)
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
            text = "Mio Voice 连接测试。",
            voiceId = config.defaultVoiceId,
            model = config.model,
            speed = config.defaultSpeed,
            emotion = config.defaultEmotion,
            pitch = 0,
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

    private fun modelsUrl(config: ProviderConfig): String {
        val base = config.baseUrl.ifBlank { "https://api.minimax.io" }.trimEnd('/')
        return if (base.endsWith("/v1")) "$base/models" else "$base/v1/models"
    }

    private suspend fun downloadAudio(url: String): ByteArray {
        val request = Request.Builder().url(url).get().build()
        return client.newCall(request).awaitBytes()
    }

    private fun parseTtsResponse(json: String, requestedFormat: String): ParsedAudio {
        if (json.isBlank()) throw MiniMaxTtsException("MiniMax 返回了空响应。")
        val root = JSONObject(json)
        val baseResp = root.optJSONObject("base_resp")
        val statusCode = baseResp?.optInt("status_code", 0) ?: 0
        if (statusCode != 0) {
            throw MiniMaxTtsException(mapBusinessError(statusCode, baseResp?.optString("status_msg").orEmpty()))
        }
        val data = root.optJSONObject("data")
            ?: throw MiniMaxTtsException("MiniMax 响应中没有音频数据。")
        val payload = listOf("audio", "audio_url", "url")
            .firstNotNullOfOrNull { key -> data.optString(key).takeIf { it.isNotBlank() } }
            ?: throw MiniMaxTtsException("MiniMax 返回的音频为空。")
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

    private fun parseModelsResponse(json: String): List<String> {
        if (json.isBlank()) throw MiniMaxTtsException("模型列表响应为空。")
        val root = JSONObject(json)
        val baseResp = root.optJSONObject("base_resp")
        val statusCode = baseResp?.optInt("status_code", 0) ?: 0
        if (statusCode != 0) {
            throw MiniMaxTtsException(mapBusinessError(statusCode, baseResp?.optString("status_msg").orEmpty()))
        }
        val result = mutableListOf<String>()
        root.optJSONArray("data")?.let { data ->
            for (i in 0 until data.length()) {
                when (val item = data.get(i)) {
                    is JSONObject -> item.optString("id").takeIf { it.isNotBlank() }?.let(result::add)
                    is String -> item.takeIf { it.isNotBlank() }?.let(result::add)
                }
            }
        }
        root.optJSONArray("models")?.let { models ->
            for (i in 0 until models.length()) {
                when (val item = models.get(i)) {
                    is JSONObject -> item.optString("id").takeIf { it.isNotBlank() }?.let(result::add)
                    is String -> item.takeIf { it.isNotBlank() }?.let(result::add)
                }
            }
        }
        if (result.isEmpty()) throw MiniMaxTtsException("没有解析到可用模型，请保留手动填写模型名。")
        return result.distinct().sorted()
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
                            continuation.resumeWithException(MiniMaxTtsException("音频 URL 下载失败，HTTP ${it.code}。"))
                        } else if (bytes.isEmpty()) {
                            continuation.resumeWithException(MiniMaxTtsException("音频 URL 返回了空文件。"))
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
            401, 403 -> " 请检查 API Key、模型权限和 voice_id 权限。"
            408 -> " 请求超时。"
            429 -> " 触发限流，请稍后重试。"
            in 500..599 -> " MiniMax 服务端错误，请稍后重试。"
            else -> ""
        }
        return "MiniMax HTTP $code。${message?.let { " $it。" } ?: ""}$hint"
    }

    private fun mapBusinessError(code: Int, message: String): String {
        val detail = when (code) {
            1001 -> "请求超时。"
            1002, 2045, 2056 -> "触发限流或用量限制。"
            1004, 2049 -> "API Key 无效或未授权。"
            1008 -> "账户余额不足。"
            1042 -> "输入包含过多不可见字符或非法字符。"
            2013 -> "请求参数无效。"
            20132 -> "音色样本或 voice_id 无效。"
            2042 -> "当前账号无权使用这个 voice_id。"
            else -> message.ifBlank { "MiniMax 业务错误。" }
        }
        return "MiniMax 错误 $code：$detail"
    }

    private fun sanitizeNetworkError(error: IOException): String =
        error.message?.replace(Regex("Bearer\\s+[^\\s]+", RegexOption.IGNORE_CASE), "Bearer ***")
            ?: "网络请求失败。"

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
