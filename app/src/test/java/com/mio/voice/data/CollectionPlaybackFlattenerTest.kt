package com.mio.voice.data

import com.mio.voice.data.generation.CollectionPlaybackFlattener
import com.mio.voice.data.generation.GeneratedAudioRecord
import com.mio.voice.data.generation.GenerationStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionPlaybackFlattenerTest {

    private fun rec(id: String, groupId: String, index: Int): GeneratedAudioRecord =
        GeneratedAudioRecord(
            id = id,
            text = "t-$id",
            generationGroupId = groupId,
            segmentIndex = index,
            segmentText = null,
            localAudioPath = "/audio/$id.wav",
            durationMs = 1000L,
            voiceId = "v",
            voiceName = "V",
            emotion = null,
            speed = 1.0f,
            format = "wav",
            createdAt = 0L,
            status = GenerationStatus.Success
        )

    @Test
    fun multipleMembersReindexedContiguously() {
        val a = listOf(rec("a0", "A", 0), rec("a1", "A", 1))
        val b = listOf(rec("b0", "B", 0))
        val result = CollectionPlaybackFlattener.flatten(listOf(a, b))

        assertEquals(0, result.missingCount)
        assertEquals(listOf(0, 1, 2), result.segments.map { it.segmentIndex })
        // 拼接顺序保留：A 的两段在前，B 的一段在后。
        assertEquals(listOf("a0", "a1", "b0"), result.segments.map { it.id })
    }

    @Test
    fun memberInternalSegmentsSortedByOriginalIndex() {
        val a = listOf(rec("a1", "A", 1), rec("a0", "A", 0))
        val result = CollectionPlaybackFlattener.flatten(listOf(a))
        assertEquals(listOf("a0", "a1"), result.segments.map { it.id })
        assertEquals(listOf(0, 1), result.segments.map { it.segmentIndex })
    }

    @Test
    fun emptyMembersCountedAsMissingAndSkipped() {
        val a = listOf(rec("a0", "A", 0))
        val result = CollectionPlaybackFlattener.flatten(listOf(a, emptyList(), emptyList()))
        assertEquals(2, result.missingCount)
        assertEquals(1, result.segments.size)
        assertEquals(listOf(0), result.segments.map { it.segmentIndex })
    }

    @Test
    fun allEmptyYieldsEmptyResult() {
        val result = CollectionPlaybackFlattener.flatten(listOf(emptyList(), emptyList()))
        assertEquals(2, result.missingCount)
        assertEquals(0, result.segments.size)
    }

    @Test
    fun noMembersYieldsEmptyResult() {
        val result = CollectionPlaybackFlattener.flatten(emptyList())
        assertEquals(0, result.missingCount)
        assertEquals(0, result.segments.size)
    }
}
