package com.katoaapps.openminilaunch.features.backup

import com.katoaapps.minklauncher.todos.home.TodoHomePreferences

import com.katoaapps.minkboard.core.MinkBoardLauncherBackupWorkflow
import com.katoaapps.minkboard.core.MinkBoardLauncherFacade
import com.katoaapps.minkboard.data.PassphraseLauncherBackupEnvelopeCodec

/** Launcher backup wiring kept separate from interactive board runtime state. */
internal class TodoBoardBackupBridge(
    launcherFacade: MinkBoardLauncherFacade,
    homePreferences: TodoHomePreferences,
) {
    val workflow by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MinkBoardLauncherBackupWorkflow(
            facade = launcherFacade,
            envelopeCodec = PassphraseLauncherBackupEnvelopeCodec(),
        )
    }

    val coordinator by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MinkBoardLauncherBackupCoordinator(
            workflow = workflow,
            facade = launcherFacade,
            homePreferences = homePreferences,
        )
    }
}
