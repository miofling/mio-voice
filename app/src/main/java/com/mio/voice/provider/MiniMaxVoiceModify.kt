package com.mio.voice.provider

import org.json.JSONObject

/**
 * MiniMax T2A v2 顶层 `voice_modify`（声音效果器）的纯逻辑封装。
 *
 * 这是 MiniMax 独有、通用 TTS 没有对应概念的能力，因此存进预设的 provider 私有袋
 * `EmotionPreset.providerExtras`，key 统一加 `minimax.voice_modify.` 前缀，其他 provider
 * 读不懂就忽略，不污染通用层（emotion/speed/pitch）。
 *
 * 与 `voice_setting.pitch`(±12 语调微调) 完全不同：这里的 pitch 是 ±100 的变声器式音色改造。
 *
 * 设计约定：**仅在非默认时写键**——三个数值为 0、音效为 null 时不写对应键，
 * 从而让未调过的预设 providerExtras 保持空、缓存键不变、序列化不膨胀。
 */
object MiniMaxVoiceModify {
    const val KEY_PITCH = "minimax.voice_modify.pitch"
    const val KEY_INTENSITY = "minimax.voice_modify.intensity"
    const val KEY_TIMBRE = "minimax.voice_modify.timbre"
    const val KEY_SOUND_EFFECTS = "minimax.voice_modify.sound_effects"

    const val VALUE_MIN = -100
    const val VALUE_MAX = 100

    /** 官方合法的 sound_effects 枚举值。 */
    val ALLOWED_SOUND_EFFECTS = setOf(
        "spacious_echo",   // 空旷回响
        "auditorium_echo", // 礼堂广播
        "lofi_telephone",  // 电话失真
        "robotic"          // 电音机器人
    )

    /** UI 回填用的解包结果；数值缺省 0，音效缺省 null。 */
    data class VoiceModifyValues(
        val pitch: Int = 0,
        val intensity: Int = 0,
        val timbre: Int = 0,
        val soundEffect: String? = null
    )

    /**
     * 把 UI 上的四项折成私有袋键值对。仅写入非默认项：
     * - 数值先 coerce 到 [-100,100]，为 0 不写。
     * - soundEffect 非法或 null 不写。
     */
    fun toExtras(pitch: Int, intensity: Int, timbre: Int, soundEffect: String?): Map<String, String> {
        val result = mutableMapOf<String, String>()
        coerce(pitch).takeIf { it != 0 }?.let { result[KEY_PITCH] = it.toString() }
        coerce(intensity).takeIf { it != 0 }?.let { result[KEY_INTENSITY] = it.toString() }
        coerce(timbre).takeIf { it != 0 }?.let { result[KEY_TIMBRE] = it.toString() }
        soundEffect?.takeIf { it in ALLOWED_SOUND_EFFECTS }?.let { result[KEY_SOUND_EFFECTS] = it }
        return result
    }

    /** 从私有袋解包出四项，供 UI 初始回填。非法/缺失项回退默认。 */
    fun fromExtras(extras: Map<String, String>): VoiceModifyValues =
        VoiceModifyValues(
            pitch = extras[KEY_PITCH]?.toIntOrNull()?.let(::coerce) ?: 0,
            intensity = extras[KEY_INTENSITY]?.toIntOrNull()?.let(::coerce) ?: 0,
            timbre = extras[KEY_TIMBRE]?.toIntOrNull()?.let(::coerce) ?: 0,
            soundEffect = extras[KEY_SOUND_EFFECTS]?.takeIf { it in ALLOWED_SOUND_EFFECTS }
        )

    /**
     * 给 provider 用：把私有袋里的 voice_modify 键拼成 JSON 顶层对象。
     * 全默认（无任何有效键）→ 返回 null（调用方据此不发 voice_modify 字段）。
     */
    fun buildJson(extras: Map<String, String>): JSONObject? {
        val values = fromExtras(extras)
        if (values.pitch == 0 && values.intensity == 0 && values.timbre == 0 && values.soundEffect == null) {
            return null
        }
        val json = JSONObject()
        if (values.pitch != 0) json.put("pitch", values.pitch)
        if (values.intensity != 0) json.put("intensity", values.intensity)
        if (values.timbre != 0) json.put("timbre", values.timbre)
        values.soundEffect?.let { json.put("sound_effects", it) }
        return json
    }

    private fun coerce(value: Int): Int = value.coerceIn(VALUE_MIN, VALUE_MAX)
}
