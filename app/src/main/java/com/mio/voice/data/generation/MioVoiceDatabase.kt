package com.mio.voice.data.generation

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class GenerationStatusConverters {
    @TypeConverter
    fun fromStatus(status: GenerationStatus): String = status.name.lowercase()

    @TypeConverter
    fun toStatus(value: String): GenerationStatus =
        GenerationStatus.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: GenerationStatus.Failed

    @TypeConverter
    fun fromGenerationType(type: GenerationType): String = type.storageValue

    @TypeConverter
    fun toGenerationType(value: String): GenerationType =
        GenerationType.entries.firstOrNull { it.storageValue == value }
            ?: GenerationType.Legacy
}

@Database(
    entities = [
        GeneratedAudioGroup::class,
        GeneratedAudioRecord::class,
        AudioCollection::class,
        AudioCollectionItem::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(GenerationStatusConverters::class)
abstract class MioVoiceDatabase : RoomDatabase() {
    abstract fun generatedAudioDao(): GeneratedAudioDao

    abstract fun audioCollectionDao(): AudioCollectionDao

    companion object {
        @Volatile
        private var instance: MioVoiceDatabase? = null

        fun getInstance(context: Context): MioVoiceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MioVoiceDatabase::class.java,
                    "mio_voice.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `generated_audio_groups` (
                        `id` TEXT NOT NULL,
                        `original_text` TEXT NOT NULL,
                        `preview_text` TEXT,
                        `voice_id` TEXT NOT NULL,
                        `voice_name` TEXT NOT NULL,
                        `emotion` TEXT,
                        `speed` REAL NOT NULL,
                        `format` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `total_duration_ms` INTEGER NOT NULL,
                        `segment_count` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `error_message` TEXT,
                        `generation_type` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `generated_audio_groups` (
                        `id`, `original_text`, `preview_text`, `voice_id`, `voice_name`,
                        `emotion`, `speed`, `format`, `created_at`, `updated_at`,
                        `total_duration_ms`, `segment_count`, `status`, `error_message`, `generation_type`
                    )
                    SELECT
                        `id`, `text`,
                        CASE WHEN length(`text`) > 80 THEN substr(`text`, 1, 80) || '…' ELSE `text` END,
                        `voice_id`, `voice_name`, `emotion`, `speed`, `format`, `created_at`, `created_at`,
                        `duration_ms`, 1, `status`, `error_message`, 'legacy'
                    FROM `generated_audio_records`
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `generated_audio_records_new` (
                        `id` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `generation_group_id` TEXT NOT NULL,
                        `segment_index` INTEGER NOT NULL,
                        `segment_text` TEXT,
                        `local_audio_path` TEXT NOT NULL,
                        `duration_ms` INTEGER NOT NULL,
                        `voice_id` TEXT NOT NULL,
                        `voice_name` TEXT NOT NULL,
                        `emotion` TEXT,
                        `speed` REAL NOT NULL,
                        `format` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `error_message` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`generation_group_id`) REFERENCES `generated_audio_groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `generated_audio_records_new` (
                        `id`, `text`, `generation_group_id`, `segment_index`, `segment_text`,
                        `local_audio_path`, `duration_ms`, `voice_id`, `voice_name`, `emotion`,
                        `speed`, `format`, `created_at`, `status`, `error_message`
                    )
                    SELECT
                        `id`, `text`, `id`, 0, `text`, `local_audio_path`, `duration_ms`,
                        `voice_id`, `voice_name`, `emotion`, `speed`, `format`, `created_at`, `status`, `error_message`
                    FROM `generated_audio_records`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `generated_audio_records`")
                db.execSQL("ALTER TABLE `generated_audio_records_new` RENAME TO `generated_audio_records`")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_generated_audio_groups_created_at` ON `generated_audio_groups` (`created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_generated_audio_groups_status_created_at` ON `generated_audio_groups` (`status`, `created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_generated_audio_records_created_at` ON `generated_audio_records` (`created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_generated_audio_records_status` ON `generated_audio_records` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_generated_audio_records_generation_group_id` ON `generated_audio_records` (`generation_group_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_generated_audio_records_generation_group_id_segment_index` ON `generated_audio_records` (`generation_group_id`, `segment_index`)")
            }
        }

        // v3：为生成组补充服务提供商与模型两个可空列，旧记录保持 NULL（详情页显示“未知”）。
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `generated_audio_groups` ADD COLUMN `provider` TEXT")
                db.execSQL("ALTER TABLE `generated_audio_groups` ADD COLUMN `model` TEXT")
            }
        }

        // v4：语音库「组」功能，纯新增两张表，不动历史两张表。
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `audio_collections` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_audio_collections_updated_at` ON `audio_collections` (`updated_at`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `audio_collection_items` (
                        `id` TEXT NOT NULL,
                        `collection_id` TEXT NOT NULL,
                        `generation_group_id` TEXT NOT NULL,
                        `added_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`collection_id`) REFERENCES `audio_collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_audio_collection_items_collection_id` ON `audio_collection_items` (`collection_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_audio_collection_items_collection_id_generation_group_id` ON `audio_collection_items` (`collection_id`, `generation_group_id`)")
            }
        }

        // v5：为生成组补充自定义标题列（重命名功能），旧记录保持 NULL（回退到预览文本）。
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `generated_audio_groups` ADD COLUMN `custom_title` TEXT")
            }
        }
    }
}
