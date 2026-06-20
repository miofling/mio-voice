package com.mio.voice.director

/**
 * MiniMax speech-2.8 系列「表演标记」的纯逻辑封装：语气词标签 + 停顿标记。
 *
 * 这些标记由 AI 导演在开启「自动表演标记」开关时插入到生成文本里，**只作用于发给 MiniMax 的文本**，
 * 不改变原文内容。校验时通过 [stripTags] 剥离后必须与原文逐字一致，从而保留防幻觉护栏。
 *
 * 标签清单与格式均核对自官方 T2A v2 文档（platform.minimax.io）：
 * - 语气词：19 个，半角圆括号、全小写、多词用连字符。
 * - 停顿：`<#x#>`，x 为秒数，范围 [0.01, 99.99]，最多两位小数。
 * 仅 speech-2.8-hd / speech-2.8-turbo 支持。
 */
object PerformanceTags {
    /** 官方 19 个语气词标签（精确拼写，区分大小写与连字符）。 */
    val INTERJECTIONS: Set<String> = setOf(
        "(laughs)", "(chuckle)", "(coughs)", "(clear-throat)", "(groans)",
        "(breath)", "(pant)", "(inhale)", "(exhale)", "(gasps)",
        "(sniffs)", "(sighs)", "(snorts)", "(burps)", "(lip-smacking)",
        "(humming)", "(hissing)", "(emm)", "(sneezes)"
    )

    /** 匹配任意 `(xxx)` 形式的候选语气词标签（内部不含括号）。 */
    private val PARENS_TAG = Regex("""\([^()]*\)""")

    /** 匹配任意 `<#...#>` 形式的候选停顿标记。 */
    private val PAUSE_TAG = Regex("""<#([^#<>]*)#>""")

    /** 合法停顿：1~2 位整数 . 最多两位小数，且数值在 [0.01, 99.99]。 */
    private fun isValidPause(inner: String): Boolean {
        if (!Regex("""\d{1,2}(\.\d{1,2})?""").matches(inner)) return false
        val value = inner.toDoubleOrNull() ?: return false
        return value in 0.01..99.99
    }

    /**
     * 移除文本中所有**合法**的语气词标签与停顿标记，返回纯正文。
     * 非法/未知标记（清单外的 `(foo)`、超范围的 `<#999#>`）会被保留——这样剥离后与原文比对时会暴露出来。
     */
    fun stripTags(text: String): String {
        var result = PAUSE_TAG.replace(text) { match ->
            if (isValidPause(match.groupValues[1].trim())) "" else match.value
        }
        result = PARENS_TAG.replace(result) { match ->
            if (match.value in INTERJECTIONS) "" else match.value
        }
        return result
    }

    /**
     * 找出文本里**不被支持**的标记：清单外的 `(xxx)` 圆括号标签，或格式/范围非法的 `<#...#>` 停顿。
     * 用于校验时判废与提示。
     *
     * 注意：正文里本就可能含普通圆括号（如「（注）」用的是中文全角，不会被英文 `(` 匹配；
     * 但英文半角括号确有歧义）。这里只在「开启自动标记」校验路径调用，正文中出现的半角 `(…)`
     * 也会被当作未知标记 → 这是有意的保守策略：开启该功能时不建议正文夹带半角圆括号。
     */
    fun extractUnknownTags(text: String): List<String> {
        val unknown = mutableListOf<String>()
        PAUSE_TAG.findAll(text).forEach { match ->
            if (!isValidPause(match.groupValues[1].trim())) unknown += match.value
        }
        PARENS_TAG.findAll(text).forEach { match ->
            if (match.value !in INTERJECTIONS) unknown += match.value
        }
        return unknown
    }

    /** 是否含有不被支持的标记。 */
    fun hasUnknownTags(text: String): Boolean = extractUnknownTags(text).isNotEmpty()
}
