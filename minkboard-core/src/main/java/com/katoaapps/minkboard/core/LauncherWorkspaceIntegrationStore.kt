package com.katoaapps.minkboard.core

/**
 * Atomic data-layer seam for launcher backup. A workspace graph and its designated board/intake
 * binding must never be captured or restored in separate transactions.
 */
interface LauncherWorkspaceIntegrationStore {
    suspend fun createLauncherBackupSnapshot(sourceApp: String): LauncherBackupCreationResult

    suspend fun importLauncherBackupSnapshot(
        plan: LauncherBackupRestorePlan.ImportWorkspace,
        mode: WorkspaceImportMode,
        context: LauncherBackupRestoreContext,
    ): LauncherWorkspaceImportOutcome.Imported
}
