package com.katoaapps.minklauncher.todos.backend.minkboard

import com.katoaapps.minklauncher.todos.home.TodoController
import com.katoaapps.minklauncher.todos.home.TodoHomePreferences
import com.katoaapps.minklauncher.todos.home.TodoUiPreferences
import com.katoaapps.minklauncher.todos.home.LEGACY_LAUNCHER_PREFERENCES
import com.katoaapps.minklauncher.todos.migration.FacadeLegacyTodoMigrationPort
import com.katoaapps.minklauncher.todos.migration.LegacyTodoMigrationCoordinator
import com.katoaapps.minklauncher.todos.migration.SharedPreferencesLegacyTodoStore

import android.content.Context
import com.katoaapps.minkboard.core.IdFactory
import com.katoaapps.minkboard.core.LauncherTodoNormalizer
import com.katoaapps.minkboard.core.MinkBoardLauncherFacade
import com.katoaapps.minkboard.data.AndroidMinkBoardRuntime
import java.util.UUID
import kotlinx.coroutines.CoroutineScope

/** Builds launcher-owned adapters around one application-scoped public MinkBoard runtime. */
internal class MinkBoardTodoBackend(
    context: Context,
    applicationScope: CoroutineScope,
    private val defaultBoardName: String,
) {
    private val applicationContext = context.applicationContext
    private val launcherPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        applicationContext.getSharedPreferences(
            LEGACY_LAUNCHER_PREFERENCES,
            Context.MODE_PRIVATE,
        )
    }

    val minkBoard by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AndroidMinkBoardRuntime.create(applicationContext)
    }

    val launcherFacade by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MinkBoardLauncherFacade(
            integrationStore = minkBoard.launcherIntegration,
            workspaceIntegrationStore = minkBoard.launcherWorkspaceIntegration,
            normalizer = LauncherTodoNormalizer(
                idFactory = IdFactory { UUID.randomUUID().toString() },
                clockMillis = System::currentTimeMillis,
            ),
        )
    }

    val legacyTodoMigration by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LegacyTodoMigrationCoordinator(
            port = FacadeLegacyTodoMigrationPort(launcherFacade),
            legacyStore = SharedPreferencesLegacyTodoStore(launcherPreferences),
        )
    }

    val homePreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        TodoHomePreferences(launcherPreferences)
    }

    val uiPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        TodoUiPreferences(launcherPreferences)
    }

    val launcherController by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        TodoController(
            integrationStore = minkBoard.launcherIntegration,
            launcherFacade = launcherFacade,
            legacyTodoMigration = legacyTodoMigration,
            homePreferences = homePreferences,
            defaultBoardName = defaultBoardName,
            applicationScope = applicationScope,
        )
    }
}
