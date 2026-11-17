package com.katoaapps.minkboard.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MinkBoardMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MinkBoardDatabase::class.java,
    )

    @Before
    fun deleteStaleMigrationDatabase() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(DATABASE)
    }

    @After
    fun deleteMigrationDatabase() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(DATABASE)
    }

    @Test
    fun migrationOneToTwoPreservesBoardsAndAddsEmptyFocusStore() {
        helper.createDatabase(DATABASE, 1).apply {
            execSQL(
                "INSERT INTO boards (board_id, encrypted_name, is_locked, position, created_at) " +
                    "VALUES ('board', X'0102', 0, 0, 123)",
            )
            close()
        }

        helper.runMigrationsAndValidate(DATABASE, 2, true, MinkBoardDatabase.MIGRATION_1_2).use { db ->
            db.query("SELECT board_id, created_at FROM boards").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("board", cursor.getString(0))
                assertEquals(123L, cursor.getLong(1))
            }
            db.query("SELECT COUNT(*) FROM focus_sessions").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrationTwoToThreeAddsEmptyLauncherBindingWithoutChangingBoards() {
        helper.createDatabase(DATABASE, 2).apply {
            execSQL(
                "INSERT INTO boards (board_id, encrypted_name, is_locked, position, created_at) " +
                    "VALUES ('board', X'0102', 0, 0, 456)",
            )
            close()
        }

        helper.runMigrationsAndValidate(DATABASE, 3, true, MinkBoardDatabase.MIGRATION_2_3).use { db ->
            db.query("SELECT board_id, created_at FROM boards").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("board", cursor.getString(0))
                assertEquals(456L, cursor.getLong(1))
            }
            db.query("SELECT COUNT(*) FROM launcher_todo_binding").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrationOneToThreeRunsEveryRequiredStep() {
        helper.createDatabase(DATABASE, 1).close()

        helper.runMigrationsAndValidate(
            DATABASE,
            3,
            true,
            MinkBoardDatabase.MIGRATION_1_2,
            MinkBoardDatabase.MIGRATION_2_3,
        ).use { db ->
            db.query("SELECT COUNT(*) FROM focus_sessions").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM launcher_todo_binding").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrationThreeToFourAddsGenerationAndPreservesLauncherBinding() {
        helper.createDatabase(DATABASE, 3).apply {
            execSQL(
                """
                INSERT INTO launcher_todo_binding (
                    singleton_key, board_id, intake_status_id, compatibility_state,
                    source_schema_version, source_fingerprint, last_projection_fingerprint,
                    migrated_at
                ) VALUES (0, 'board', 'todo', 'CANONICAL_COMPAT', 2,
                    '${"a".repeat(64)}', '${"b".repeat(64)}', 987)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(DATABASE, 4, true, MinkBoardDatabase.MIGRATION_3_4).use { db ->
            db.query(
                """
                SELECT binding_generation, board_id, intake_status_id, compatibility_state,
                    source_schema_version, source_fingerprint, last_projection_fingerprint,
                    migrated_at
                FROM launcher_todo_binding
                """.trimIndent(),
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                val generation = cursor.getString(0)
                assertEquals(32, generation.length)
                assertEquals(true, generation.all { it in '0'..'9' || it in 'a'..'f' })
                assertEquals("board", cursor.getString(1))
                assertEquals("todo", cursor.getString(2))
                assertEquals("CANONICAL_COMPAT", cursor.getString(3))
                assertEquals(2, cursor.getInt(4))
                assertEquals("a".repeat(64), cursor.getString(5))
                assertEquals("b".repeat(64), cursor.getString(6))
                assertEquals(987L, cursor.getLong(7))
            }
        }
    }

    @Test
    fun migrationOneToFourRunsEveryRequiredStep() {
        helper.createDatabase(DATABASE, 1).close()

        helper.runMigrationsAndValidate(
            DATABASE,
            4,
            true,
            MinkBoardDatabase.MIGRATION_1_2,
            MinkBoardDatabase.MIGRATION_2_3,
            MinkBoardDatabase.MIGRATION_3_4,
        ).use { db ->
            db.query("SELECT COUNT(*) FROM launcher_todo_binding").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrationFourToFiveGeneralizesBindingWithoutLosingCompatibilityReceipt() {
        helper.createDatabase(DATABASE, 4).apply {
            execSQL(
                """
                INSERT INTO launcher_todo_binding (
                    singleton_key, binding_generation, board_id, intake_status_id,
                    compatibility_state, source_schema_version, source_fingerprint,
                    last_projection_fingerprint, migrated_at
                ) VALUES (0, 'generation-v4', 'board', 'todo', 'CANONICAL_COMPAT', 3,
                    '${"c".repeat(64)}', '${"d".repeat(64)}', 1234)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(DATABASE, 5, true, MinkBoardDatabase.MIGRATION_4_5)
            .use { db ->
                db.query(
                    """
                    SELECT designation_generation, board_id, intake_status_id, designated_at,
                        compatibility_state, source_schema_version, source_fingerprint,
                        last_projection_fingerprint, migrated_at
                    FROM launcher_board_designation
                    """.trimIndent(),
                ).use { cursor ->
                    assertEquals(true, cursor.moveToFirst())
                    assertEquals("generation-v4", cursor.getString(0))
                    assertEquals("board", cursor.getString(1))
                    assertEquals("todo", cursor.getString(2))
                    assertEquals(1234L, cursor.getLong(3))
                    assertEquals("CANONICAL_COMPAT", cursor.getString(4))
                    assertEquals(3, cursor.getInt(5))
                    assertEquals("c".repeat(64), cursor.getString(6))
                    assertEquals("d".repeat(64), cursor.getString(7))
                    assertEquals(1234L, cursor.getLong(8))
                }
                db.query(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' " +
                        "AND name = 'launcher_todo_binding'",
                ).use { cursor ->
                    assertEquals(true, cursor.moveToFirst())
                    assertEquals(0, cursor.getInt(0))
                }
            }
    }

    @Test
    fun migrationOneToFiveRunsEveryRequiredStep() {
        helper.createDatabase(DATABASE, 1).close()

        helper.runMigrationsAndValidate(
            DATABASE,
            5,
            true,
            MinkBoardDatabase.MIGRATION_1_2,
            MinkBoardDatabase.MIGRATION_2_3,
            MinkBoardDatabase.MIGRATION_3_4,
            MinkBoardDatabase.MIGRATION_4_5,
        ).use { db ->
            db.query("SELECT COUNT(*) FROM launcher_board_designation").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private companion object { const val DATABASE = "minkboard-migration-test" }
}
