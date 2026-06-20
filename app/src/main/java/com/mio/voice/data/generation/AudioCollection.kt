package com.mio.voice.data.generation

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 语音库「组」（收藏集）。叠在历史记录之上的管理层，不改动生成管线/历史两张表。
 * 一个组通过 [AudioCollectionItem] 多对多关联若干条历史生成记录（GeneratedAudioGroup）。
 */
@Entity(
    tableName = "audio_collections",
    indices = [Index(value = ["updated_at"])]
)
data class AudioCollection(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    // 加入/移出/改名时更新，列表按此倒序，最近操作的组排前面。
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

/**
 * 组↔历史记录 的多对多关系行。
 *
 * - [collectionId] 真实外键（删组级联清成员）。
 * - [generationGroupId] 仅逻辑引用现有 generated_audio_groups.id，**不加真实外键**：
 *   历史删除走另一个 repo，跨表外键会强耦合；改为读取时惰性过滤无效成员 + 删历史时显式清孤儿。
 */
@Entity(
    tableName = "audio_collection_items",
    foreignKeys = [
        ForeignKey(
            entity = AudioCollection::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["collection_id"]),
        Index(value = ["collection_id", "generation_group_id"], unique = true)
    ]
)
data class AudioCollectionItem(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "collection_id") val collectionId: String,
    @ColumnInfo(name = "generation_group_id") val generationGroupId: String,
    @ColumnInfo(name = "added_at") val addedAt: Long
)

/** 组列表项摘要：组本身 + 成员数量。 */
data class CollectionSummary(
    val collection: AudioCollection,
    val itemCount: Int
)
