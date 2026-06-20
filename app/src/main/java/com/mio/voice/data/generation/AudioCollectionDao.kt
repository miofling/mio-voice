package com.mio.voice.data.generation

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AudioCollectionDao {
    // ---- 组本身 ----
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCollection(collection: AudioCollection)

    @Query("UPDATE audio_collections SET name = :name, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateCollectionName(id: String, name: String, updatedAt: Long): Int

    @Query("UPDATE audio_collections SET updated_at = :updatedAt WHERE id = :id")
    suspend fun touchCollection(id: String, updatedAt: Long): Int

    @Query("DELETE FROM audio_collections WHERE id = :id")
    suspend fun deleteCollectionById(id: String): Int

    @Query("SELECT * FROM audio_collections ORDER BY updated_at DESC")
    suspend fun findAllCollections(): List<AudioCollection>

    @Query("SELECT * FROM audio_collections WHERE id = :id LIMIT 1")
    suspend fun findCollectionById(id: String): AudioCollection?

    // ---- 成员关系 ----
    // 唯一索引 (collection_id, generation_group_id) + IGNORE → 重复加入天然去重。
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: AudioCollectionItem)

    @Query("DELETE FROM audio_collection_items WHERE collection_id = :collectionId AND generation_group_id = :groupId")
    suspend fun deleteItem(collectionId: String, groupId: String): Int

    @Query("DELETE FROM audio_collection_items WHERE generation_group_id = :groupId")
    suspend fun deleteItemsForGroup(groupId: String): Int

    @Query("SELECT * FROM audio_collection_items WHERE collection_id = :collectionId ORDER BY added_at ASC")
    suspend fun findItemsForCollection(collectionId: String): List<AudioCollectionItem>

    @Query("SELECT COUNT(*) FROM audio_collection_items WHERE collection_id = :collectionId")
    suspend fun countItems(collectionId: String): Int

    @Query("SELECT collection_id FROM audio_collection_items WHERE generation_group_id = :groupId")
    suspend fun findCollectionIdsForGroup(groupId: String): List<String>
}
