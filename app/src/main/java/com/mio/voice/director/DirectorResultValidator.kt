package com.mio.voice.director

import com.mio.voice.data.VoiceLibrary
import com.mio.voice.data.VoiceProfile
import org.json.JSONObject

object DirectorResultValidator {
    fun parse(json: String): DirectorResult {
        val root = runCatching { JSONObject(json) }.getOrElse {
            throw DirectorValidationException("AI 返回不是合法 JSON。")
        }
        val array = root.optJSONArray("segments")
            ?: throw DirectorValidationException("AI JSON 缺少 segments。")
        if (array.length() == 0) {
            throw DirectorValidationException("AI 返回的 segments 为空。")
        }
        val segments = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index)
                    ?: throw DirectorValidationException("segments[$index] 不是对象。")
                val text = item.optString("text")
                val presetId = item.optString("presetId")
                if (text.isEmpty()) throw DirectorValidationException("segments[$index].text 为空。")
                if (presetId.isBlank()) throw DirectorValidationException("segments[$index].presetId 为空。")
                add(DirectorRawSegment(text = text, presetId = presetId))
            }
        }
        return DirectorResult(segments = segments, rawJson = json)
    }

    fun validate(
        originalText: String,
        voiceProfile: VoiceProfile,
        result: DirectorResult,
        allowTags: Boolean = false
    ): DirectorValidationResult {
        if (result.segments.isEmpty()) {
            return DirectorValidationResult.Invalid("AI 返回的 segments 为空。")
        }
        val normalizedVoice = VoiceLibrary.normalizeVoice(voiceProfile)
        val allowedPresetIds = normalizedVoice.presets.map { it.id }.toSet()
        val defaultPresetId = normalizedVoice.defaultPresetId
        val warnings = mutableListOf<String>()
        val prepared = result.segments.mapIndexed { index, segment ->
            val presetId = if (segment.presetId in allowedPresetIds) {
                segment.presetId
            } else {
                val warning = "第 ${index + 1} 段使用了不存在的 presetId：${segment.presetId}，已回退到默认预设。"
                warnings += warning
                defaultPresetId
            }
            DirectorDraftSegment(
                id = "director-$index",
                text = segment.text,
                presetId = presetId,
                warnings = if (presetId == segment.presetId) emptyList() else listOf(warnings.last())
            )
        }

        val joinedRaw = prepared.joinToString(separator = "") { it.text }
        // 核心文本：开启标记时先剥离合法标记，再做「忽略空白」的比对。
        // 容忍空白差异（小模型常多/少空格或换行），但标点/正文改写仍判废。
        val reconstructFailure =
            "AI 分段后的文本无法按原顺序还原原文，请重新分析或使用固定预设朗读。"
        val finalSegments: List<DirectorDraftSegment>
        if (allowTags) {
            if (PerformanceTags.hasUnknownTags(joinedRaw)) {
                return DirectorValidationResult.Invalid("AI 使用了不支持的标记，请重试或关闭「自动表演标记」。")
            }
            if (PerformanceTags.stripTags(joinedRaw).stripWhitespace() != originalText.stripWhitespace()) {
                return DirectorValidationResult.Invalid(reconstructFailure)
            }
            // 含标记的段不做字符级回镀（需保留标记位置），text 保留 AI 原样输出。
            finalSegments = prepared
        } else {
            if (joinedRaw.stripWhitespace() != originalText.stripWhitespace()) {
                return DirectorValidationResult.Invalid(reconstructFailure)
            }
            // 原文回镀：用原文实际切片重建每段 text，丢弃 AI 段里的空白变体，保证原标点空格。
            finalSegments = rebindToOriginal(prepared, originalText)
                ?: return DirectorValidationResult.Invalid(reconstructFailure)
        }

        return DirectorValidationResult.Valid(
            segments = mergeAdjacentSamePreset(finalSegments),
            warnings = warnings
        )
    }

    fun parseAndValidate(
        json: String,
        originalText: String,
        voiceProfile: VoiceProfile,
        allowTags: Boolean = false
    ): DirectorValidationResult =
        runCatching { parse(json) }
            .fold(
                onSuccess = { validate(originalText, voiceProfile, it, allowTags) },
                onFailure = { DirectorValidationResult.Invalid(it.message ?: "AI JSON 解析失败。") }
            )

    fun mergeAdjacentSamePreset(segments: List<DirectorDraftSegment>): List<DirectorDraftSegment> {
        val merged = mutableListOf<DirectorDraftSegment>()
        segments.forEach { segment ->
            val last = merged.lastOrNull()
            if (last != null && last.presetId == segment.presetId) {
                merged[merged.lastIndex] = last.copy(
                    text = last.text + segment.text,
                    warnings = last.warnings + segment.warnings
                )
            } else {
                merged += segment.copy(id = "director-${merged.size}")
            }
        }
        return merged.mapIndexed { index, segment -> segment.copy(id = "director-$index") }
    }

    /**
     * 用原文实际字符重建每段 text：按各段「非空白字符个数」从原文游标向前消费，段 text
     * 取原文对应切片（保留原标点、空格、换行）。任一段非空白字符不够、或消费完原文仍有剩余
     * 非空白字符时返回 null（视为还原失败）。
     */
    private fun rebindToOriginal(
        segments: List<DirectorDraftSegment>,
        originalText: String
    ): List<DirectorDraftSegment>? {
        var cursor = 0
        val rebound = ArrayList<DirectorDraftSegment>(segments.size)
        for (segment in segments) {
            val needed = segment.text.count { !it.isWhitespace() }
            var consumed = 0
            var end = cursor
            while (end < originalText.length && consumed < needed) {
                if (!originalText[end].isWhitespace()) consumed++
                end++
            }
            if (consumed < needed) return null
            rebound += segment.copy(text = originalText.substring(cursor, end))
            cursor = end
        }
        // 若原文末尾仍有未消费的非空白字符，说明对不齐。
        for (i in cursor until originalText.length) {
            if (!originalText[i].isWhitespace()) return null
        }
        return rebound
    }

    private fun String.stripWhitespace(): String = filterNot { it.isWhitespace() }
}

class DirectorValidationException(message: String) : Exception(message)

