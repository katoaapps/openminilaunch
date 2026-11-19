package com.katoaapps.minklauncher.todos.migration

import com.katoaapps.minkboard.core.LauncherBindingProblem
import com.katoaapps.minkboard.core.LauncherBoardDesignation
import com.katoaapps.minkboard.core.LauncherNormalizationResult
import com.katoaapps.minkboard.core.LegacyTodoSource
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.StatusId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyTodoMigrationTest {
    @Test
    fun nullStoredValueIsCorruptRatherThanEmpty() {
        assertEquals(LegacyTodoSource.Corrupt, parseLegacyTodoSource(null))
    }

    @Test
    fun validEmptyArrayRemainsIntentionallyAvailable() {
        assertEquals(LegacyTodoSource.Available(emptyList()), parseLegacyTodoSource("[]"))
    }

    @Test
    fun validItemsPreserveIdTextAndCompletion() {
        val result = parseLegacyTodoSource(
            """[{"id":"one","text":"Call May","completed":true}]""",
        ) as LegacyTodoSource.Available

        assertEquals(1, result.items.size)
        assertEquals("one", result.items.single().id)
        assertEquals("Call May", result.items.single().text)
        assertTrue(result.items.single().completed)
    }

    @Test
    fun malformedItemsAreCorruptRatherThanPartiallyImported() {
        assertEquals(
            LegacyTodoSource.Corrupt,
            parseLegacyTodoSource("""[{"id":"one"}]"""),
        )
    }

    @Test
    fun migratedSourceIsClearedOnlyAfterCanonicalSuccess() = runBlocking {
        val legacy = FakeLegacyTodoStore(source = LegacyTodoSource.Available(emptyList()))
        val coordinator = LegacyTodoMigrationCoordinator(
            port = FakeMigrationPort(LauncherNormalizationResult.Migrated(DESIGNATION)),
            legacyStore = legacy,
        )

        assertEquals(
            LegacyTodoMigrationResult.Migrated(DESIGNATION),
            coordinator.migrateIfNeeded(),
        )
        assertTrue(legacy.cleared)
    }

    @Test
    fun retryAfterRoomCommitClearsStaleLegacySourceWithoutDuplicating() = runBlocking {
        val legacy = FakeLegacyTodoStore(source = LegacyTodoSource.Available(emptyList()))
        val coordinator = LegacyTodoMigrationCoordinator(
            port = FakeMigrationPort(LauncherNormalizationResult.AlreadyNormalized(DESIGNATION)),
            legacyStore = legacy,
        )

        assertEquals(
            LegacyTodoMigrationResult.AlreadyMigrated(DESIGNATION),
            coordinator.migrateIfNeeded(),
        )
        assertTrue(legacy.cleared)
    }

    @Test
    fun corruptSourceIsNeverCleared() = runBlocking {
        val legacy = FakeLegacyTodoStore(source = LegacyTodoSource.Corrupt)
        val port = FakeMigrationPort(LauncherNormalizationResult.CorruptLegacyData)
        val coordinator = LegacyTodoMigrationCoordinator(
            port = port,
            legacyStore = legacy,
        )

        assertEquals(LegacyTodoMigrationResult.CorruptLegacyData, coordinator.migrateIfNeeded())
        assertEquals(1, port.callCount)
        assertFalse(legacy.cleared)
    }

    @Test
    fun brokenBindingDoesNotClearLegacySource() = runBlocking {
        val legacy = FakeLegacyTodoStore(source = LegacyTodoSource.Available(emptyList()))
        val coordinator = LegacyTodoMigrationCoordinator(
            port = FakeMigrationPort(
                LauncherNormalizationResult.BrokenBinding(
                    designation = DESIGNATION,
                    problem = LauncherBindingProblem.BOARD_MISSING,
                ),
            ),
            legacyStore = legacy,
        )

        assertEquals(
            LegacyTodoMigrationResult.BrokenBinding(
                designation = DESIGNATION,
                problem = LauncherBindingProblem.BOARD_MISSING,
            ),
            coordinator.migrateIfNeeded(),
        )
        assertFalse(legacy.cleared)
    }

    @Test
    fun failedPreferenceCleanupIsRetriedOnNextLaunch() = runBlocking {
        val legacy = FakeLegacyTodoStore(
            source = LegacyTodoSource.Available(emptyList()),
            clearSucceeds = false,
        )
        val coordinator = LegacyTodoMigrationCoordinator(
            port = FakeMigrationPort(LauncherNormalizationResult.Migrated(DESIGNATION)),
            legacyStore = legacy,
        )

        assertEquals(LegacyTodoMigrationResult.CleanupFailed, coordinator.migrateIfNeeded())
        assertTrue(legacy.cleared)
    }

    @Test
    fun cleanupFailureRecoversOnLaterExplicitAttemptWithoutReimporting() = runBlocking {
        val legacy = SequencedClearLegacyTodoStore(
            source = LegacyTodoSource.Available(emptyList()),
            clearResults = ArrayDeque(listOf(false, true)),
        )
        val port = SequencedMigrationPort(
            ArrayDeque(
                listOf(
                    LauncherNormalizationResult.Migrated(DESIGNATION),
                    LauncherNormalizationResult.AlreadyNormalized(DESIGNATION),
                ),
            ),
        )
        val coordinator = LegacyTodoMigrationCoordinator(port, legacy)

        assertEquals(LegacyTodoMigrationResult.CleanupFailed, coordinator.migrateIfNeeded())
        assertEquals(
            LegacyTodoMigrationResult.AlreadyMigrated(DESIGNATION),
            coordinator.migrateIfNeeded(),
        )
        assertEquals(2, port.callCount)
        assertEquals(2, legacy.clearCallCount)
    }

    private class FakeMigrationPort(
        private val result: LauncherNormalizationResult,
    ) : LegacyTodoMigrationPort {
        var callCount = 0
            private set

        override suspend fun normalize(source: LegacyTodoSource): LauncherNormalizationResult {
            callCount++
            return result
        }
    }

    private class SequencedMigrationPort(
        private val results: ArrayDeque<LauncherNormalizationResult>,
    ) : LegacyTodoMigrationPort {
        var callCount = 0
            private set

        override suspend fun normalize(source: LegacyTodoSource): LauncherNormalizationResult {
            callCount++
            return results.removeFirst()
        }
    }

    private class FakeLegacyTodoStore(
        private val source: LegacyTodoSource,
        private val clearSucceeds: Boolean = true,
    ) : LegacyTodoStore {
        var cleared = false
            private set

        override fun read(): LegacyTodoSource = source

        override fun clear(): Boolean {
            cleared = true
            return clearSucceeds
        }
    }

    private class SequencedClearLegacyTodoStore(
        private val source: LegacyTodoSource,
        private val clearResults: ArrayDeque<Boolean>,
    ) : LegacyTodoStore {
        var clearCallCount = 0
            private set

        override fun read(): LegacyTodoSource = source

        override fun clear(): Boolean {
            clearCallCount++
            return clearResults.removeFirst()
        }
    }

    private companion object {
        val DESIGNATION = LauncherBoardDesignation(
            generation = "generation",
            boardId = BoardId("board"),
            intakeStatusId = StatusId("status"),
            designatedAtEpochMillis = 1L,
            legacyCompatibility = null,
        )
    }
}
