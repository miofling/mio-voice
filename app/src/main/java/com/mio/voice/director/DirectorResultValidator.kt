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
        result: DirectorResult
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

        val joined = prepared.joinToString(separator = "") { it.text }.normalizeLineEndings()
        if (joined != originalText.normalizeLineEndings()) {
            return DirectorValidationResult.Invalid("AI 分段后的文本无法按原顺序还原原文，请重新分析或使用固定预设朗读。")
        }

        return DirectorValidationResult.Valid(
            segments = mergeAdjacentSamePreset(prepared),
            warnings = warnings
        )
    }

    fun parseAndValidate(
        json: String,
        originalText: String,
        voiceProfile: VoiceProfile
    ): DirectorValidationResult =
        runCatching { parse(json) }
            .fold(
                onSuccess = { validate(originalText, voiceProfile, it) },
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

    private fun String.normalizeLineEndings(): String =
        replace("\r\n", "\n").replace('\r', '\n')
}

class DirectorValidationException(message: String) : Exception(message)

