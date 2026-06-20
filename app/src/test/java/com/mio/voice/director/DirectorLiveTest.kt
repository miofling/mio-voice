package com.mio.voice.director

import com.mio.voice.data.DirectorConfig
import com.mio.voice.data.EmotionPreset
import com.mio.voice.data.VoiceLibrary
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * 联网真实测试：用根目录 .director-test.env 里的 URL/Key/模型，打真实 AI 导演接口，
 * 走改过的 [OpenAiCompatibleDirectorProvider]（新 prompt）+ [DirectorResultValidator]（宽容校验+回镀），
 * 打印分段结果。
 *
 * 这是一个手动/按需测试：当 .director-test.env 不存在或没填值时，会被 assume 跳过，
 * 不影响常规 ./gradlew testDebugUnitTest。
 *
 * 单独运行：
 *   ./gradlew :app:testDebugUnitTest --tests "com.mio.voice.director.DirectorLiveTest" --info
 */
class DirectorLiveTest {

    private data class Env(
        val baseUrl: String,
        val apiKey: String,
        val models: List<String>,
        val endpointPath: String
    )

    private fun readEnvFile(): Map<String, String> {
        val candidates = listOf(
            File(".director-test.env"),
            File("../.director-test.env"),
            File(System.getProperty("user.dir"), "../.director-test.env")
        )
        val file = candidates.firstOrNull { it.exists() } ?: return emptyMap()
        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
            .associate { line ->
                val idx = line.indexOf('=')
                line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }
    }

    private fun liveFlagFromEnvFile(): Boolean = readEnvFile()["DIRECTOR_LIVE"] == "1"

    private fun loadEnv(): Env? {
        // 测试工作目录是 app/，env 文件在仓库根目录。
        val map = readEnvFile()
        if (map.isEmpty()) return null
        val baseUrl = map["DIRECTOR_BASE_URL"].orEmpty()
        val apiKey = map["DIRECTOR_API_KEY"].orEmpty()
        val singleModel = map["DIRECTOR_MODEL"].orEmpty()
        val multiModels = map["DIRECTOR_MODELS"].orEmpty()
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val models = multiModels.ifEmpty { listOfNotNull(singleModel.takeIf { it.isNotBlank() }) }
        if (baseUrl.isBlank() || apiKey.isBlank() || models.isEmpty()) return null
        return Env(
            baseUrl = baseUrl,
            apiKey = apiKey,
            models = models,
            endpointPath = map["DIRECTOR_ENDPOINT_PATH"]?.takeIf { it.isNotBlank() } ?: "/v1/chat/completions"
        )
    }

    private data class Sample(val name: String, val text: String, val note: String)

    /** 多种风格的测试文本，覆盖：多情绪起伏、纯单一情绪、密集对话、英文标点、短文本。 */
    private val samples: List<Sample> = listOf(
        Sample(
            name = "多情绪起伏(旁白+对话)",
            note = "期待→平静叙事→难过→平复，预期切多段",
            text = """
                清晨的阳光照进窗台，她伸了个懒腰，心里满是期待：今天终于要去看海了。
                可是刚走到门口，手机响了。是公司打来的：项目出了严重事故，必须马上回去处理。
                她愣在原地，眼眶一下子红了。"为什么偏偏是今天……"她低声说，声音有些发抖。
                过了好一会儿，她深吸一口气，擦掉眼泪，重新站直："没关系，海一直都在，先把事情解决了再说。"
            """.trimIndent()
        ),
        Sample(
            name = "纯单一情绪(平静叙述)",
            note = "全程平静说明文，预期不该过度切分（理想1-2段）",
            text = """
                水在标准大气压下，温度达到一百摄氏度时开始沸腾。沸腾时液体内部和表面同时发生剧烈的汽化。
                这个温度被称为沸点。海拔升高时气压下降，沸点也会随之降低。
                因此在高原地区，水往往不到一百度就会沸腾，煮饭也就更难熟透。
            """.trimIndent()
        ),
        Sample(
            name = "密集多人对话",
            note = "三人对话快速切换+情绪不同，预期切多段",
            text = """
                "你到底来不来？"小明不耐烦地喊。
                小红怯生生地回答："我……我有点害怕。"
                "怕什么，有我在呢！"小刚拍着胸脯，满脸得意。
                小明叹了口气："行了行了，别吹了，赶紧走吧。"
            """.trimIndent()
        ),
        Sample(
            name = "英文标点+中英混排",
            note = "含半角逗号句号引号，检验回镀对英文标点的处理",
            text = """
                "Are you sure about this?" she asked, frowning.
                He nodded slowly. "Yes. I've never been more certain in my life."
                然后他笑了，那种久违的、发自内心的笑。
            """.trimIndent()
        ),
        Sample(
            name = "短文本(两句)",
            note = "很短，检验是否会被无意义切碎",
            text = "他猛地站起来，怒吼道：滚出去！房间里瞬间安静得可怕。"
        )
    )

    private fun voice() =
        VoiceLibrary.upsertPreset(
            VoiceLibrary.upsertPreset(
                VoiceLibrary.upsertPreset(
                    VoiceLibrary.upsertPreset(
                        VoiceLibrary.upsertPreset(
                            VoiceLibrary.createVoice("测试音色", "voice-id", id = "voice"),
                            EmotionPreset("calm", "平静", "neutral", 1.0f, 0, description = "平稳、自然的叙述语气。"),
                            makeDefault = true
                        ),
                        EmotionPreset("happy", "开心", "happy", 1.1f, 1, description = "愉快、明亮、充满期待。"),
                        makeDefault = false
                    ),
                    EmotionPreset("sad", "难过", "sad", 0.9f, -2, description = "低落、委屈、声音发抖。"),
                    makeDefault = false
                ),
                EmotionPreset("angry", "愤怒", "angry", 1.05f, 1, description = "激动、生气、提高音量。"),
                makeDefault = false
            ),
            EmotionPreset("fear", "害怕", "fearful", 0.95f, 0, description = "紧张、胆怯、迟疑。"),
            makeDefault = false
        )

    @Test
    fun liveDirectorAnalysis() = runBlocking {
        // 显式开关：环境变量 DIRECTOR_LIVE=1 或 .director-test.env 里 DIRECTOR_LIVE=1 才联网跑，
        // 否则常规 testDebugUnitTest 会跳过它（避免每次构建都真联网）。
        val live = System.getenv("DIRECTOR_LIVE") == "1" || liveFlagFromEnvFile()
        assumeTrue(
            "跳过：未开启联网测试（设 DIRECTOR_LIVE=1 或在 .director-test.env 写 DIRECTOR_LIVE=1）。",
            live
        )
        val env = loadEnv()
        assumeTrue(
            "跳过：未找到 .director-test.env 或未填 URL/Key/模型（这是按需联网测试）。",
            env != null
        )
        env!!

        val rounds = (System.getenv("DIRECTOR_ROUNDS") ?: "3").toIntOrNull()?.coerceIn(1, 20) ?: 3
        val voice = voice()
        val presets = voice.presets.map {
            DirectorPresetInfo(presetId = it.id, label = it.label, description = it.description)
        }
        val provider = OpenAiCompatibleDirectorProvider()

        // 统计：每个 (model) 累计成功/失败/逐字还原次数 + 每个 cell 的段数序列。
        val summary = StringBuilder()

        println("================ AI 导演联网测试（多文本×多模型×多轮）================")
        println("接口: ${env.baseUrl}${env.endpointPath}")
        println("模型: ${env.models}  | 文本: ${samples.size} 段 | 每组轮数: $rounds")

        for (model in env.models) {
            println()
            println("==================== 模型: $model ====================")
            var ok = 0; var fail = 0; var exact = 0; var total = 0
            for (sample in samples) {
                val segCounts = mutableListOf<Int>()
                val presetSeqs = mutableListOf<String>()
                println()
                println("  ◆ 文本《${sample.name}》(${sample.note}) 非空白字符=${sample.text.count { !it.isWhitespace() }}")
                repeat(rounds) { round ->
                    total++
                    val request = DirectorRequest(
                        text = sample.text,
                        voiceProfileId = voice.id,
                        defaultPresetId = voice.defaultPresetId,
                        presets = presets,
                        config = DirectorConfig(
                            baseUrl = env.baseUrl,
                            endpointPath = env.endpointPath,
                            apiKey = env.apiKey,
                            model = model
                        ),
                        systemPromptOverride = null,
                        autoPerformanceTags = false
                    )
                    val result = runCatching { provider.analyze(request) }.getOrElse { e ->
                        fail++
                        println("    第${round + 1}轮 ❌ 请求/解析失败: ${e.message}")
                        runCatching { dumpRawContent(env, model, provider, request) }
                            .onFailure { println("      （原始返回获取失败: ${it.message}）") }
                        return@repeat
                    }
                    val validation = DirectorResultValidator.validate(
                        originalText = sample.text,
                        voiceProfile = voice,
                        result = result,
                        allowTags = false
                    )
                    when (validation) {
                        is DirectorValidationResult.Valid -> {
                            ok++
                            val rebuilt = validation.segments.joinToString("") { it.text }
                            val isExact = rebuilt == sample.text
                            if (isExact) exact++
                            segCounts += validation.segments.size
                            val presetSeq = validation.segments.joinToString(">") { it.presetId }
                            presetSeqs += presetSeq
                            val rawN = result.segments.size
                            println(
                                "    第${round + 1}轮 ✅ 原始${rawN}段→合并${validation.segments.size}段 " +
                                    "| 情绪序列: $presetSeq | 逐字还原原文: ${if (isExact) "是" else "否(仅空白容忍)"}"
                            )
                        }
                        is DirectorValidationResult.Invalid -> {
                            fail++
                            println("    第${round + 1}轮 ❌ 校验失败: ${validation.message}")
                        }
                    }
                }
                if (segCounts.isNotEmpty()) {
                    val distinctSeq = presetSeqs.distinct()
                    println(
                        "    └ 小结: 段数 ${segCounts}  | 情绪序列稳定性: ${distinctSeq.size} 种" +
                            if (distinctSeq.size > 1) " ⚠不稳定" else " ✓一致"
                    )
                }
            }
            val line = "[$model] 通过 $ok/$total，失败 $fail，逐字还原 $exact/$ok"
            summary.appendLine(line)
            println()
            println("  ── 模型 $model 汇总: $line ──")
        }
        println()
        println("================ 总汇总 ================")
        print(summary)
        println("=======================================")
    }

    /** 用与 provider 完全一致的请求体直接发一次，打印模型返回的原始 content（用于诊断非法 JSON）。 */
    private fun dumpRawContent(
        env: Env,
        model: String,
        provider: OpenAiCompatibleDirectorProvider,
        request: DirectorRequest
    ) {
        val body = JSONObject()
            .put("model", model)
            .put("temperature", 0.2)
            .put("response_format", JSONObject().put("type", "json_object"))
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", OpenAiCompatibleDirectorProvider.DEFAULT_SYSTEM_PROMPT))
                    .put(JSONObject().put("role", "user").put("content", provider.userPrompt(request)))
            )
        val url = env.baseUrl.trimEnd('/') + env.endpointPath
        val httpReq = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${env.apiKey}")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        OkHttpClient().newCall(httpReq).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            val content = runCatching {
                JSONObject(raw).optJSONArray("choices")?.optJSONObject(0)
                    ?.optJSONObject("message")?.optString("content")
            }.getOrNull()
            println("  ── 原始 content（HTTP ${resp.code}）──")
            println("  " + (content ?: raw).take(1200).replace("\n", "\n  "))
            println("  ──────────────────────────────")
        }
    }
}
