package com.mio.voice.ui

/**
 * 轻量顶层返回栈的纯逻辑（不依赖 Compose），便于单测覆盖返回行为。
 *
 * 约定：
 * - 空栈 = 主页面（首页 / 底部导航）。
 * - 栈顶 = 当前显示的子页面，路由为 "history" 或 "detail/{groupId}"。
 * - [pop] 返回 false 表示已在主页面，应交回系统默认逻辑退出 App。
 */
object NavBackStack {
    const val SCREEN_HISTORY = "history"
    const val SCREEN_DETAIL = "detail"
    const val SCREEN_SETTINGS_TTS = "settings_tts"
    const val SCREEN_SETTINGS_AI = "settings_ai"
    const val SCREEN_SETTINGS_PROMPT = "settings_prompt"

    // 关于页面及其本地子页（开源许可证 / 隐私说明）。无参数，路由即页面类型。
    const val SCREEN_ABOUT = "about"
    const val SCREEN_ABOUT_LICENSE = "about_license"
    const val SCREEN_ABOUT_PRIVACY = "about_privacy"

    // 音色库三级结构：音色详情（音色子页）/ 新增预设占位页 / 编辑预设占位页。
    // voiceProfileId 与 presetId 均为不含 '/' 的 UUID，可安全拼进路由字符串。
    const val SCREEN_VOICE_DETAIL = "voice_detail"
    const val SCREEN_VOICE_NEW = "voice_new"
    const val SCREEN_VOICE_CLONE = "voice_clone"
    const val SCREEN_VOICE_EDIT = "voice_edit"
    const val SCREEN_PRESET_NEW = "preset_new"
    const val SCREEN_PRESET_EDIT = "preset_edit"

    // 语音库「组」详情子页：collection_detail/{collectionId}，collectionId 为不含 '/' 的 UUID。
    const val SCREEN_COLLECTION_DETAIL = "collection_detail"

    // 组内「从历史选记录加入」选择页：collection_picker/{collectionId}。
    const val SCREEN_COLLECTION_PICKER = "collection_picker"

    fun historyRoute(): String = SCREEN_HISTORY

    fun detailRoute(groupId: String): String = "$SCREEN_DETAIL/$groupId"

    fun settingsTtsRoute(): String = SCREEN_SETTINGS_TTS

    fun settingsAiRoute(): String = SCREEN_SETTINGS_AI

    fun settingsPromptRoute(): String = SCREEN_SETTINGS_PROMPT

    fun aboutRoute(): String = SCREEN_ABOUT

    fun aboutLicenseRoute(): String = SCREEN_ABOUT_LICENSE

    fun aboutPrivacyRoute(): String = SCREEN_ABOUT_PRIVACY

    fun voiceDetailRoute(voiceProfileId: String): String = "$SCREEN_VOICE_DETAIL/$voiceProfileId"

    fun voiceNewRoute(): String = SCREEN_VOICE_NEW

    fun voiceCloneRoute(): String = SCREEN_VOICE_CLONE

    fun voiceEditRoute(voiceProfileId: String): String = "$SCREEN_VOICE_EDIT/$voiceProfileId"

    fun presetNewRoute(voiceProfileId: String): String = "$SCREEN_PRESET_NEW/$voiceProfileId"

    fun presetEditRoute(voiceProfileId: String, presetId: String): String =
        "$SCREEN_PRESET_EDIT/$voiceProfileId/$presetId"

    fun collectionDetailRoute(collectionId: String): String = "$SCREEN_COLLECTION_DETAIL/$collectionId"

    fun collectionPickerRoute(collectionId: String): String = "$SCREEN_COLLECTION_PICKER/$collectionId"

    /** 当前栈顶页面类型；空栈返回 null（代表主页面）。 */
    fun currentScreen(stack: List<String>): String? =
        stack.lastOrNull()?.substringBefore('/')

    /** 从栈顶详情路由解析 groupId；非详情或为空返回 null。 */
    fun currentDetailGroupId(stack: List<String>): String? {
        val top = stack.lastOrNull() ?: return null
        if (top.substringBefore('/') != SCREEN_DETAIL) return null
        return top.substringAfter('/').ifBlank { null }
    }

    /** 从栈顶组详情路由解析 collectionId；非组详情或为空返回 null。 */
    fun currentCollectionId(stack: List<String>): String? {
        val top = stack.lastOrNull() ?: return null
        if (top.substringBefore('/') != SCREEN_COLLECTION_DETAIL) return null
        return top.substringAfter('/').ifBlank { null }
    }

    /** 从栈顶选记录页路由解析 collectionId；非该页或为空返回 null。 */
    fun currentCollectionPickerId(stack: List<String>): String? {
        val top = stack.lastOrNull() ?: return null
        if (top.substringBefore('/') != SCREEN_COLLECTION_PICKER) return null
        return top.substringAfter('/').ifBlank { null }
    }

    /**
     * 从栈顶音色库路由解析 voiceProfileId（voice_detail / preset_new / preset_edit 的第一个参数段）。
     * 非这三类或为空返回 null。
     */
    fun currentVoiceProfileId(stack: List<String>): String? {
        val top = stack.lastOrNull() ?: return null
        val screen = top.substringBefore('/')
        if (screen != SCREEN_VOICE_DETAIL &&
            screen != SCREEN_VOICE_EDIT &&
            screen != SCREEN_PRESET_NEW &&
            screen != SCREEN_PRESET_EDIT
        ) {
            return null
        }
        return top.substringAfter('/').substringBefore('/').ifBlank { null }
    }

    /** 仅从 preset_edit 路由解析 presetId（第二个参数段）；其它路由或为空返回 null。 */
    fun currentPresetId(stack: List<String>): String? {
        val top = stack.lastOrNull() ?: return null
        if (top.substringBefore('/') != SCREEN_PRESET_EDIT) return null
        // 形如 preset_edit/{voiceProfileId}/{presetId}
        val afterScreen = top.substringAfter('/')           // {voiceProfileId}/{presetId}
        if (!afterScreen.contains('/')) return null
        return afterScreen.substringAfter('/').ifBlank { null }
    }

    /**
     * 出栈一层。返回新栈与是否处理了返回：
     * - 已在主页面（空栈）→ handled = false（系统按默认逻辑退出 App）。
     * - 否则移除栈顶 → handled = true。
     */
    fun pop(stack: List<String>): PopResult =
        if (stack.isEmpty()) PopResult(stack, handled = false)
        else PopResult(stack.dropLast(1), handled = true)

    data class PopResult(val stack: List<String>, val handled: Boolean)
}
