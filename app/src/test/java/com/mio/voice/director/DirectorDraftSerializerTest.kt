package com.mio.voice.director

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DirectorDraftSerializerTest {

    @Test
    fun roundTripPreservesAllFields() {
        val draft = DirectorDraft(
            voiceProfileId = "voice-1",
            segments = listOf(
                DirectorDraftSegment("director-0", "开心。", "happy", warnings = emptyList()),
                DirectorDraftSegment("director-1", "难过。", "sad", warnings = listOf("回退到默认预设。"))
            ),
            warnings = listOf("整体警告")
        )

        val restored = DirectorDraftSerializer.deserialize(DirectorDraftSerializer.serialize(draft))

        assertEquals(draft, restored)
    }

    @Test
    fun blankOrNullReturnsNull() {
        assertNull(DirectorDraftSerializer.deserialize(null))
        assertNull(DirectorDraftSerializer.deserialize(""))
        assertNull(DirectorDraftSerializer.deserialize("   "))
    }

    @Test
    fun malformedJsonReturnsNull() {
        assertNull(DirectorDraftSerializer.deserialize("not json"))
        assertNull(DirectorDraftSerializer.deserialize("""{"segments":"oops"}"""))
    }

    @Test
    fun emptySegmentsReturnsNull() {
        assertNull(DirectorDraftSerializer.deserialize("""{"voiceProfileId":"v","segments":[]}"""))
    }

    @Test
    fun missingVoiceProfileIdReturnsNull() {
        assertNull(
            DirectorDraftSerializer.deserialize(
                """{"segments":[{"id":"director-0","text":"x","presetId":"happy"}]}"""
            )
        )
    }

    @Test
    fun segmentWithoutTextOrPresetReturnsNull() {
        assertNull(
            DirectorDraftSerializer.deserialize(
                """{"voiceProfileId":"v","segments":[{"id":"director-0","text":"","presetId":"happy"}]}"""
            )
        )
        assertNull(
            DirectorDraftSerializer.deserialize(
                """{"voiceProfileId":"v","segments":[{"id":"director-0","text":"x","presetId":""}]}"""
            )
        )
    }
}
