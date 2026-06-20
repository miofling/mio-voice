package com.mio.voice.playback

import com.mio.voice.data.generation.GeneratedAudioRecord
import java.io.File

data class SegmentTimePosition(
    val segmentIndex: Int,
    val positionMs: Long
)

class GroupPlaybackTimeline(durationsMs: List<Long>) {
    val segmentDurationsMs: List<Long> = durationsMs.map { it.coerceAtLeast(0L) }
    val totalDurationMs: Long = segmentDurationsMs.sum()

    init {
        require(segmentDurationsMs.isNotEmpty()) { "播放时间轴不能为空。" }
    }

    fun toGlobalPosition(segmentIndex: Int, segmentPositionMs: Long): Long {
        require(segmentIndex in segmentDurationsMs.indices) { "分段序号超出时间轴范围。" }
        val before = segmentDurationsMs.take(segmentIndex).sum()
        return before + segmentPositionMs.coerceIn(0L, segmentDurationsMs[segmentIndex])
    }

    fun toSegmentPosition(globalPositionMs: Long): SegmentTimePosition {
        val target = globalPositionMs.coerceIn(0L, totalDurationMs)
        if (target == totalDurationMs) {
            return SegmentTimePosition(segmentDurationsMs.lastIndex, segmentDurationsMs.last())
        }
        var remaining = target
        segmentDurationsMs.forEachIndexed { index, duration ->
            if (remaining < duration) return SegmentTimePosition(index, remaining)
            remaining -= duration
        }
        return SegmentTimePosition(segmentDurationsMs.lastIndex, segmentDurationsMs.last())
    }
}

data class GroupPlaybackEntry(
    val recordId: String,
    val text: String,
    val file: File,
    val durationMs: Long,
    val segmentIndex: Int
)

data class GenerationGroupPlaybackPlan(
    val groupId: String,
    val entries: List<GroupPlaybackEntry>,
    val timeline: GroupPlaybackTimeline
) {
    companion object {
        fun create(groupId: String, segments: List<GeneratedAudioRecord>): GenerationGroupPlaybackPlan {
            require(segments.isNotEmpty()) { "生成组没有可播放分段。" }
            val ordered = segments.sortedBy { it.segmentIndex }
            require(ordered.map { it.segmentIndex } == ordered.indices.toList()) {
                "生成组分段顺序不连续。"
            }
            val entries = ordered.map { segment ->
                val file = File(segment.localAudioPath)
                require(file.isFile && file.length() > 0L) {
                    "本地音频文件不存在：${file.name}"
                }
                GroupPlaybackEntry(
                    recordId = segment.id,
                    text = segment.segmentText ?: segment.text,
                    file = file,
                    durationMs = segment.durationMs,
                    segmentIndex = segment.segmentIndex
                )
            }
            return GenerationGroupPlaybackPlan(
                groupId = groupId,
                entries = entries,
                timeline = GroupPlaybackTimeline(entries.map { it.durationMs })
            )
        }
    }
}
