package com.mio.voice.data.generation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 语音库「组」的数据访问层，与 [GeneratedAudioRepository] 平行。
 * 复用同一个 Room DB 实例；读取组成员的分段时委托 [GeneratedAudioRepository]。
 */
class AudioCollectionRepository(
    private val dao: AudioCollectionDao,
    private val audioRepository: GeneratedAudioRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() }
) {
    constructor(context: Context, audioRepository: GeneratedAudioRepository) : this(
        dao = MioVoiceDatabase.getInstance(context).audioCollectionDao(),
        audioRepository = audioRepository
    )

    suspend fun createCollection(name: String): AudioCollection = withContext(Dispatchers.IO) {
        val now = clock()
        val collection = AudioCollection(id = idFactory(), name = name, createdAt = now, updatedAt = now)
        dao.insertCollection(collection)
        collection
    }

    suspend fun renameCollection(id: String, name: String): Boolean = withContext(Dispatchers.IO) {
        dao.updateCollectionName(id, name, clock()) > 0
    }

    suspend fun deleteCollection(id: String): Boolean = withContext(Dispatchers.IO) {
        dao.deleteCollectionById(id) > 0
    }

    /** 组列表（按 updated_at 倒序）+ 各组成员数量。 */
    suspend fun listCollections(): List<CollectionSummary> = withContext(Dispatchers.IO) {
        dao.findAllCollections().map { collection ->
            CollectionSummary(collection, dao.countItems(collection.id))
        }
    }

    suspend fun getCollection(id: String): AudioCollection? = withContext(Dispatchers.IO) {
        dao.findCollectionById(id)
    }

    suspend fun addToCollection(collectionId: String, groupId: String): Boolean = withContext(Dispatchers.IO) {
        val now = clock()
        dao.insertItem(
            AudioCollectionItem(
                id = idFactory(),
                collectionId = collectionId,
                generationGroupId = groupId,
                addedAt = now
            )
        )
        dao.touchCollection(collectionId, now)
        true
    }

    suspend fun removeFromCollection(collectionId: String, groupId: String): Boolean = withContext(Dispatchers.IO) {
        val removed = dao.deleteItem(collectionId, groupId) > 0
        if (removed) dao.touchCollection(collectionId, clock())
        removed
    }

    /** 这条历史记录当前归属的组 id 集合（给「加入组」弹窗预勾选）。 */
    suspend fun collectionIdsForGroup(groupId: String): Set<String> = withContext(Dispatchers.IO) {
        dao.findCollectionIdsForGroup(groupId).toSet()
    }

    /** 本组当前的成员记录 id 集合（给「从历史选记录」页过滤已在组的）。 */
    suspend fun memberGroupIds(collectionId: String): Set<String> = withContext(Dispatchers.IO) {
        dao.findItemsForCollection(collectionId).map { it.generationGroupId }.toSet()
    }

    /** 删除某条历史记录时清理它在所有组里的孤儿成员行。 */
    suspend fun removeGroupEverywhere(groupId: String): Int = withContext(Dispatchers.IO) {
        dao.deleteItemsForGroup(groupId)
    }

    /**
     * 组成员对应的历史记录（已过滤无效/已删/文件缺失），按加入顺序。
     * 用于组详情页渲染成员卡片。
     */
    suspend fun getMemberGroups(collectionId: String): List<GeneratedAudioGroup> = withContext(Dispatchers.IO) {
        dao.findItemsForCollection(collectionId).mapNotNull { item ->
            audioRepository.getGenerationGroup(item.generationGroupId)
                ?.takeIf { it.status == GenerationStatus.Success }
        }
    }

    /**
     * 组内连续播放用：把所有有效成员的分段拼接并重排 segment_index 为 0..N-1。
     * 返回扁平分段列表 + 被跳过的失效成员数。
     */
    suspend fun getPlayableSegmentsForCollection(
        collectionId: String
    ): CollectionPlaybackFlattener.Result = withContext(Dispatchers.IO) {
        val items = dao.findItemsForCollection(collectionId)
        val memberSegments = items.map { item ->
            val group = audioRepository.getGenerationGroup(item.generationGroupId)
            if (group == null || group.status != GenerationStatus.Success) {
                emptyList()
            } else {
                audioRepository.getSegmentsForGroup(item.generationGroupId)
                    .filter { it.localAudioPath.isNotBlank() && audioRepository.localAudioExists(it.localAudioPath) }
            }
        }
        CollectionPlaybackFlattener.flatten(memberSegments)
    }
}
