package com.mio.voice.data

import com.mio.voice.data.generation.CollectionFormLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionFormLogicTest {

    @Test
    fun trimsAndAcceptsValidName() {
        val result = CollectionFormLogic.validateName("  2025六级  ")
        assertTrue(result is CollectionFormLogic.NameResult.Valid)
        assertEquals("2025六级", (result as CollectionFormLogic.NameResult.Valid).name)
    }

    @Test
    fun rejectsBlankName() {
        assertTrue(CollectionFormLogic.validateName("   ") is CollectionFormLogic.NameResult.Invalid)
        assertTrue(CollectionFormLogic.validateName("") is CollectionFormLogic.NameResult.Invalid)
    }

    @Test
    fun rejectsTooLongName() {
        val tooLong = "a".repeat(CollectionFormLogic.MAX_NAME_LENGTH + 1)
        assertTrue(CollectionFormLogic.validateName(tooLong) is CollectionFormLogic.NameResult.Invalid)
    }

    @Test
    fun acceptsNameAtMaxLength() {
        val atMax = "a".repeat(CollectionFormLogic.MAX_NAME_LENGTH)
        assertTrue(CollectionFormLogic.validateName(atMax) is CollectionFormLogic.NameResult.Valid)
    }
}
