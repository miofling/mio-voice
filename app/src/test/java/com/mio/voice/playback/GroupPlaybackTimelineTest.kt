package com.mio.voice.playback

import com.mio.voice.data.generation.GeneratedAudioRecord
import com.mio.voice.data.generation.GenerationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GroupPlaybackTimelineTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun convertsBetweenGlobalAndSegmentPositions() {
        val timeline = GroupPlaybackTimeline(listOf(1_000L, 2_000L, 500L))

        assertEquals(3_250L, timeline.toGlobalPosition(2, 250L))
        assertEquals(SegmentTimePosition(1, 500L), timeline.toSegmentPosition(1_500L))
        assertEquals(SegmentTimePosition(2, 500L), timeline.toSegmentPosition(9_999L))
        assertEquals(3_500L, timeline.totalDurationMs)
    }

    @Test
    fun seekOffsetsCrossSegmentBoundariesInBothDirections() {
        val timeline = GroupPlaybackTimeline(listOf(4_000L, 4_000L, 4_000L))
        val start = timeline.toGlobalPosition(0, 3_500L)

        assertEquals(SegmentTimePosition(2, 500L), timeline.toSegmentPosition(start + 5_000L))
        assertEquals(SegmentTimePosition(0, 0L), timeline.toSegmentPosition(start - 5_000L))
        assertEquals(SegmentTimePosition(2, 4_000L), timeline.toSegmentPosition(start + 50_000L))
    }

    @Test
    fun playbackPlanOrdersSingleAndMultipleSegments() {
        val first = temporaryFolder.newFile("first.wav").apply { writeBytes(byteArrayOf(1)) }
        val second = temporaryFolder.newFile("second.wav").apply { writeBytes(byteArrayOf(2)) }

        val single = GenerationGroupPlaybackPlan.create("single", listOf(record("a", 0, first.path, 100L)))
        val multiple = GenerationGroupPlaybackPlan.create(
            "multiple",
            listOf(record("b", 1, second.path, 200L), record("a", 0, first.path, 100L))
        )

        assertEquals(listOf("a"), single.entries.map { it.recordId })
        assertEquals(listOf("a", "b"), multiple.entries.map { it.recordId })
        assertEquals(300L, multiple.timeline.totalDurationMs)
    }

    @Test
    fun playbackPlanRejectsMissingFiles() {
        val error = runCatching {
            GenerationGroupPlaybackPlan.create(
                "missing",
                listOf(record("missing", 0, temporaryFolder.root.resolve("missing.wav").path, 100L))
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    private fun record(id: String, index: Int, path: String, durationMs: Long) = GeneratedAudioRecord(
        id = id,
        text = id,
        generationGroupId = "group",
        segmentIndex = index,
        segmentText = id,
        localAudioPath = path,
        durationMs = durationMs,
        voiceId = "voice",
        voiceName = "Mio",
        emotion = "neutral",
        speed = 1f,
        format = "wav",
        createdAt = index.toLong(),
        status = GenerationStatus.Success
    )
}
