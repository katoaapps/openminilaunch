package com.katoaapps.openminilaunch.features.minkboard

import android.content.Context
import com.katoaapps.openminilaunch.R
import com.katoaapps.minkboard.core.IdFactory
import com.katoaapps.minkboard.core.LauncherTodoNormalizer
import com.katoaapps.minkboard.core.MinkBoardLauncherFacade
import com.katoaapps.minkboard.data.RoomMinkBoardRepository
import java.util.UUID
import kotlinx.coroutines.CoroutineScope

/** Builds the launcher-owned adapters around one application-scoped MinkBoard repository. */
internal class OpenMinkMinkBoardGraph(
    context: Context,
    applicationScope: CoroutineScope,
) {
    private val applicationContext = context.applicationContext
    private val launcherPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        applicationContext.getSharedPreferences(
            LEGACY_LAUNCHER_PREFERENCES,
            Context.MODE_PRIVATE,
        )
    }

    val repository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        RoomMinkBoardRepository.create(applicationContext)
    }

    val launcherFacade by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MinkBoardLauncherFacade(
            integrationStore = repository,
            workspaceIntegrationStore = repository,
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
        LauncherBoardHomePreferences(launcherPreferences)
    }

    val uiPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OpenMinkBoardUiPreferences(launcherPreferences)
    }

    val launcherController by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OpenMinkLauncherBoardController(
            repository = repository,
            launcherFacade = launcherFacade,
            legacyTodoMigration = legacyTodoMigration,
            homePreferences = homePreferences,
            defaultBoardName = applicationContext.getString(R.string.todo_page_title),
            applicationScope = applicationScope,
        )
    }
}
