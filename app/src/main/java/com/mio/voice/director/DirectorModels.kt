package com.mio.voice.director

import com.mio.voice.data.DirectorConfig

data class DirectorPresetInfo(
    val presetId: String,
    val label: String,
    val description: String
)

data class DirectorRequest(
    val text: String,
    val voiceProfileId: String,
    val defaultPresetId: String,
    val presets: List<DirectorPresetInfo>,
    val config: DirectorConfig,
    /** 自定义系统提示词；为空时 provider 使用内置默认提示词。 */
    val systemPromptOverride: String? = null,
    /** 是否允许 AI 插入表演标记（语气词/停顿）。默认 false：prompt 不追加标记规则，行为与旧版一致。 */
    val autoPerformanceTags: Boolean = false
)

data class DirectorResult(
    val segments: List<DirectorRawSegment>,
    val rawJson: String
)

data class DirectorRawSegment(
    val text: String,
    val presetId: String
)

data class DirectorDraftSegment(
    val id: String,
    val text: String,
    val presetId: String,
    val warnings: List<String> = emptyList()
)

sealed class DirectorValidationResult {
    data class Valid(
        val segments: List<DirectorDraftSegment>,
        val warnings: List<String> = emptyList()
    ) : DirectorValidationResult()

    data class Invalid(val message: String) : DirectorValidationResult()
}

