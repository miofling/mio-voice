package com.mio.voice.director

import org.json.JSONArray
import org.json.JSONObject

/**
 * AI 导演分段「草稿预览」的持久化载体：把分析出来、尚未/正在生成的分段预览存到 DataStore，
 * 实现切 tab / 切后台 / 进程重建后仍能恢复预览，直到用户放弃或重新分析替换。
 *
 * 只存预览草稿（文本+预设+警告+关联父音色），不含已生成音频（那走 Room）。
 */
data class DirectorDraft(
    val voiceProfileId: String,
    val segments: List<DirectorDraftSegment>,
    val warnings: List<String> = emptyList()
)

object DirectorDraftSerializer {
    private const val VERSION = 1

    fun serialize(draft: DirectorDraft): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("voiceProfileId", draft.voiceProfileId)
        root.put("warnings", JSONArray(draft.warnings))
        val segs = JSONArray()
        draft.segments.forEach { s ->
            segs.put(
                JSONObject()
                    .put("id", s.id)
                    .put("text", s.text)
                    .put("presetId", s.presetId)
                    .put("warnings", JSONArray(s.warnings))
            )
        }
        root.put("segments", segs)
        return root.toString()
    }

    /** 反序列化；任何异常或字段缺失返回 null（容错，坏数据当作没有草稿）。 */
    fun deserialize(json: String?): DirectorDraft? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val root = JSONObject(json)
            val voiceProfileId = root.optString("voiceProfileId")
            if (voiceProfileId.isBlank()) return null
            val segArray = root.optJSONArray("segments") ?: return null
            if (segArray.length() == 0) return null
            val segments = buildList {
                for (i in 0 until segArray.length()) {
                    val o = segArray.optJSONObject(i) ?: return null
                    val id = o.optString("id").ifBlank { "director-$i" }
                    val text = o.optString("text")
                    val presetId = o.optString("presetId")
                    if (text.isEmpty() || presetId.isBlank()) return null
                    add(
                        DirectorDraftSegment(
                            id = id,
                            text = text,
                            presetId = presetId,
                            warnings = o.optJSONArray("warnings").toStringList()
                        )
                    )
                }
            }
            DirectorDraft(
                voiceProfileId = voiceProfileId,
                segments = segments,
                warnings = root.optJSONArray("warnings").toStringList()
            )
        }.getOrNull()
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val v = optString(i)
                if (v.isNotEmpty()) add(v)
            }
        }
    }
}
