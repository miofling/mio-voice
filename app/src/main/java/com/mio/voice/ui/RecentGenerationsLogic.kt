package com.mio.voice.ui

/**
 * 纯逻辑工具：最近生成模块的展开项选择、整组进度换算、时间格式化。
 *
 * 抽成无 Android 依赖的对象，便于 JVM 单元测试覆盖，UI 与 ViewModel 仅做调用。
 */
object RecentGenerationsLogic {

    /**
     * 根据最新的最近生成列表、当前播放组、以及用户上一次的展开项，决定应展开的记录。
     *
     * 规则（优先级从高到低）：
     * 1. 正在播放的记录若在列表中，则优先展开；
     * 2. 否则若用户之前选择的展开项仍在列表中，保持不变；
     * 3. 否则默认展开列表第一条（最近一条）。
     */
    fun resolveExpandedId(
        recentIds: List<String>,
        playingGroupId: String?,
        previousExpandedId: String?
    ): String? {
        if (recentIds.isEmpty()) return null
        if (playingGroupId != null && playingGroupId in recentIds) return playingGroupId
        if (previousExpandedId != null && previousExpandedId in recentIds) return previousExpandedId
        return recentIds.first()
    }

    /**
     * Slider 拖动比例（0f..1f）换算为整组毫秒位置。
     */
    fun fractionToPositionMs(fraction: Float, durationMs: Long): Long {
        if (durationMs <= 0L) return 0L
        val clamped = fraction.coerceIn(0f, 1f)
        return (clamped * durationMs).toLong().coerceIn(0L, durationMs)
    }

    /**
     * 整组当前位置换算为 Slider 比例（0f..1f）。
     */
    fun positionToFraction(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L) return 0f
        return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * 毫秒格式化为 mm:ss，超过一小时显示 H:mm:ss。
     */
    fun formatDuration(positionMs: Long): String {
        val totalSeconds = (positionMs.coerceAtLeast(0L)) / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    /**
     * 相对时间描述：刚刚 / N 分钟前 / N 小时前 / N 天前 / 更久。
     */
    fun relativeTime(createdAtMs: Long, nowMs: Long): String {
        val diff = (nowMs - createdAtMs).coerceAtLeast(0L)
        val minutes = diff / 60_000
        val hours = diff / 3_600_000
        val days = diff / 86_400_000
        return when {
            minutes < 1 -> "刚刚"
            minutes < 60 -> "${minutes}分钟前"
            hours < 24 -> "${hours}小时前"
            days < 30 -> "${days}天前"
            else -> "更早"
        }
    }
}
