package com.mio.voice.provider

import com.mio.voice.data.ProviderConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** 音色克隆结果：克隆得到的 voice_id 与可选的试听音频 URL。 */
data class VoiceCloneResult(
    val voiceId: String,
    val demoAudioUrl: String?
)

/**
 * MiniMax 音色快速复刻（Voice Clone）。
 *
 * 两步流程：
 * 1. [uploadSample] 上传音频样本（multipart, purpose=voice_clone）换取 file_id。
 * 2. [cloneVoice] 用 file_id + 自定义 voice_id 完成克隆；带 demoText 时返回试听音频 URL。
 *
 * 鉴权与 baseUrl 拼接策略与 [MiniMaxTtsProvider] 保持一致，复用同一 API Key。
 * 不需要 GroupId（官方 SDK 在使用 Bearer Key 时均不传）。
 */
class MiniMaxVoiceCloneProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
) {
    /** 上传音频样本，返回 file_id。 */
    suspend fun uploadSample(config: ProviderConfig, fileBytes: ByteArray, fileName: String): Long {
        val apiKey = config.apiKey?.takeIf { it.isNotBlank() }
            ?: throw MiniMaxTtsException("请先填写 API Key。")
        if (fileBytes.isEmpty()) throw MiniMaxTtsException("音频样本为空，请重新选择文件。")

        val mediaType = guessAudioMediaType(fileName)
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("purpose", "voice_clone")
            .addFormDataPart("file", fileName, fileBytes.toRequestBody(mediaType))
            .build()
        val httpRequest = Request.Builder()
            .url(uploadUrl(config))
            .header("Authorization", "Bearer $apiKey")
            .post(body)
            .build()
        val responseText = client.newCall(httpRequest).awaitBody()
        return parseFileId(responseText)
    }

    /** 用 file_id + 自定义 voice_id 完成克隆。带 demoText 时一并请求试听音频。 */
    suspend fun cloneVoice(
        config: ProviderConfig,
        fileId: Long,
        voiceId: String,
        demoText: String?,
        model: String?
    ): VoiceCloneResult {
        val apiKey = config.apiKey?.takeIf { it.isNotBlank() }
            ?: throw MiniMaxTtsException("请先填写 API Key。")
        val vid = voiceId.takeIf { it.isNotBlank() }
            ?: throw MiniMaxTtsException("请先填写 voice_id。")

        val payload = JSONObject()
            .put("file_id", fileId)
            .put("voice_id", vid)
        // text + model 是配套的试听字段：仅当提供了试听文本时才一起带上。
        demoText?.takeIf { it.isNotBlank() }?.let { text ->
            payload.put("text", text)
            payload.put("model", model?.takeIf { it.isNotBlank() } ?: "speech-2.5-hd-preview")
        }

        val httpRequest = Request.Builder()
            .url(cloneUrl(config))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON))
            .build()
        val responseText = client.newCall(httpRequest).awaitBody()
        val demoUrl = parseCloneResponse(responseText)
        return VoiceCloneResult(voiceId = vid, demoAudioUrl = demoUrl)
    }

    private fun uploadUrl(config: ProviderConfig): String = buildUrl(config, "files/upload")

    private fun cloneUrl(config: ProviderConfig): String = buildUrl(config, "voice_clone")

    private fun buildUrl(config: ProviderConfig, path: String): String {
        val base = config.baseUrl.ifBlank { "https://api.minimax.io" }.trimEnd('/')
        return if (base.endsWith("/v1")) "$base/$path" else "$base/v1/$path"
    }

    private fun guessAudioMediaType(fileName: String) =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            else -> "application/octet-stream"
        }.toMediaType()

    private fun parseFileId(json: String): Long {
        if (json.isBlank()) throw MiniMaxTtsException("文件上传返回了空响应。")
        val root = JSONObject(json)
        ensureSuccess(root)
        val file = root.optJSONObject("file")
            ?: throw MiniMaxTtsException("上传响应中没有文件信息。")
        val fileId = file.optLong("file_id", 0L)
        if (fileId == 0L) throw MiniMaxTtsException("上传响应中没有有效的 file_id。")
        return fileId
    }

    /** 返回 demo_audio URL（请求带 text 时存在），否则 null。 */
    private fun parseCloneResponse(json: String): String? {
        if (json.isBlank()) throw MiniMaxTtsException("音色克隆返回了空响应。")
        val root = JSONObject(json)
        ensureSuccess(root)
        return root.optString("demo_audio").takeIf { it.isNotBlank() }
    }

    private fun ensureSuccess(root: JSONObject) {
        val baseResp = root.optJSONObject("base_resp")
        val statusCode = baseResp?.optInt("status_code", 0) ?: 0
        if (statusCode != 0) {
            throw MiniMaxTtsException(mapBusinessError(statusCode, baseResp?.optString("status_msg").orEmpty()))
        }
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

    private fun httpError(code: Int, body: String): String {
        val message = runCatching {
            JSONObject(body).optJSONObject("base_resp")?.optString("status_msg")
        }.getOrNull()?.takeIf { it.isNotBlank() }
        val hint = when (code) {
            401, 403 -> " 请检查 API Key 与权限。"
            408 -> " 请求超时。"
            413 -> " 音频文件过大。"
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
            2013 -> "请求参数无效（请检查 voice_id 是否符合规则、音频样本是否有效）。"
            20132 -> "音色样本或 voice_id 无效。"
            2038 -> "需先在 MiniMax 开放平台完成实名认证。"
            else -> message.ifBlank { "MiniMax 业务错误。" }
        }
        return "MiniMax 错误 $code：$detail"
    }

    private fun sanitizeNetworkError(error: IOException): String =
        error.message?.replace(Regex("Bearer\\s+[^\\s]+", RegexOption.IGNORE_CASE), "Bearer ***")
            ?: "网络请求失败。"

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
