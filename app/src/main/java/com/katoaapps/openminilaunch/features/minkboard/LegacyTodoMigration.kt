package com.katoaapps.openminilaunch.features.minkboard

import android.content.SharedPreferences
import com.katoaapps.minkboard.core.LauncherBindingProblem
import com.katoaapps.minkboard.core.LauncherBoardDesignation
import com.katoaapps.minkboard.core.LauncherNormalizationResult
import com.katoaapps.minkboard.core.LegacyLauncherTodo
import com.katoaapps.minkboard.core.LegacyTodoSource
import com.katoaapps.minkboard.core.MinkBoardLauncherFacade
import org.json.JSONArray

internal interface LegacyTodoMigrationPort {
    suspend fun normalize(source: LegacyTodoSource): LauncherNormalizationResult
}

internal class FacadeLegacyTodoMigrationPort(
    private val facade: MinkBoardLauncherFacade,
) : LegacyTodoMigrationPort {
    override suspend fun normalize(source: LegacyTodoSource): LauncherNormalizationResult =
        facade.normalizeLegacyTodos(
            source = source,
            sourceSchemaVersion = LEGACY_TODO_SOURCE_SCHEMA_VERSION,
        )
}

internal interface LegacyTodoStore {
    fun read(): LegacyTodoSource
    fun clear(): Boolean
}

internal class SharedPreferencesLegacyTodoStore(
    private val preferences: SharedPreferences,
) : LegacyTodoStore {
    override fun read(): LegacyTodoSource {
        if (!preferences.contains(LEGACY_TODOS_KEY)) return LegacyTodoSource.Missing
        return parseLegacyTodoSource(preferences.getString(LEGACY_TODOS_KEY, null))
    }

    override fun clear(): Boolean = preferences.edit().remove(LEGACY_TODOS_KEY).commit()
}

internal sealed interface LegacyTodoMigrationResult {
    data object NoLegacyData : LegacyTodoMigrationResult
    data object CorruptLegacyData : LegacyTodoMigrationResult
    data object CleanupFailed : LegacyTodoMigrationResult

    data class Migrated(
        val designation: LauncherBoardDesignation,
    ) : LegacyTodoMigrationResult

    data class AlreadyMigrated(
        val designation: LauncherBoardDesignation,
    ) : LegacyTodoMigrationResult

    data class BrokenBinding(
        val designation: LauncherBoardDesignation,
        val problem: LauncherBindingProblem,
    ) : LegacyTodoMigrationResult
}

/**
 * Performs the one-way launcher cutover. The legacy preference is deleted only after Room proves
 * the board is canonical. Retrying after process death is safe because an existing designation
 * returns [LauncherNormalizationResult.AlreadyNormalized] instead of inserting another board.
 */
internal class LegacyTodoMigrationCoordinator(
    private val port: LegacyTodoMigrationPort,
    private val legacyStore: LegacyTodoStore,
) {
    suspend fun migrateIfNeeded(): LegacyTodoMigrationResult {
        val source = legacyStore.read()
        return when (val normalized = port.normalize(source)) {
            LauncherNormalizationResult.NoLegacyData -> LegacyTodoMigrationResult.NoLegacyData
            LauncherNormalizationResult.CorruptLegacyData ->
                LegacyTodoMigrationResult.CorruptLegacyData
            is LauncherNormalizationResult.BrokenBinding ->
                LegacyTodoMigrationResult.BrokenBinding(
                    designation = normalized.designation,
                    problem = normalized.problem,
                )
            is LauncherNormalizationResult.Migrated -> {
                if (source is LegacyTodoSource.Available && !legacyStore.clear()) {
                    LegacyTodoMigrationResult.CleanupFailed
                } else {
                    LegacyTodoMigrationResult.Migrated(normalized.designation)
                }
            }
            is LauncherNormalizationResult.AlreadyNormalized -> {
                if (source is LegacyTodoSource.Available && !legacyStore.clear()) {
                    LegacyTodoMigrationResult.CleanupFailed
                } else {
                    LegacyTodoMigrationResult.AlreadyMigrated(normalized.designation)
                }
            }
        }
    }
}

internal fun parseLegacyTodoSource(raw: String?): LegacyTodoSource {
    if (raw == null) return LegacyTodoSource.Corrupt
    return runCatching {
        val array = JSONArray(raw)
        LegacyTodoSource.Available(
            buildList {
                repeat(array.length()) { index ->
                    val item = array.getJSONObject(index)
                    add(
                        LegacyLauncherTodo(
                            id = item.getString("id"),
                            text = item.getString("text"),
                            completed = item.optBoolean("completed", false),
                        ),
                    )
                }
            },
        )
    }.getOrElse { LegacyTodoSource.Corrupt }
}

private const val LEGACY_TODOS_KEY = "todos"
private const val LEGACY_TODO_SOURCE_SCHEMA_VERSION = 1
