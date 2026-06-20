package com.mio.voice.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniMaxVoiceModifyTest {
    @Test
    fun toExtrasAllDefaultsYieldsEmptyMap() {
        assertTrue(MiniMaxVoiceModify.toExtras(0, 0, 0, null).isEmpty())
    }

    @Test
    fun toExtrasOnlyWritesNonDefaultKeys() {
        val extras = MiniMaxVoiceModify.toExtras(30, 0, -40, "lofi_telephone")
        assertEquals("30", extras[MiniMaxVoiceModify.KEY_PITCH])
        assertEquals("-40", extras[MiniMaxVoiceModify.KEY_TIMBRE])
        assertEquals("lofi_telephone", extras[MiniMaxVoiceModify.KEY_SOUND_EFFECTS])
        // intensity 为 0 → 不写键
        assertFalse(extras.containsKey(MiniMaxVoiceModify.KEY_INTENSITY))
        assertEquals(3, extras.size)
    }

    @Test
    fun toExtrasCoercesOutOfRange() {
        val extras = MiniMaxVoiceModify.toExtras(999, -999, 0, null)
        assertEquals("100", extras[MiniMaxVoiceModify.KEY_PITCH])
        assertEquals("-100", extras[MiniMaxVoiceModify.KEY_INTENSITY])
    }

    @Test
    fun toExtrasDropsIllegalSoundEffect() {
        val extras = MiniMaxVoiceModify.toExtras(0, 0, 0, "not_a_real_effect")
        assertTrue(extras.isEmpty())
    }

    @Test
    fun fromExtrasEmptyYieldsDefaults() {
        val v = MiniMaxVoiceModify.fromExtras(emptyMap())
        assertEquals(0, v.pitch)
        assertEquals(0, v.intensity)
        assertEquals(0, v.timbre)
        assertNull(v.soundEffect)
    }

    @Test
    fun toFromRoundTrip() {
        val extras = MiniMaxVoiceModify.toExtras(15, -60, 80, "robotic")
        val v = MiniMaxVoiceModify.fromExtras(extras)
        assertEquals(15, v.pitch)
        assertEquals(-60, v.intensity)
        assertEquals(80, v.timbre)
        assertEquals("robotic", v.soundEffect)
    }

    @Test
    fun fromExtrasIgnoresIllegalSoundEffect() {
        val v = MiniMaxVoiceModify.fromExtras(mapOf(MiniMaxVoiceModify.KEY_SOUND_EFFECTS to "bogus"))
        assertNull(v.soundEffect)
    }

    @Test
    fun buildJsonEmptyReturnsNull() {
        assertNull(MiniMaxVoiceModify.buildJson(emptyMap()))
        assertNull(MiniMaxVoiceModify.buildJson(MiniMaxVoiceModify.toExtras(0, 0, 0, null)))
    }

    @Test
    fun buildJsonProducesCorrectFields() {
        val extras = MiniMaxVoiceModify.toExtras(30, -20, 0, "spacious_echo")
        val json = MiniMaxVoiceModify.buildJson(extras)!!
        assertEquals(30, json.getInt("pitch"))
        assertEquals(-20, json.getInt("intensity"))
        assertEquals("spacious_echo", json.getString("sound_effects"))
        // timbre 为 0 → JSON 里不出现
        assertFalse(json.has("timbre"))
    }
}
