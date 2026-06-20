package com.mio.voice.data.generation

import com.mio.voice.data.ProviderConfig
import com.mio.voice.data.TtsRequest
import com.mio.voice.data.TtsResult
import com.mio.voice.provider.TtsProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.ArrayDeque

class GeneratedAudioRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun createsAndQueriesSingleSegmentGroup() = runTest {
        val fixture = fixture(durations = listOf(640L))
        val group = fixture.createGroup("hello", 1)

        fixture.repository.saveSegmentResult(group.id, 0, "hello", request("hello"), audio())
        val completed = fixture.repository.completeGenerationGroup(group.id)

        assertEquals(GenerationStatus.Success, completed.status)
        assertEquals(1, completed.segmentCount)
        assertEquals(640L, completed.totalDurationMs)
        assertEquals("hello", fixture.repository.getGenerationGroup(group.id)?.originalText)
    }

    @Test
    fun multipleSegmentsShareGroupAndRemainInIndexOrder() = runTest {
        val fixture = fixture(durations = listOf(100L, 200L, 300L))
        val group = fixture.createGroup("complete text", 3)
        listOf("third", "first", "second").forEachIndexed { index, text ->
            fixture.repository.saveSegmentResult(group.id, index, text, request(text), audio())
        }

        val completed = fixture.repository.completeGenerationGroup(group.id)
        val segments = fixture.repository.getSegmentsForGroup(group.id)

        assertTrue(segments.all { it.generationGroupId == group.id })
        assertEquals(listOf(0, 1, 2), segments.map { it.segmentIndex })
        assertEquals(600L, completed.totalDurationMs)
    }

    @Test
    fun recentThreeReturnsGroupsRatherThanSegments() = runTest {
        var now = 100L
        val fixture = fixture(clock = { now++ }, durations = List(8) { 50L })
        repeat(4) { groupIndex ->
            val group = fixture.createGroup("group-$groupIndex", 2)
            repeat(2) { segmentIndex ->
                fixture.repository.saveSegmentResult(
                    group.id,
                    segmentIndex,
                    "segment-$segmentIndex",
                    request("segment-$segmentIndex"),
                    audio()
                )
            }
            fixture.repository.completeGenerationGroup(group.id)
        }

        val recent = fixture.repository.getRecentThreeSuccessfulGroups()

        assertEquals(listOf("group-3", "group-2", "group-1"), recent.map { it.originalText })
        assertTrue(recent.all { it.segmentCount == 2 })
    }

    @Test
    fun getAllSuccessfulGroupsReturnsEveryValidGroupNewestFirst() = runTest {
        var now = 100L
        val fixture = fixture(clock = { now++ }, durations = List(10) { 50L })
        repeat(5) { groupIndex ->
            val group = fixture.createGroup("all-$groupIndex", 1)
            fixture.repository.saveSegmentResult(group.id, 0, "seg", request("seg"), audio())
            fixture.repository.completeGenerationGroup(group.id)
        }

        val all = fixture.repository.getAllSuccessfulGroups()

        assertEquals(
            listOf("all-4", "all-3", "all-2", "all-1", "all-0"),
            all.map { it.originalText }
        )
    }

    @Test
    fun getAllSuccessfulGroupsExcludesGroupsWithMissingFiles() = runTest {
        val fixture = fixture(durations = listOf(100L, 100L))
        val intact = fixture.createGroup("intact", 1)
        fixture.repository.saveSegmentResult(intact.id, 0, "seg", request("seg"), audio())
        fixture.repository.completeGenerationGroup(intact.id)
        val broken = fixture.createGroup("broken", 1)
        fixture.repository.saveSegmentResult(broken.id, 0, "seg", request("seg"), audio())
        fixture.repository.completeGenerationGroup(broken.id)
        File(fixture.repository.getSegmentsForGroup(broken.id).first().localAudioPath).delete()

        val all = fixture.repository.getAllSuccessfulGroups()

        assertEquals(listOf("intact"), all.map { it.originalText })
    }

    @Test
    fun groupFileSizeSumsDistinctSegmentFileBytes() = runTest {
        val fixture = fixture(durations = listOf(100L, 100L))
        val group = fixture.createGroup("size", 2)
        repeat(2) { index ->
            fixture.repository.saveSegmentResult(group.id, index, "s$index", request("s$index"), audio())
        }
        fixture.repository.completeGenerationGroup(group.id)

        val expected = fixture.repository.getSegmentsForGroup(group.id)
            .map { it.localAudioPath }
            .distinct()
            .sumOf { File(it).length() }

        assertTrue(expected > 0L)
        assertEquals(expected, fixture.repository.getGroupFileSizeBytes(group.id))
    }

    @Test
    fun createGenerationGroupPersistsProviderAndModel() = runTest {
        val fixture = fixture(durations = listOf(100L))
        val group = fixture.repository.createGenerationGroup(
            originalText = "meta",
            voiceId = "voice-id",
            voiceName = "Mio",
            emotion = "happy",
            speed = 1.1f,
            format = "wav",
            generationType = GenerationType.PlainText,
            expectedSegmentCount = 1,
            provider = "MiniMax",
            model = "speech-02"
        )

        assertEquals("MiniMax", group.provider)
        assertEquals("speech-02", group.model)
        val stored = fixture.dao.findGroupById(group.id)
        assertEquals("MiniMax", stored?.provider)
        assertEquals("speech-02", stored?.model)
    }

    @Test
    fun deletingGroupDeletesEverySegmentFileAndDatabaseRow() = runTest {
        val fixture = fixture(durations = listOf(100L, 100L))
        val group = fixture.createGroup("delete", 2)
        repeat(2) { index ->
            fixture.repository.saveSegmentResult(group.id, index, "s$index", request("s$index"), audio())
        }
        fixture.repository.completeGenerationGroup(group.id)
        val files = fixture.repository.getSegmentsForGroup(group.id).map { File(it.localAudioPath) }

        assertTrue(fixture.repository.deleteGenerationGroup(group.id))

        assertTrue(files.none { it.exists() })
        assertEquals(null, fixture.dao.findGroupById(group.id))
        assertTrue(fixture.dao.findSegmentsForGroup(group.id).isEmpty())
    }

    @Test
    fun missingSegmentFileInvalidatesWholeGroup() = runTest {
        val fixture = fixture(durations = listOf(100L, 100L))
        val group = fixture.createGroup("missing", 2)
        repeat(2) { index ->
            fixture.repository.saveSegmentResult(group.id, index, "s$index", request("s$index"), audio())
        }
        fixture.repository.completeGenerationGroup(group.id)
        File(fixture.repository.getSegmentsForGroup(group.id).first().localAudioPath).delete()

        val recent = fixture.repository.getRecentSuccessfulGroups(3)

        assertTrue(recent.isEmpty())
        assertEquals(GenerationStatus.Failed, fixture.dao.findGroupById(group.id)?.status)
        assertTrue(fixture.dao.findSegmentsForGroup(group.id).isEmpty())
    }

    @Test
    fun providerFailureOrCancellationCannotLeaveSuccessfulGroup() = runTest {
        val fixture = fixture(durations = listOf(100L))
        val group = fixture.createGroup("cancel", 2)
        val first = fixture.repository.saveSegmentResult(group.id, 0, "first", request("first"), audio())
        val cancellingProvider = object : TtsProvider {
            override suspend fun generate(request: TtsRequest): TtsResult {
                throw CancellationException("cancelled")
            }

            override suspend fun testConnection(config: ProviderConfig): TtsResult = audio()
        }

        val error = runCatching {
            fixture.repository.generateAndSaveSegment(
                group.id,
                1,
                "second",
                request("second"),
                cancellingProvider
            )
        }.exceptionOrNull()

        assertTrue(error is CancellationException)
        assertEquals(GenerationStatus.Failed, fixture.dao.findGroupById(group.id)?.status)
        assertTrue(fixture.dao.findSegmentsForGroup(group.id).isEmpty())
        assertFalse(File(first.localAudioPath).exists())
        assertTrue(fixture.repository.getRecentSuccessfulGroups(3).isEmpty())
    }

    private fun fixture(
        clock: () -> Long = { 123L },
        durations: List<Long> = emptyList()
    ): Fixture {
        val dao = FakeGeneratedAudioDao()
        val directory = temporaryFolder.newFolder("generated-${System.nanoTime()}")
        var nextId = 0
        val durationQueue = ArrayDeque(durations)
        val repository = GeneratedAudioRepository(
            dao = dao,
            fileStore = GeneratedAudioFileStore(directory),
            durationReader = AudioDurationReader {
                if (durationQueue.isEmpty()) 640L else durationQueue.removeFirst()
            },
            clock = clock,
            idFactory = { "id-${nextId++}" }
        )
        return Fixture(repository, dao)
    }

    private suspend fun Fixture.createGroup(text: String, count: Int): GeneratedAudioGroup =
        repository.createGenerationGroup(
            originalText = text,
            voiceId = "voice-id",
            voiceName = "Mio",
            emotion = "happy",
            speed = 1.1f,
            format = "wav",
            generationType = GenerationType.PlainText,
            expectedSegmentCount = count
        )

    private fun request(text: String) = TtsRequest(
        providerProfileId = "fake",
        config = ProviderConfig(audioFormat = "wav"),
        text = text,
        voiceId = "voice-id",
        model = "fake",
        speed = 1.1f,
        emotion = "happy",
        audioFormat = "wav"
    )

    private fun audio() = TtsResult(byteArrayOf(1, 2, 3), "wav", "audio/wav")

    private data class Fixture(
        val repository: GeneratedAudioRepository,
        val dao: FakeGeneratedAudioDao
    )
}

private class FakeGeneratedAudioDao : GeneratedAudioDao {
    private val groups = linkedMapOf<String, GeneratedAudioGroup>()
    private val records = linkedMapOf<String, GeneratedAudioRecord>()

    override suspend fun insertGroup(group: GeneratedAudioGroup) {
        check(groups.putIfAbsent(group.id, group) == null)
    }

    override suspend fun insert(record: GeneratedAudioRecord) {
        check(groups.containsKey(record.generationGroupId))
        check(records.values.none {
            it.generationGroupId == record.generationGroupId && it.segmentIndex == record.segmentIndex
        })
        check(records.putIfAbsent(record.id, record) == null)
    }

    override suspend fun findGroupById(id: String): GeneratedAudioGroup? = groups[id]

    override suspend fun findRecentGroupsByStatus(
        status: GenerationStatus,
        limit: Int,
        offset: Int
    ): List<GeneratedAudioGroup> = groups.values
        .filter { it.status == status }
        .sortedByDescending { it.createdAt }
        .drop(offset)
        .take(limit)

    override suspend fun findGroupsByStatus(status: GenerationStatus): List<GeneratedAudioGroup> =
        groups.values.filter { it.status == status }

    override suspend fun findAllGroupsByStatus(status: GenerationStatus): List<GeneratedAudioGroup> =
        groups.values.filter { it.status == status }.sortedByDescending { it.createdAt }

    override suspend fun findSegmentsForGroup(groupId: String): List<GeneratedAudioRecord> =
        records.values.filter { it.generationGroupId == groupId }.sortedBy { it.segmentIndex }

    override suspend fun findById(id: String): GeneratedAudioRecord? = records[id]

    override suspend fun findRecentByStatus(
        status: GenerationStatus,
        limit: Int,
        offset: Int
    ): List<GeneratedAudioRecord> = records.values
        .filter { it.status == status }
        .sortedByDescending { it.createdAt }
        .drop(offset)
        .take(limit)

    override suspend fun markSuccess(
        id: String,
        localAudioPath: String,
        durationMs: Long,
        format: String,
        status: GenerationStatus
    ): Int = updateRecord(id) {
        it.copy(
            localAudioPath = localAudioPath,
            durationMs = durationMs,
            format = format,
            status = status,
            errorMessage = null
        )
    }

    override suspend fun markFailed(id: String, errorMessage: String, status: GenerationStatus): Int =
        updateRecord(id) { it.copy(status = status, errorMessage = errorMessage) }

    override suspend fun markGroupSuccess(
        id: String,
        totalDurationMs: Long,
        segmentCount: Int,
        format: String,
        updatedAt: Long,
        generatingStatus: GenerationStatus,
        successStatus: GenerationStatus
    ): Int = updateGroup(id) {
        if (it.status != generatingStatus) it else it.copy(
            totalDurationMs = totalDurationMs,
            segmentCount = segmentCount,
            format = format,
            updatedAt = updatedAt,
            status = successStatus,
            errorMessage = null
        )
    }

    override suspend fun markGroupFailed(
        id: String,
        errorMessage: String,
        updatedAt: Long,
        failedStatus: GenerationStatus
    ): Int = updateGroup(id) {
        it.copy(
            totalDurationMs = 0L,
            segmentCount = 0,
            status = failedStatus,
            updatedAt = updatedAt,
            errorMessage = errorMessage
        )
    }

    override suspend fun failInterruptedRecords(
        errorMessage: String,
        generatingStatus: GenerationStatus,
        failedStatus: GenerationStatus
    ): Int {
        val ids = records.values.filter { it.status == generatingStatus }.map { it.id }
        ids.forEach { markFailed(it, errorMessage, failedStatus) }
        return ids.size
    }

    override suspend fun deleteSegmentsForGroup(groupId: String): Int {
        val ids = records.values.filter { it.generationGroupId == groupId }.map { it.id }
        ids.forEach(records::remove)
        return ids.size
    }

    override suspend fun deleteGroupById(groupId: String): Int {
        val deleted = groups.remove(groupId) ?: return 0
        deleteSegmentsForGroup(deleted.id)
        return 1
    }

    override suspend fun deleteById(id: String): Int = if (records.remove(id) != null) 1 else 0

    override suspend fun updateCustomTitle(id: String, title: String?, updatedAt: Long): Int {
        val current = groups[id] ?: return 0
        groups[id] = current.copy(customTitle = title, updatedAt = updatedAt)
        return 1
    }

    private fun updateRecord(
        id: String,
        transform: (GeneratedAudioRecord) -> GeneratedAudioRecord
    ): Int {
        val current = records[id] ?: return 0
        records[id] = transform(current)
        return 1
    }

    private fun updateGroup(
        id: String,
        transform: (GeneratedAudioGroup) -> GeneratedAudioGroup
    ): Int {
        val current = groups[id] ?: return 0
        groups[id] = transform(current)
        return 1
    }
}
