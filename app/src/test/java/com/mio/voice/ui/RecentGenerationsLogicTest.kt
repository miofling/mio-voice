package com.mio.voice.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentGenerationsLogicTest {

    private val ids = listOf("a", "b", "c")

    // ---- 展开项选择 ----

    @Test
    fun defaultsToMostRecentWhenNothingPlayingOrSelected() {
        // 验收点 2：默认展开最近一条。
        assertEquals("a", RecentGenerationsLogic.resolveExpandedId(ids, null, null))
    }

    @Test
    fun playingGroupTakesPriority() {
        // 验收点：正在播放的记录优先展开。
        assertEquals("c", RecentGenerationsLogic.resolveExpandedId(ids, playingGroupId = "c", previousExpandedId = "a"))
    }

    @Test
    fun keepsUserSelectionWhenNotPlaying() {
        // 验收点 3：用户切换展开项后保持（列表刷新不丢失展开组）。
        assertEquals("b", RecentGenerationsLogic.resolveExpandedId(ids, playingGroupId = null, previousExpandedId = "b"))
    }

    @Test
    fun fallsBackToFirstWhenPreviousExpandedRemoved() {
        // 删除当前展开项后回退到最近一条。
        assertEquals("a", RecentGenerationsLogic.resolveExpandedId(ids, playingGroupId = null, previousExpandedId = "deleted"))
    }

    @Test
    fun emptyListHasNoExpansion() {
        // 验收点 11：无历史记录时空状态。
        assertNull(RecentGenerationsLogic.resolveExpandedId(emptyList(), "x", "y"))
    }

    @Test
    fun ignoresPlayingGroupNotInList() {
        assertEquals("b", RecentGenerationsLogic.resolveExpandedId(ids, playingGroupId = "z", previousExpandedId = "b"))
    }

    // ---- Slider 全局进度换算 ----

    @Test
    fun fractionConvertsToPositionAndBack() {
        // 验收点 5：Slider 全局进度换算正确。
        assertEquals(45_000L, RecentGenerationsLogic.fractionToPositionMs(0.5f, 90_000L))
        assertEquals(0.5f, RecentGenerationsLogic.positionToFraction(45_000L, 90_000L), 0.0001f)
    }

    @Test
    fun fractionClampsToRange() {
        assertEquals(90_000L, RecentGenerationsLogic.fractionToPositionMs(2f, 90_000L))
        assertEquals(0L, RecentGenerationsLogic.fractionToPositionMs(-1f, 90_000L))
        assertEquals(0f, RecentGenerationsLogic.positionToFraction(45_000L, 0L), 0.0001f)
    }

    // ---- 时间格式化 ----

    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("00:18", RecentGenerationsLogic.formatDuration(18_000L))
        assertEquals("01:26", RecentGenerationsLogic.formatDuration(86_000L))
    }

    @Test
    fun formatsHoursWhenOverOneHour() {
        assertEquals("1:01:05", RecentGenerationsLogic.formatDuration(3_665_000L))
    }

    @Test
    fun relativeTimeBuckets() {
        val now = 1_000_000_000_000L
        assertEquals("刚刚", RecentGenerationsLogic.relativeTime(now - 30_000L, now))
        assertEquals("5分钟前", RecentGenerationsLogic.relativeTime(now - 5 * 60_000L, now))
        assertEquals("3小时前", RecentGenerationsLogic.relativeTime(now - 3 * 3_600_000L, now))
        assertEquals("2天前", RecentGenerationsLogic.relativeTime(now - 2 * 86_400_000L, now))
    }
}
