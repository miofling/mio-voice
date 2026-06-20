package com.mio.voice.data.generation

import android.content.Context
import android.util.Log
import com.mio.voice.data.TtsRequest
import com.mio.voice.data.TtsResult
import com.mio.voice.provider.TtsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.UUID

class GeneratedAudioRepository(
    private val dao: GeneratedAudioDao,
    private val fileStore: GeneratedAudioFileStore,
    private val durationReader: AudioDurationReader,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() }
) {
    constructor(context: Context) : this(
        dao = MioVoiceDatabase.getInstance(context).generatedAudioDao(),
        fileStore = GeneratedAudioFileStore(context),
        durationReader = AndroidAudioDurationReader()
    )

    suspend fun createGenerationGroup(
        originalText: String,
        voiceId: String,
        voiceName: String,
        emotion: String?,
        speed: Float,
        format: String,
        generationType: GenerationType,
        expectedSegmentCount: Int,
        provider: String? = null,
        model: String? = null,
        previewText: String? = originalText.trim().take(80).ifBlank { null }
    ): GeneratedAudioGroup {
        require(expectedSegmentCount > 0) { "生成组至少需要一个分段。" }
        val now = clock()
        val group = GeneratedAudioGroup(
            id = idFactory(),
            originalText = originalText,
            previewText = previewText,
            voiceId = voiceId,
            voiceName = voiceName,
            emotion = emotion,
            speed = speed,
            format = format,
            createdAt = now,
            updatedAt = now,
            totalDurationMs = 0L,
            segmentCount = expectedSegmentCount,
            status = GenerationStatus.Generating,
            generationType = generationType,
            provider = provider,
            model = model
        )
        try {
            dao.insertGroup(group)
            return group
        } catch (error: Throwable) {
            withContext(NonCancellable + Dispatchers.IO) {
                val inserted = runCatching { dao.findGroupById(group.id) }.getOrNull()
                if (inserted == group) {
                    runCatching {
                        dao.failGroupAndDeleteSegments(group.id, safeMessage(error), clock())
                    }
                }
            }
            throw error
        }
    }

    suspend fun generateAndSaveSegment(
        groupId: String,
        segmentIndex: Int,
        segmentText: String?,
        request: TtsRequest,
        provider: TtsProvider
    ): GeneratedAudioRecord = persistSegment(groupId, segmentIndex, segmentText, request) {
        provider.generate(request)
    }

    suspend fun saveSegmentResult(
        groupId: String,
        segmentIndex: Int,
        segmentText: String?,
        request: TtsRequest,
        result: TtsResult
    ): GeneratedAudioRecord = persistSegment(groupId, segmentIndex, segmentText, request) { result }

    suspend fun addExistingFileSegment(
        groupId: String,
        segmentIndex: Int,
        segmentText: String?,
        source: GeneratedAudioRecord
    ): GeneratedAudioRecord {
        val group = requireGeneratingGroup(groupId)
        require(source.generationGroupId == groupId) { "不能跨生成组复用音频文件。" }
        require(fileStore.exists(source.localAudioPath)) { "复用的本地音频文件不存在。" }
        val record = source.copy(
            id = idFactory(),
            text = segmentText ?: source.text,
            segmentIndex = segmentIndex,
            segmentText = segmentText,
            createdAt = clock()
        )
        try {
            dao.insert(record)
            return record
        } catch (error: Throwable) {
            failGenerationGroup(group.id, safeMessage(error))
            throw error
        }
    }

    suspend fun completeGenerationGroup(groupId: String): GeneratedAudioGroup {
        val group = requireGeneratingGroup(groupId)
        val segments = dao.findSegmentsForGroup(groupId)
        val expectedIndices = (0 until group.segmentCount).toList()
        val valid = segments.size == group.segmentCount &&
            segments.map { it.segmentIndex } == expectedIndices &&
            segments.all { it.status == GenerationStatus.Success && fileStore.exists(it.localAudioPath) }
        if (!valid) {
            val message = "生成组分段不完整，无法标记成功。"
            failGenerationGroup(groupId, message)
            throw IllegalStateException(message)
        }
        val durationMs = segments.sumOf { it.durationMs }
        val format = segments.firstOrNull()?.format ?: group.format
        val updatedAt = clock()
        check(
            dao.markGroupSuccess(
                id = groupId,
                totalDurationMs = durationMs,
                segmentCount = segments.size,
                format = format,
                updatedAt = updatedAt
            ) == 1
        ) { "无法完成生成组。" }
        return group.copy(
            totalDurationMs = durationMs,
            segmentCount = segments.size,
            format = format,
            updatedAt = updatedAt,
            status = GenerationStatus.Success,
            errorMessage = null
        )
    }

    suspend fun failGenerationGroup(groupId: String, errorMessage: String) {
        withContext(NonCancellable + Dispatchers.IO) {
            val segments = runCatching { dao.findSegmentsForGroup(groupId) }.getOrDefault(emptyList())
            val marked = runCatching {
                dao.failGroupAndDeleteSegments(groupId, errorMessage.take(500), clock())
            }.onFailure { logWarning("标记生成组失败：$groupId", it) }.isSuccess
            if (marked) deleteFiles(segments, "清理失败生成组音频")
        }
    }

    suspend fun getGenerationGroup(groupId: String): GeneratedAudioGroup? = withContext(Dispatchers.IO) {
        val group = dao.findGroupById(groupId) ?: return@withContext null
        validateGroup(group)
    }

    suspend fun getSegmentsForGroup(groupId: String): List<GeneratedAudioRecord> =
        withContext(Dispatchers.IO) { dao.findSegmentsForGroup(groupId) }

    suspend fun getRecentSuccessfulGroups(limit: Int): List<GeneratedAudioGroup> =
        withContext(Dispatchers.IO) {
            if (limit <= 0) return@withContext emptyList()
            val result = mutableListOf<GeneratedAudioGroup>()
            val pageSize = maxOf(limit * 2, 20)
            while (result.size < limit) {
                val page = dao.findRecentGroupsByStatus(
                    GenerationStatus.Success,
                    pageSize,
                    result.size
                )
                if (page.isEmpty()) break
                var invalidCount = 0
                page.forEach { group ->
                    val valid = validateGroup(group)
                    if (valid.status == GenerationStatus.Success && result.size < limit) {
                        result += valid
                    } else {
                        invalidCount++
                    }
                }
                if (page.size < pageSize && invalidCount == 0) break
            }
            result
        }

    suspend fun getRecentThreeSuccessfulGroups(): List<GeneratedAudioGroup> =
        getRecentSuccessfulGroups(3)

    /** 历史页：返回全部成功且文件完整的生成组，按生成时间倒序。 */
    suspend fun getAllSuccessfulGroups(): List<GeneratedAudioGroup> = withContext(Dispatchers.IO) {
        dao.findAllGroupsByStatus(GenerationStatus.Success)
            .map { validateGroup(it) }
            .filter { it.status == GenerationStatus.Success }
    }

    /** 计算生成组所有本地音频分段文件的总字节数（去重路径）。 */
    suspend fun getGroupFileSizeBytes(groupId: String): Long = withContext(Dispatchers.IO) {
        dao.findSegmentsForGroup(groupId)
            .asSequence()
            .map { it.localAudioPath }
            .filter { it.isNotBlank() }
            .distinct()
            .map { path -> runCatching { java.io.File(path).length() }.getOrDefault(0L) }
            .sum()
    }

    /** 给整条生成记录起自定义标题；空白归一为 null（清除，回退到预览文本）。 */
    suspend fun renameGroup(groupId: String, title: String?): Boolean = withContext(Dispatchers.IO) {
        val normalized = title?.trim()?.takeIf { it.isNotEmpty() }
        dao.updateCustomTitle(groupId, normalized, System.currentTimeMillis()) > 0
    }

    suspend fun deleteGenerationGroup(groupId: String): Boolean = withContext(Dispatchers.IO) {
        val group = dao.findGroupById(groupId) ?: return@withContext false
        val segments = dao.findSegmentsForGroup(group.id)
        if (dao.deleteGroupById(groupId) <= 0) return@withContext false
        deleteFiles(segments, "删除生成组音频")
        true
    }

    // Compatibility APIs retained for existing callers. New history code should use group APIs above.
    suspend fun generateAndSave(
        request: TtsRequest,
        voiceName: String,
        provider: TtsProvider
    ): GeneratedAudioRecord {
        val group = createSingleSegmentGroup(request, voiceName)
        return try {
            val record = generateAndSaveSegment(group.id, 0, request.text, request, provider)
            completeGenerationGroup(group.id)
            record
        } catch (error: Throwable) {
            failGenerationGroup(group.id, safeMessage(error))
            throw error
        }
    }

    suspend fun saveGeneratedAudio(
        request: TtsRequest,
        voiceName: String,
        result: TtsResult
    ): GeneratedAudioRecord {
        val group = createSingleSegmentGroup(request, voiceName)
        return try {
            val record = saveSegmentResult(group.id, 0, request.text, request, result)
            completeGenerationGroup(group.id)
            record
        } catch (error: Throwable) {
            failGenerationGroup(group.id, safeMessage(error))
            throw error
        }
    }

    suspend fun getById(id: String): GeneratedAudioRecord? = withContext(Dispatchers.IO) {
        val record = dao.findById(id) ?: return@withContext null
        val group = dao.findGroupById(record.generationGroupId) ?: return@withContext null
        val validated = validateGroup(group)
        if (validated.status == GenerationStatus.Success) record else record.copy(
            localAudioPath = "",
            durationMs = 0L,
            status = GenerationStatus.Failed,
            errorMessage = validated.errorMessage
        )
    }

    suspend fun getRecentSuccessful(limit: Int): List<GeneratedAudioRecord> = withContext(Dispatchers.IO) {
        if (limit <= 0) return@withContext emptyList()
        dao.findRecentByStatus(GenerationStatus.Success, maxOf(limit * 2, 20), 0)
            .filter { record ->
                dao.findGroupById(record.generationGroupId)?.let { validateGroup(it) }?.status == GenerationStatus.Success
            }
            .take(limit)
    }

    suspend fun getRecentThreeSuccessful(): List<GeneratedAudioRecord> = getRecentSuccessful(3)

    suspend fun delete(id: String): Boolean {
        val record = dao.findById(id) ?: return false
        return deleteGenerationGroup(record.generationGroupId)
    }

    suspend fun localAudioExists(path: String): Boolean = withContext(Dispatchers.IO) {
        fileStore.exists(path)
    }

    suspend fun cleanupInterruptedWork() {
        fileStore.cleanupExpiredTemporaryFiles()
        dao.findGroupsByStatus(GenerationStatus.Generating).forEach { group ->
            failGenerationGroup(group.id, "上次生成被中断。")
        }
    }

    private suspend fun persistSegment(
        groupId: String,
        segmentIndex: Int,
        segmentText: String?,
        request: TtsRequest,
        resultProvider: suspend () -> TtsResult
    ): GeneratedAudioRecord {
        val group = requireGeneratingGroup(groupId)
        require(segmentIndex in 0 until group.segmentCount) { "分段序号超出生成组范围。" }
        val id = idFactory()
        val createdAt = clock()
        val generating = GeneratedAudioRecord(
            id = id,
            text = segmentText ?: request.text,
            generationGroupId = groupId,
            segmentIndex = segmentIndex,
            segmentText = segmentText,
            localAudioPath = "",
            durationMs = 0L,
            voiceId = request.voiceId,
            voiceName = group.voiceName,
            emotion = request.emotion,
            speed = request.speed,
            format = request.audioFormat,
            createdAt = createdAt,
            status = GenerationStatus.Generating
        )
        var savedFile: SavedAudioFile? = null
        try {
            dao.insert(generating)
            val result = resultProvider()
            savedFile = fileStore.save(id, createdAt, result.audioBytes, result.audioFormat)
            val durationMs = runCatching { durationReader.readDurationMs(savedFile.file) }
                .onFailure { logWarning("读取音频时长失败：$id", it) }
                .getOrDefault(0L)
            check(
                dao.markSuccess(id, savedFile.file.absolutePath, durationMs, savedFile.format) == 1
            ) { "无法更新音频分段记录。" }
            return generating.copy(
                localAudioPath = savedFile.file.absolutePath,
                durationMs = durationMs,
                format = savedFile.format,
                status = GenerationStatus.Success
            )
        } catch (error: Throwable) {
            withContext(NonCancellable + Dispatchers.IO) {
                savedFile?.file?.absolutePath?.let { fileStore.delete(it) }
                failGenerationGroup(groupId, safeMessage(error))
            }
            throw error
        }
    }

    private suspend fun requireGeneratingGroup(groupId: String): GeneratedAudioGroup {
        val group = dao.findGroupById(groupId) ?: error("生成组不存在。")
        check(group.status == GenerationStatus.Generating) { "生成组已结束，不能继续写入。" }
        return group
    }

    private suspend fun validateGroup(group: GeneratedAudioGroup): GeneratedAudioGroup {
        if (group.status != GenerationStatus.Success) return group
        val segments = dao.findSegmentsForGroup(group.id)
        val valid = segments.size == group.segmentCount &&
            segments.map { it.segmentIndex } == (0 until group.segmentCount).toList() &&
            segments.all { it.status == GenerationStatus.Success && fileStore.exists(it.localAudioPath) }
        if (valid) return group
        val message = "生成组包含缺失或无效的音频分段。"
        failGenerationGroup(group.id, message)
        return group.copy(
            totalDurationMs = 0L,
            segmentCount = 0,
            status = GenerationStatus.Failed,
            updatedAt = clock(),
            errorMessage = message
        )
    }

    private suspend fun createSingleSegmentGroup(
        request: TtsRequest,
        voiceName: String
    ): GeneratedAudioGroup = createGenerationGroup(
        originalText = request.text,
        voiceId = request.voiceId,
        voiceName = voiceName,
        emotion = request.emotion,
        speed = request.speed,
        format = request.audioFormat,
        generationType = GenerationType.Legacy,
        expectedSegmentCount = 1
    )

    private fun deleteFiles(segments: List<GeneratedAudioRecord>, action: String) {
        segments.asSequence()
            .map { it.localAudioPath }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { path ->
                if (!fileStore.delete(path)) logWarning("${action}失败：${java.io.File(path).name}")
            }
    }

    private fun safeMessage(error: Throwable): String =
        error.message
            ?.replace(Regex("Bearer\\s+[^\\s]+", RegexOption.IGNORE_CASE), "Bearer ***")
            ?.take(500)
            ?: "音频生成或保存失败。"

    private fun logWarning(message: String, error: Throwable? = null) {
        runCatching {
            if (error == null) Log.w(TAG, message) else Log.w(TAG, message, error)
        }
    }

    private companion object {
        const val TAG = "GeneratedAudioRepo"
    }
}
