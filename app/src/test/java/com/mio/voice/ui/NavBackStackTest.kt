package com.mio.voice.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavBackStackTest {

    @Test
    fun emptyStackMeansMainScreen() {
        assertNull(NavBackStack.currentScreen(emptyList()))
        assertNull(NavBackStack.currentDetailGroupId(emptyList()))
    }

    @Test
    fun historyRouteResolvesToHistoryScreen() {
        val stack = listOf(NavBackStack.historyRoute())
        assertEquals(NavBackStack.SCREEN_HISTORY, NavBackStack.currentScreen(stack))
        assertNull(NavBackStack.currentDetailGroupId(stack))
    }

    @Test
    fun detailRouteCarriesGroupIdOnly() {
        val stack = listOf(NavBackStack.detailRoute("group-42"))
        assertEquals(NavBackStack.SCREEN_DETAIL, NavBackStack.currentScreen(stack))
        assertEquals("group-42", NavBackStack.currentDetailGroupId(stack))
    }

    // 场景一：首页 → 历史 → 返回 → 首页（不退出 App）
    @Test
    fun homeToHistoryThenBackReturnsToHome() {
        var stack = listOf(NavBackStack.historyRoute())
        val result = NavBackStack.pop(stack)
        stack = result.stack
        assertTrue(result.handled)
        assertNull(NavBackStack.currentScreen(stack)) // 回到主页面
    }

    // 场景二：首页 → 历史 → 详情 → 返回 → 历史 → 返回 → 首页
    @Test
    fun homeToHistoryToDetailBacksOutOneLevelEachTime() {
        var stack = listOf(
            NavBackStack.historyRoute(),
            NavBackStack.detailRoute("g1")
        )
        // 详情 → 历史
        var result = NavBackStack.pop(stack)
        stack = result.stack
        assertTrue(result.handled)
        assertEquals(NavBackStack.SCREEN_HISTORY, NavBackStack.currentScreen(stack))

        // 历史 → 首页
        result = NavBackStack.pop(stack)
        stack = result.stack
        assertTrue(result.handled)
        assertNull(NavBackStack.currentScreen(stack))
    }

    // 场景三：首页 → 详情（最近生成直接进入）→ 返回 → 首页
    @Test
    fun homeToDetailThenBackReturnsToHome() {
        var stack = listOf(NavBackStack.detailRoute("g2"))
        val result = NavBackStack.pop(stack)
        stack = result.stack
        assertTrue(result.handled)
        assertNull(NavBackStack.currentScreen(stack))
    }

    @Test
    fun settingsTtsRouteResolvesToTtsScreen() {
        val stack = listOf(NavBackStack.settingsTtsRoute())
        assertEquals(NavBackStack.SCREEN_SETTINGS_TTS, NavBackStack.currentScreen(stack))
        assertNull(NavBackStack.currentDetailGroupId(stack))
    }

    @Test
    fun settingsAiRouteResolvesToAiScreen() {
        val stack = listOf(NavBackStack.settingsAiRoute())
        assertEquals(NavBackStack.SCREEN_SETTINGS_AI, NavBackStack.currentScreen(stack))
        assertNull(NavBackStack.currentDetailGroupId(stack))
    }

    @Test
    fun settingsPromptRouteResolvesToPromptScreen() {
        val stack = listOf(NavBackStack.settingsPromptRoute())
        assertEquals(NavBackStack.SCREEN_SETTINGS_PROMPT, NavBackStack.currentScreen(stack))
        assertNull(NavBackStack.currentDetailGroupId(stack))
    }

    // 设置主页 → TTS 子页 → 返回 → 设置主页（不退出 App）
    @Test
    fun settingsSubPageBackReturnsToMainScreen() {
        var stack = listOf(NavBackStack.settingsTtsRoute())
        val result = NavBackStack.pop(stack)
        stack = result.stack
        assertTrue(result.handled)
        assertNull(NavBackStack.currentScreen(stack)) // 回到主页面（底部导航“设置”仍选中）
    }

    // 已在主页面（空栈）时返回键不被处理，交回系统默认逻辑退出 App。
    @Test
    fun popOnMainScreenIsNotHandled() {
        val result = NavBackStack.pop(emptyList())
        assertFalse(result.handled)
        assertTrue(result.stack.isEmpty())
    }

    // ---- 音色库三级结构路由 ----

    @Test
    fun voiceDetailRouteCarriesVoiceProfileId() {
        val stack = listOf(NavBackStack.voiceDetailRoute("voice-1"))
        assertEquals(NavBackStack.SCREEN_VOICE_DETAIL, NavBackStack.currentScreen(stack))
        assertEquals("voice-1", NavBackStack.currentVoiceProfileId(stack))
        assertNull(NavBackStack.currentPresetId(stack))
        assertNull(NavBackStack.currentDetailGroupId(stack))
    }

    @Test
    fun presetNewRouteCarriesVoiceProfileIdOnly() {
        val stack = listOf(NavBackStack.presetNewRoute("voice-2"))
        assertEquals(NavBackStack.SCREEN_PRESET_NEW, NavBackStack.currentScreen(stack))
        assertEquals("voice-2", NavBackStack.currentVoiceProfileId(stack))
        assertNull(NavBackStack.currentPresetId(stack))
    }

    @Test
    fun presetEditRouteCarriesVoiceProfileIdAndPresetId() {
        val stack = listOf(NavBackStack.presetEditRoute("voice-3", "preset-9"))
        assertEquals(NavBackStack.SCREEN_PRESET_EDIT, NavBackStack.currentScreen(stack))
        assertEquals("voice-3", NavBackStack.currentVoiceProfileId(stack))
        assertEquals("preset-9", NavBackStack.currentPresetId(stack))
    }

    @Test
    fun currentVoiceProfileIdIsNullForUnrelatedRoutes() {
        assertNull(NavBackStack.currentVoiceProfileId(listOf(NavBackStack.historyRoute())))
        assertNull(NavBackStack.currentVoiceProfileId(listOf(NavBackStack.detailRoute("g1"))))
        assertNull(NavBackStack.currentPresetId(listOf(NavBackStack.presetNewRoute("voice-x"))))
    }

    @Test
    fun voiceNewRouteHasNoVoiceProfileId() {
        val stack = listOf(NavBackStack.voiceNewRoute())
        assertEquals(NavBackStack.SCREEN_VOICE_NEW, NavBackStack.currentScreen(stack))
        assertNull(NavBackStack.currentVoiceProfileId(stack))
        assertNull(NavBackStack.currentPresetId(stack))
    }

    @Test
    fun voiceEditRouteCarriesVoiceProfileId() {
        val stack = listOf(NavBackStack.voiceEditRoute("voice-5"))
        assertEquals(NavBackStack.SCREEN_VOICE_EDIT, NavBackStack.currentScreen(stack))
        assertEquals("voice-5", NavBackStack.currentVoiceProfileId(stack))
        assertNull(NavBackStack.currentPresetId(stack))
    }

    // 详情 → 编辑音色 → 返回 → 详情（不退出 App）
    @Test
    fun voiceDetailToVoiceEditBacksOutToDetail() {
        var stack = listOf(
            NavBackStack.voiceDetailRoute("voice-1"),
            NavBackStack.voiceEditRoute("voice-1")
        )
        val result = NavBackStack.pop(stack)
        stack = result.stack
        assertTrue(result.handled)
        assertEquals(NavBackStack.SCREEN_VOICE_DETAIL, NavBackStack.currentScreen(stack))
        assertEquals("voice-1", NavBackStack.currentVoiceProfileId(stack))
    }

    // 音色库 → 详情 → 新增预设 → 逐级返回，不退出 App。
    @Test
    fun voiceDetailToPresetNewBacksOutOneLevelEachTime() {
        var stack = listOf(
            NavBackStack.voiceDetailRoute("voice-1"),
            NavBackStack.presetNewRoute("voice-1")
        )
        // 新增预设 → 详情
        var result = NavBackStack.pop(stack)
        stack = result.stack
        assertTrue(result.handled)
        assertEquals(NavBackStack.SCREEN_VOICE_DETAIL, NavBackStack.currentScreen(stack))

        // 详情 → 主页面
        result = NavBackStack.pop(stack)
        stack = result.stack
        assertTrue(result.handled)
        assertNull(NavBackStack.currentScreen(stack))
    }

    @Test
    fun collectionDetailRouteCarriesCollectionId() {
        val stack = listOf(NavBackStack.collectionDetailRoute("col-7"))
        assertEquals(NavBackStack.SCREEN_COLLECTION_DETAIL, NavBackStack.currentScreen(stack))
        assertEquals("col-7", NavBackStack.currentCollectionId(stack))
    }

    @Test
    fun currentCollectionIdNullForOtherRoutes() {
        assertNull(NavBackStack.currentCollectionId(emptyList()))
        assertNull(NavBackStack.currentCollectionId(listOf(NavBackStack.detailRoute("g1"))))
        assertNull(NavBackStack.currentCollectionId(listOf(NavBackStack.historyRoute())))
    }

    @Test
    fun collectionPickerRouteCarriesCollectionId() {
        val stack = listOf(NavBackStack.collectionPickerRoute("col-9"))
        assertEquals(NavBackStack.SCREEN_COLLECTION_PICKER, NavBackStack.currentScreen(stack))
        assertEquals("col-9", NavBackStack.currentCollectionPickerId(stack))
        // picker 路由不应被误判为组详情。
        assertNull(NavBackStack.currentCollectionId(stack))
    }

    @Test
    fun currentCollectionPickerIdNullForOtherRoutes() {
        assertNull(NavBackStack.currentCollectionPickerId(emptyList()))
        assertNull(NavBackStack.currentCollectionPickerId(listOf(NavBackStack.collectionDetailRoute("c1"))))
    }
}
