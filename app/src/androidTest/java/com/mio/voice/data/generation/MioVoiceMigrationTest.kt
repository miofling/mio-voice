package com.mio.voice.data.generation

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MioVoiceMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MioVoiceDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2TurnsEachLegacyRecordIntoSingleSegmentGroup() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                """
                INSERT INTO generated_audio_records (
                    id, text, local_audio_path, duration_ms, voice_id, voice_name,
                    emotion, speed, format, created_at, status, error_message
                ) VALUES (
                    'legacy-id', 'legacy text', '/files/legacy.wav', 321,
                    'voice-id', 'Mio', 'neutral', 1.0, 'wav', 123, 'success', NULL
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            2,
            true,
            MioVoiceDatabase.MIGRATION_1_2
        )

        migrated.query("SELECT * FROM generated_audio_groups WHERE id = 'legacy-id'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("legacy text", cursor.getString(cursor.getColumnIndexOrThrow("original_text")))
            assertEquals(321L, cursor.getLong(cursor.getColumnIndexOrThrow("total_duration_ms")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("segment_count")))
            assertEquals("legacy", cursor.getString(cursor.getColumnIndexOrThrow("generation_type")))
        }
        migrated.query(
            "SELECT * FROM generated_audio_records WHERE generation_group_id = 'legacy-id'"
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("segment_index")))
            assertEquals("/files/legacy.wav", cursor.getString(cursor.getColumnIndexOrThrow("local_audio_path")))
        }
        migrated.close()
    }

    @Test
    fun migrate2To3AddsProviderAndModelColumns() {
        helper.createDatabase(TEST_DATABASE, 2).apply {
            execSQL(
                """
                INSERT INTO generated_audio_groups (
                    id, original_text, preview_text, voice_id, voice_name, emotion, speed,
                    format, created_at, updated_at, total_duration_ms, segment_count,
                    status, error_message, generation_type
                ) VALUES (
                    'g1', 'hello world', 'hello', 'voice-id', 'Mio', 'neutral', 1.0,
                    'wav', 100, 100, 500, 1, 'success', NULL, 'plain_text'
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            3,
            true,
            MioVoiceDatabase.MIGRATION_2_3
        )

        migrated.query("SELECT provider, model FROM generated_audio_groups WHERE id = 'g1'").use { cursor ->
            cursor.moveToFirst()
            // 旧记录迁移后两列均为 NULL（详情页显示“未知”）。
            assertEquals(true, cursor.isNull(cursor.getColumnIndexOrThrow("provider")))
            assertEquals(true, cursor.isNull(cursor.getColumnIndexOrThrow("model")))
        }
        migrated.close()
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
    }
}
