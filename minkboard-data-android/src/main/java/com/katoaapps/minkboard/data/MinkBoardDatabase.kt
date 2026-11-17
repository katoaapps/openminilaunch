package com.katoaapps.minkboard.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BoardEntity::class,
        StatusEntity::class,
        CardEntity::class,
        SubtaskEntity::class,
        FocusSessionEntity::class,
        LauncherBoardDesignationEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
internal abstract class MinkBoardDatabase : RoomDatabase() {
    abstract fun boardDao(): MinkBoardDao

    companion object {
        fun create(context: Context): MinkBoardDatabase = Room.databaseBuilder(
            context.applicationContext,
            MinkBoardDatabase::class.java,
            "minkboard.db",
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_sessions` (
                        `singleton_key` INTEGER NOT NULL,
                        `timer_id` TEXT NOT NULL,
                        `board_id` TEXT NOT NULL,
                        `card_id` TEXT NOT NULL,
                        `started_at` INTEGER NOT NULL,
                        `ends_at` INTEGER NOT NULL,
                        `state` TEXT NOT NULL,
                        PRIMARY KEY(`singleton_key`),
                        FOREIGN KEY(`board_id`) REFERENCES `boards`(`board_id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`card_id`) REFERENCES `cards`(`card_id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_focus_sessions_board_id` ON `focus_sessions` (`board_id`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_focus_sessions_card_id` ON `focus_sessions` (`card_id`)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `launcher_todo_binding` (
                        `singleton_key` INTEGER NOT NULL,
                        `board_id` TEXT NOT NULL,
                        `intake_status_id` TEXT NOT NULL,
                        `compatibility_state` TEXT NOT NULL,
                        `source_schema_version` INTEGER NOT NULL,
                        `source_fingerprint` TEXT NOT NULL,
                        `last_projection_fingerprint` TEXT,
                        `migrated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`singleton_key`)
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `launcher_todo_binding_v4` (
                        `singleton_key` INTEGER NOT NULL,
                        `binding_generation` TEXT NOT NULL,
                        `board_id` TEXT NOT NULL,
                        `intake_status_id` TEXT NOT NULL,
                        `compatibility_state` TEXT NOT NULL,
                        `source_schema_version` INTEGER NOT NULL,
                        `source_fingerprint` TEXT NOT NULL,
                        `last_projection_fingerprint` TEXT,
                        `migrated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`singleton_key`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `launcher_todo_binding_v4` (
                        `singleton_key`, `binding_generation`, `board_id`, `intake_status_id`,
                        `compatibility_state`, `source_schema_version`, `source_fingerprint`,
                        `last_projection_fingerprint`, `migrated_at`
                    )
                    SELECT `singleton_key`, lower(hex(randomblob(16))), `board_id`, `intake_status_id`,
                        `compatibility_state`, `source_schema_version`, `source_fingerprint`,
                        `last_projection_fingerprint`, `migrated_at`
                    FROM `launcher_todo_binding`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `launcher_todo_binding`")
                db.execSQL(
                    "ALTER TABLE `launcher_todo_binding_v4` RENAME TO `launcher_todo_binding`",
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `launcher_board_designation` (
                        `singleton_key` INTEGER NOT NULL,
                        `designation_generation` TEXT NOT NULL,
                        `board_id` TEXT NOT NULL,
                        `intake_status_id` TEXT NOT NULL,
                        `designated_at` INTEGER NOT NULL,
                        `compatibility_state` TEXT,
                        `source_schema_version` INTEGER,
                        `source_fingerprint` TEXT,
                        `last_projection_fingerprint` TEXT,
                        `migrated_at` INTEGER,
                        PRIMARY KEY(`singleton_key`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `launcher_board_designation` (
                        `singleton_key`, `designation_generation`, `board_id`, `intake_status_id`,
                        `designated_at`, `compatibility_state`, `source_schema_version`,
                        `source_fingerprint`, `last_projection_fingerprint`, `migrated_at`
                    )
                    SELECT `singleton_key`, `binding_generation`, `board_id`, `intake_status_id`,
                        `migrated_at`, `compatibility_state`, `source_schema_version`,
                        `source_fingerprint`, `last_projection_fingerprint`, `migrated_at`
                    FROM `launcher_todo_binding`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `launcher_todo_binding`")
            }
        }
    }
}
