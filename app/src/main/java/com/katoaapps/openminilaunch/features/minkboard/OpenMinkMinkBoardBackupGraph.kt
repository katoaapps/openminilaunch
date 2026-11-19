package com.katoaapps.openminilaunch.features.minkboard

import com.katoaapps.openminilaunch.features.backup.MinkBoardLauncherBackupCoordinator
import com.katoaapps.minkboard.core.MinkBoardLauncherBackupWorkflow
import com.katoaapps.minkboard.core.MinkBoardLauncherFacade
import com.katoaapps.minkboard.data.PassphraseLauncherBackupEnvelopeCodec

/** Launcher backup wiring kept separate from interactive board runtime state. */
internal class OpenMinkMinkBoardBackupGraph(
    launcherFacade: MinkBoardLauncherFacade,
    homePreferences: LauncherBoardHomePreferences,
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
