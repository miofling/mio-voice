package com.mio.voice.data.generation

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeneratedAudioDaoTest {
    private lateinit var database: MioVoiceDatabase
    private lateinit var dao: GeneratedAudioDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MioVoiceDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.generatedAudioDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun persistsGroupAndReturnsSegmentsInPlaybackOrder() = runBlocking {
        dao.insertGroup(group("group", 2L, 2))
        dao.insert(record("second", "group", 1))
        dao.insert(record("first", "group", 0))

        val segments = dao.findSegmentsForGroup("group")

        assertEquals(listOf("first", "second"), segments.map { it.id })
        assertEquals("complete text", dao.findGroupById("group")?.originalText)
    }

    private fun group(id: String, createdAt: Long, segmentCount: Int) = GeneratedAudioGroup(
        id = id,
        originalText = "complete text",
        previewText = "complete text",
        voiceId = "voice-id",
        voiceName = "Mio",
        emotion = "neutral",
        speed = 1f,
        format = "wav",
        createdAt = createdAt,
        updatedAt = createdAt,
        totalDurationMs = 200L,
        segmentCount = segmentCount,
        status = GenerationStatus.Success,
        generationType = GenerationType.PlainText
    )

    private fun record(id: String, groupId: String, index: Int) = GeneratedAudioRecord(
        id = id,
        text = id,
        generationGroupId = groupId,
        segmentIndex = index,
        segmentText = id,
        localAudioPath = "/files/$id.wav",
        durationMs = 100L,
        voiceId = "voice-id",
        voiceName = "Mio",
        emotion = "neutral",
        speed = 1f,
        format = "wav",
        createdAt = index.toLong(),
        status = GenerationStatus.Success
    )
}
