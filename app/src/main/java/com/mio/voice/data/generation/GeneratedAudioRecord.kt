package com.mio.voice.data.generation

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class GenerationStatus {
    Generating,
    Success,
    Failed
}

enum class GenerationType(val storageValue: String) {
    PlainText("plain_text"),
    Words("words"),
    AiDirector("ai_director"),
    Legacy("legacy")
}

@Entity(
    tableName = "generated_audio_groups",
    indices = [
        Index(value = ["created_at"]),
        Index(value = ["status", "created_at"])
    ]
)
data class GeneratedAudioGroup(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "original_text") val originalText: String,
    @ColumnInfo(name = "preview_text") val previewText: String?,
    @ColumnInfo(name = "voice_id") val voiceId: String,
    @ColumnInfo(name = "voice_name") val voiceName: String,
    val emotion: String?,
    val speed: Float,
    val format: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "total_duration_ms") val totalDurationMs: Long,
    @ColumnInfo(name = "segment_count") val segmentCount: Int,
    val status: GenerationStatus,
    @ColumnInfo(name = "error_message") val errorMessage: String? = null,
    @ColumnInfo(name = "generation_type") val generationType: GenerationType,
    // v3 新增：保存生成时真实使用的服务提供商与模型，旧记录为 null（UI 显示“未知”）。
    val provider: String? = null,
    val model: String? = null,
    // v5 新增：用户为整条生成记录起的自定义标题；非空时作为该记录的统一显示名，回退到 previewText/originalText。
    @ColumnInfo(name = "custom_title") val customTitle: String? = null
)

@Entity(
    tableName = "generated_audio_records",
    foreignKeys = [
        ForeignKey(
            entity = GeneratedAudioGroup::class,
            parentColumns = ["id"],
            childColumns = ["generation_group_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["created_at"]),
        Index(value = ["status"]),
        Index(value = ["generation_group_id"]),
        Index(value = ["generation_group_id", "segment_index"], unique = true)
    ]
)
data class GeneratedAudioRecord(
    @PrimaryKey val id: String,
    val text: String,
    @ColumnInfo(name = "generation_group_id") val generationGroupId: String,
    @ColumnInfo(name = "segment_index") val segmentIndex: Int,
    @ColumnInfo(name = "segment_text") val segmentText: String?,
    @ColumnInfo(name = "local_audio_path") val localAudioPath: String,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "voice_id") val voiceId: String,
    @ColumnInfo(name = "voice_name") val voiceName: String,
    val emotion: String?,
    val speed: Float,
    val format: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val status: GenerationStatus,
    @ColumnInfo(name = "error_message") val errorMessage: String? = null
)
