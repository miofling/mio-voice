package com.mio.voice.provider

/**
 * 从官方音色的 voice_id / voice_name 启发式推断「语言」与「性别」。
 *
 * MiniMax 的 get_voice 接口不返回结构化的 language / gender 字段，只能据命名推断：
 * - 新版音色形如 `Language_RoleDescription`（如 `English_Trustworthy_Man`）。
 * - 旧版中文音色形如 `male-qn-qingse` / `female-shaonv`（拼音 + male/female 前缀，均为普通话）。
 * 推不出的（角色/拟物音等）一律归 [Language.Other] / [Gender.Other]，不臆造。
 */
object OfficialVoiceClassifier {

    enum class Language(val label: String) {
        Chinese("中文"),
        Cantonese("粤语"),
        English("英语"),
        Japanese("日语"),
        Korean("韩语"),
        Spanish("西语"),
        French("法语"),
        German("德语"),
        Russian("俄语"),
        Italian("意语"),
        Portuguese("葡语"),
        Arabic("阿语"),
        Other("其他")
    }

    enum class Gender(val label: String) {
        Male("男声"),
        Female("女声"),
        Other("其他")
    }

    // voiceId 语言前缀表。更特异的前缀排在更前（先匹配 "Chinese (Mandarin)_" 再 "Chinese_"）。
    private val languagePrefixes: List<Pair<String, Language>> = listOf(
        "chinese (mandarin)_" to Language.Chinese,
        "chinese_" to Language.Chinese,
        "cantonese_" to Language.Cantonese,
        "english_" to Language.English,
        "japanese_" to Language.Japanese,
        "korean_" to Language.Korean,
        "spanish_" to Language.Spanish,
        "french_" to Language.French,
        "german_" to Language.German,
        "russian_" to Language.Russian,
        "italian_" to Language.Italian,
        "portuguese_" to Language.Portuguese,
        "arabic_" to Language.Arabic
    )

    fun language(voice: OfficialVoice): Language {
        val id = voice.voiceId.trim().lowercase()
        languagePrefixes.firstOrNull { id.startsWith(it.first) }?.let { return it.second }
        // 旧版拼音中文音色：male-/female- 前缀，或常见拼音角色音，均为普通话。
        if (id.startsWith("male-") || id.startsWith("female-")) return Language.Chinese
        // voiceName 含中文字符 -> 视为中文（兜底，旧版无前缀音色名多为中文）。
        if (voice.voiceName.any { it in '\u4e00'..'\u9fff' }) return Language.Chinese
        return Language.Other
    }

    // 性别关键词。先判女后判男，避免 "woman" 内含 "man" 之类误判。词边界匹配。
    private val femaleWords = listOf(
        "female", "woman", "women", "girl", "lady", "sister", "queen",
        "grandma", "granny", "mom", "mother", "aunt", "heroine", "actress"
    )
    private val maleWords = listOf(
        "male", "man", "men", "boy", "gentleman", "guy", "uncle",
        "grandpa", "dad", "father", "brother", "bloke", "hero", "king", "boss"
    )

    fun gender(voice: OfficialVoice): Gender {
        val id = voice.voiceId.trim().lowercase()
        // 旧版前缀优先且最可靠。
        if (id.startsWith("female-")) return Gender.Female
        if (id.startsWith("male-")) return Gender.Male

        // 用子串匹配（音色名常为 camelCase 拼接，如 SereneWoman，无词边界）。
        // 先判女后判男：可正确处理 "woman" 内含 "man" 的情形。
        val haystack = (voice.voiceId + " " + voice.voiceName).lowercase()
        if (femaleWords.any { haystack.contains(it) }) return Gender.Female
        if (maleWords.any { haystack.contains(it) }) return Gender.Male

        // 中文名兜底。
        val name = voice.voiceName
        if (name.any { it in "女姐妹媛婆奶娘母后" }) return Gender.Female
        if (name.any { it in "男爷叔哥弟父兄" }) return Gender.Male

        return Gender.Other
    }

    // ---- 高置信判定（仅信最可靠信号，用于「缩减官方音色库」过滤）----

    // 高置信角色词白名单。整词（拆 token + camelCase 拆分后）精确匹配，不做子串匹配，
    // 因此 "Mentor" 不会命中 "men"、"Salesman" 不会命中 "man"。
    // 已剔除过泛、易误判的词：guy / boss / bloke。
    private val femaleTokens = setOf(
        "female", "woman", "women", "girl", "girls", "lady", "ladies",
        "mom", "mother", "sister", "queen", "grandma", "granny", "aunt",
        "actress", "heroine"
    )
    private val maleTokens = setOf(
        "male", "man", "men", "boy", "boys", "gentleman", "uncle",
        "grandpa", "dad", "father", "brother", "hero", "king"
    )

    /** 把字符串按非字母切分，并对每个 token 做 camelCase 拆分，返回全小写子词集合。 */
    private fun roleTokens(raw: String): Set<String> {
        val out = mutableSetOf<String>()
        for (chunk in raw.split(Regex("[^A-Za-z]+"))) {
            if (chunk.isEmpty()) continue
            // camelCase 拆分：在小写/数字→大写边界处断开（SereneWoman -> Serene, Woman）。
            for (sub in chunk.split(Regex("(?<=[a-z])(?=[A-Z])"))) {
                if (sub.isNotEmpty()) out += sub.lowercase()
            }
        }
        return out
    }

    /**
     * 高置信性别：只认旧版 male-/female- 前缀，或 voice_id 里出现白名单角色词（整词匹配）。
     * 无法可靠判定时返回 null（不臆造、不看中文名兜底）。
     */
    fun confidentGender(voice: OfficialVoice): Gender? {
        val id = voice.voiceId.trim().lowercase()
        if (id.startsWith("female-")) return Gender.Female
        if (id.startsWith("male-")) return Gender.Male

        val tokens = roleTokens(voice.voiceId)
        // 先判女后判男（白名单互斥，顺序仅为稳妥）。
        if (tokens.any { it in femaleTokens }) return Gender.Female
        if (tokens.any { it in maleTokens }) return Gender.Male
        return null
    }

    /**
     * 高置信语言：只认 voice_id 前缀（languagePrefixes 或 male-/female-=中文）。
     * 纯靠中文名兜底的不算高置信，返回 null。
     */
    fun confidentLanguage(voice: OfficialVoice): Language? {
        val id = voice.voiceId.trim().lowercase()
        languagePrefixes.firstOrNull { id.startsWith(it.first) }?.let { return it.second }
        if (id.startsWith("male-") || id.startsWith("female-")) return Language.Chinese
        return null
    }

    /** 仅保留性别与语言都能从命名强信号可靠判定的音色（特征最明显、不会误判的那批）。 */
    fun confidentVoices(voices: List<OfficialVoice>): List<OfficialVoice> =
        voices.filter { confidentGender(it) != null && confidentLanguage(it) != null }
}
