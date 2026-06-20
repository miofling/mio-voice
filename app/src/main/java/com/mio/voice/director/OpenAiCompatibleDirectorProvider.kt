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
                    .put(JSONObject().put("role", "system").put("content", request.systemPromptOverride?.takeIf { it.isNotBlank() } ?: DEFAULT_SYSTEM_PROMPT))
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

    override suspend fun fetchModels(config: DirectorConfig): List<String> {
        val apiKey = config.apiKey?.takeIf { it.isNotBlank() }
            ?: throw DirectorProviderException("请先填写 AI API Key。")
        val httpRequest = Request.Builder()
            .url(modelsUrl(config))
            .header("Authorization", "Bearer $apiKey")
            .get()
            .build()
        val responseText = client.newCall(httpRequest).awaitBody()
        return parseModelsResponse(responseText)
    }

    private fun parseModelsResponse(json: String): List<String> {
        if (json.isBlank()) throw DirectorProviderException("模型列表响应为空。")
        val root = JSONObject(json)
        root.optJSONObject("error")?.let { error ->
            throw DirectorProviderException(error.optString("message").ifBlank { "拉取模型失败。" })
        }
        val result = mutableListOf<String>()
        val arrays = listOf(root.optJSONArray("data"), root.optJSONArray("models"))
        arrays.forEach { array ->
            if (array != null) {
                for (i in 0 until array.length()) {
                    when (val item = array.get(i)) {
                        is JSONObject -> item.optString("id").takeIf { it.isNotBlank() }?.let(result::add)
                        is String -> item.takeIf { it.isNotBlank() }?.let(result::add)
                    }
                }
            }
        }
        if (result.isEmpty()) throw DirectorProviderException("没有解析到可用模型，请保留手动填写模型名。")
        return result.distinct()
    }

    private fun modelsUrl(config: DirectorConfig): String {
        val base = config.baseUrl.ifBlank { "https://api.openai.com" }.trimEnd('/')
        return if (base.endsWith("/v1")) "$base/models" else "$base/v1/models"
    }

    internal fun userPrompt(request: DirectorRequest): String =
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
            appendLine("输出格式要求：")
            appendLine("1. 只能输出 JSON 对象，结构为 {\"segments\":[{\"text\":\"连续的一段原文\",\"presetId\":\"preset_id\"}]}")
            appendLine("2. presetId 只能从上面列出的预设中选择，禁止编造。")
            if (request.autoPerformanceTags) {
                appendLine("3. 每个 segment.text 必须基于原文中连续出现的片段；**除了下方允许的表演标记外**，所有 text 按出现顺序拼接、再移除全部表演标记后，必须与原文逐字完全一致（含标点、空格、换行），不得改写、翻译、总结、补充或删减任何正文字符。")
            } else {
                appendLine("3. 每个 segment.text 必须是原文中连续出现的片段，所有 text 按出现顺序拼接后必须与原文逐字完全一致（含标点、空格、换行），不得改写、翻译、总结、补充或删减。")
            }
            appendLine("4. 不输出 MiniMax 的 emotion、speed、pitch 字段。")
            appendLine()
            appendLine("分段原则（按情绪划分，让每段对应一种较一致的情绪/语气）：")
            appendLine("5. 按情绪/语气的变化来分段：当原文中出现明显不同的情绪、语气或场景时，应拆成多段，每段对应一种较一致的情绪。不要把明显不同的情绪硬塞进同一段。")
            appendLine("6. 出现下面任一“切换信号”时，应当在该处分段：")
            appendLine("   a) 情绪/语气发生明确转折（如由平静转激动、由开心转难过）；")
            appendLine("   b) 叙述（旁白）与人物对话之间相互切换；")
            appendLine("   c) 不同人物的对话之间切换。")
            appendLine("7. 同一种情绪连续的内容可以放在同一段，不必逐句切分；但也不要为了追求段数少而把不同情绪混在一段里，情绪贴合度优先于段数多少。")
            appendLine("8. 选 presetId 时，依据每个预设的 label 与 description 来匹配该段最贴切的情绪；当一段情绪确实不明显或难以判断时，使用默认 presetId（${request.defaultPresetId}）。")
            appendLine("9. 相邻两段如果选了相同的 presetId，应合并为一段。")
            appendLine("10. 输出前自检：每段内部情绪应尽量统一，原文中情绪明显变化的位置应当有分段边界。")
            if (request.autoPerformanceTags) {
                appendLine()
                appendLine("表演标记规则（仅本次开启了「自动表演标记」，务必严格遵守）：")
                appendLine("11. 你可以在 segment.text 的合适位置**插入**以下两类标记，让配音更自然，但这不是必须的——不合适就不要加。")
                appendLine("12. 允许的语气词标签（必须原样使用，区分大小写与连字符，不得自创、不得改拼写、不得增减字母）：")
                appendLine("    " + PerformanceTags.INTERJECTIONS.joinToString(" "))
                appendLine("13. 允许的停顿标记：<#秒数#>，秒数范围 0.01~99.99，最多两位小数，例如 <#0.5#>。停顿标记必须放在可朗读文本之间，不得两个停顿标记相邻连用。")
                appendLine("14. 除上述两类标记外，**禁止插入任何其它括号标记或符号**；移除全部标记后必须与原文逐字一致（见第 3 条）。")
                appendLine("15. 标记要少而精、服务于表演：仅在文本本身明确暗示该动作/停顿时才加（如出现“他笑了”“叹了口气”“沉默片刻”），情绪或动作不明确时一律不加。")
            }
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

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        /** AI 导演内置的系统提示词（角色设定）。展示 / 恢复默认时引用此常量。 */
        const val DEFAULT_SYSTEM_PROMPT = """你是 Mio Voice 的 AI 配音导演。你只负责把用户给出的连续原文按明显情绪变化划分为配音块，并为每个块选择一个提供的 presetId。
禁止改写、翻译、总结、补充或删减原文。禁止输出 MiniMax emotion、speed、pitch。禁止使用未提供的 presetId。
输出必须是严格 JSON 对象，不要 Markdown，不要解释。"""
    }
}

class DirectorProviderException(message: String) : Exception(message)

