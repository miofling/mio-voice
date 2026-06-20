package com.mio.voice.data.generation

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface GeneratedAudioDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGroup(group: GeneratedAudioGroup)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: GeneratedAudioRecord)

    @Query("SELECT * FROM generated_audio_groups WHERE id = :id LIMIT 1")
    suspend fun findGroupById(id: String): GeneratedAudioGroup?

    @Query(
        "SELECT * FROM generated_audio_groups " +
            "WHERE status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun findRecentGroupsByStatus(
        status: GenerationStatus,
        limit: Int,
        offset: Int
    ): List<GeneratedAudioGroup>

    @Query("SELECT * FROM generated_audio_groups WHERE status = :status")
    suspend fun findGroupsByStatus(status: GenerationStatus): List<GeneratedAudioGroup>

    @Query(
        "SELECT * FROM generated_audio_groups " +
            "WHERE status = :status ORDER BY created_at DESC"
    )
    suspend fun findAllGroupsByStatus(status: GenerationStatus): List<GeneratedAudioGroup>

    @Query(
        "SELECT * FROM generated_audio_records " +
            "WHERE generation_group_id = :groupId ORDER BY segment_index ASC"
    )
    suspend fun findSegmentsForGroup(groupId: String): List<GeneratedAudioRecord>

    @Query("SELECT * FROM generated_audio_records WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): GeneratedAudioRecord?

    @Query(
        "SELECT * FROM generated_audio_records " +
            "WHERE status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun findRecentByStatus(
        status: GenerationStatus,
        limit: Int,
        offset: Int
    ): List<GeneratedAudioRecord>

    @Query(
        "UPDATE generated_audio_records SET " +
            "local_audio_path = :localAudioPath, duration_ms = :durationMs, format = :format, " +
            "status = :status, error_message = NULL WHERE id = :id"
    )
    suspend fun markSuccess(
        id: String,
        localAudioPath: String,
        durationMs: Long,
        format: String,
        status: GenerationStatus = GenerationStatus.Success
    ): Int

    @Query(
        "UPDATE generated_audio_records SET local_audio_path = '', duration_ms = 0, " +
            "status = :status, error_message = :errorMessage WHERE id = :id"
    )
    suspend fun markFailed(
        id: String,
        errorMessage: String,
        status: GenerationStatus = GenerationStatus.Failed
    ): Int

    @Query(
        "UPDATE generated_audio_groups SET total_duration_ms = :totalDurationMs, " +
            "segment_count = :segmentCount, format = :format, status = :successStatus, " +
            "updated_at = :updatedAt, error_message = NULL " +
            "WHERE id = :id AND status = :generatingStatus"
    )
    suspend fun markGroupSuccess(
        id: String,
        totalDurationMs: Long,
        segmentCount: Int,
        format: String,
        updatedAt: Long,
        generatingStatus: GenerationStatus = GenerationStatus.Generating,
        successStatus: GenerationStatus = GenerationStatus.Success
    ): Int

    @Query(
        "UPDATE generated_audio_groups SET total_duration_ms = 0, segment_count = 0, " +
            "status = :failedStatus, updated_at = :updatedAt, error_message = :errorMessage " +
            "WHERE id = :id"
    )
    suspend fun markGroupFailed(
        id: String,
        errorMessage: String,
        updatedAt: Long,
        failedStatus: GenerationStatus = GenerationStatus.Failed
    ): Int

    @Query(
        "UPDATE generated_audio_records SET status = :failedStatus, " +
            "error_message = :errorMessage WHERE status = :generatingStatus"
    )
    suspend fun failInterruptedRecords(
        errorMessage: String,
        generatingStatus: GenerationStatus = GenerationStatus.Generating,
        failedStatus: GenerationStatus = GenerationStatus.Failed
    ): Int

    @Query("DELETE FROM generated_audio_records WHERE generation_group_id = :groupId")
    suspend fun deleteSegmentsForGroup(groupId: String): Int

    @Query("DELETE FROM generated_audio_groups WHERE id = :groupId")
    suspend fun deleteGroupById(groupId: String): Int

    @Transaction
    suspend fun failGroupAndDeleteSegments(groupId: String, errorMessage: String, updatedAt: Long): Int {
        deleteSegmentsForGroup(groupId)
        return markGroupFailed(groupId, errorMessage, updatedAt)
    }

    @Query("DELETE FROM generated_audio_records WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("UPDATE generated_audio_groups SET custom_title = :title, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateCustomTitle(id: String, title: String?, updatedAt: Long): Int
}
