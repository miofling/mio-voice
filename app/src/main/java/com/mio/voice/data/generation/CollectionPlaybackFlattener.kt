package com.mio.voice.data.generation

/**
 * 把组内多条历史记录的分段拼接成一条可连续播放的扁平列表（纯逻辑，便于单测）。
 *
 * 组内连续播放复用现有 [com.mio.voice.playback.GenerationGroupPlaybackPlan]，
 * 而后者要求 segment_index 必须是连续的 0..N-1，因此这里在拼接时**重排** segment_index。
 */
object CollectionPlaybackFlattener {

    data class Result(
        /** 重排 segment_index 为 0..N-1 的扁平分段列表，按成员顺序拼接。 */
        val segments: List<GeneratedAudioRecord>,
        /** 因无有效分段而被跳过的成员数量（文件丢失 / 失败 / 空）。 */
        val missingCount: Int
    )

    /**
     * @param memberSegments 各成员（按组内顺序）的**已过滤有效**分段列表；
     *                       无有效分段的成员请传入空列表（会被计入 missingCount）。
     */
    fun flatten(memberSegments: List<List<GeneratedAudioRecord>>): Result {
        val out = ArrayList<GeneratedAudioRecord>()
        var missing = 0
        var runningIndex = 0
        for (segments in memberSegments) {
            if (segments.isEmpty()) {
                missing++
                continue
            }
            // 成员内部按原 segment_index 排序后追加，整体重排为连续序号。
            segments.sortedBy { it.segmentIndex }.forEach { segment ->
                out += segment.copy(segmentIndex = runningIndex)
                runningIndex++
            }
        }
        return Result(segments = out, missingCount = missing)
    }
}
