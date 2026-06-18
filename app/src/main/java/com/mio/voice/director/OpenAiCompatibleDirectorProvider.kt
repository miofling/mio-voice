package com.mio.voice.director

import com.mio.voice.data.DirectorConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OpenAiCompatibleDirectorProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : DirectorProvider {
    override suspend fun analyze(request: DirectorRequest): DirectorResult {
        val apiKey = request.config.apiKey?.takeIf { it.isNotBlank() }
            ?: throw DirectorProviderException("请先填写 AI 导演 API Key。")
        val model = request.config.model.takeIf { it.isNotBlank() }
            ?: throw DirectorProviderException("请先填写 AI 导演模型名。")
        val body = JSONObject()
            .put("model", model)
            .put("temperature", 0.2)
            .put("response_format", JSONObject().put("type", "json_object"))
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                    .put(JSONObject().put("role", "user").put("content", userPrompt(request)))
            )

        val httpRequest = Request.Builder()
            .url(endpointUrl(request.config))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON))
            .build()

        val responseText = client.newCall(httpRequest).awaitBody()
        val content = parseChatContent(responseText)
        return DirectorResultValidator.parse(content)
    }

    private fun userPrompt(request: DirectorRequest): String =
        buildString {
            appendLine("完整原文如下，必须只划分这段原文，不能改写、翻译、总结、补充或删减：")
            appendLine("<text>")
            appendLine(request.text)
            appendLine("</text>")
            appendLine()
            appendLine("当前父音色 ID：${request.voiceProfileId}")
            appendLine("默认 presetId：${request.defaultPresetId}")
            appendLine("可用情绪预设，只能从这些 presetId 中选择：")
            request.presets.forEach { preset ->
                appendLine("- presetId: ${preset.presetId}")
                appendLine("  label: ${preset.label}")
                appendLine("  description: ${preset.description.ifBlank { "无" }}")
            }
            appendLine()
            appendLine("要求：")
            appendLine("1. 只能输出 JSON 对象，结构为 {\"segments\":[{\"text\":\"连续的一段原文\",\"presetId\":\"preset_id\"}]}")
            appendLine("2. 只能使用上面提供的 presetId。")
            appendLine("3. segment.text 必须是原文中连续出现的文本片段，所有 text 按顺序拼接后必须与原文完全一致。")
            appendLine("4. 连续且情绪相同的内容尽量合并；只有情绪明显变化时才切换预设。")
            appendLine("5. 不要机械地一句一段。")
            appendLine("6. 不输出 MiniMax emotion、speed、pitch。")
        }

    private fun parseChatContent(json: String): String {
        if (json.isBlank()) throw DirectorProviderException("AI 导演返回空响应。")
        val root = JSONObject(json)
        root.optJSONObject("error")?.let { error ->
            throw DirectorProviderException(error.optString("message").ifBlank { "AI 导演接口返回错误。" })
        }
        val choices = root.optJSONArray("choices")
            ?: throw DirectorProviderException("AI 导演响应缺少 choices。")
        if (choices.length() == 0) throw DirectorProviderException("AI 导演响应 choices 为空。")
        val content = choices.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            .orEmpty()
        if (content.isBlank()) throw DirectorProviderException("AI 导演响应没有 JSON 内容。")
        return content.trim()
    }

    private suspend fun Call.awaitBody(): String =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { cancel() }
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(DirectorProviderException(sanitizeNetworkError(e)))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val body = it.body?.string().orEmpty()
                        if (!it.isSuccessful) {
                            continuation.resumeWithException(DirectorProviderException(httpError(it.code, body)))
                        } else {
                            continuation.resume(body)
                        }
                    }
                }
            })
        }

    private fun endpointUrl(config: DirectorConfig): String {
        val endpoint = config.endpointPath.ifBlank { "/v1/chat/completions" }
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) return endpoint
        val base = config.baseUrl.ifBlank { "https://api.openai.com" }.trimEnd('/')
        val path = if (endpoint.startsWith('/')) endpoint else "/$endpoint"
        return base + path
    }

    private fun httpError(code: Int, body: String): String {
        val message = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull()?.takeIf { it.isNotBlank() }
        val hint = when (code) {
            401, 403 -> " 请检查 AI 导演 API Key 和模型权限。"
            408 -> " 请求超时。"
            429 -> " 触发限流，请稍后重试。"
            in 500..599 -> " AI 服务端错误，请稍后重试。"
            else -> ""
        }
        return "AI 导演 HTTP $code。${message?.let { " $it。" } ?: ""}$hint"
    }

    private fun sanitizeNetworkError(error: IOException): String =
        error.message?.replace(Regex("Bearer\\s+[^\\s]+", RegexOption.IGNORE_CASE), "Bearer ***")
            ?: "AI 导演网络请求失败。"

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
        const val SYSTEM_PROMPT = """
你是 Mio Voice 的 AI 配音导演。你只负责把用户给出的连续原文按明显情绪变化划分为配音块，并为每个块选择一个提供的 presetId。
禁止改写、翻译、总结、补充或删减原文。禁止输出 MiniMax emotion、speed、pitch。禁止使用未提供的 presetId。
输出必须是严格 JSON 对象，不要 Markdown，不要解释。
"""
    }
}

class DirectorProviderException(message: String) : Exception(message)

