package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.minkboard.model.SubtaskId

data class WorkspaceImportIdMap(
    val boardIds: Map<String, BoardId>,
    val statusIds: Map<String, StatusId>,
    val cardIds: Map<String, CardId>,
    val subtaskIds: Map<String, SubtaskId>,
)

data class WorkspaceImportResult(
    val firstBoardId: BoardId?,
    val ids: WorkspaceImportIdMap,
)

/**
 * Plaintext snapshot boundary for the repository. Callers must encrypt snapshots before writing
 * them outside the app and must fully decode and validate an archive before importing it.
 */
interface WorkspaceArchiveStore {
    suspend fun createPortableSnapshot(sourceApp: String): PortableWorkspaceSnapshot

    /**
     * Imports the complete snapshot in one transaction and returns every portable-to-local ID
     * mapping. Merge remaps incoming IDs. Replace preserves them after validation. Host metadata
     * must use this result rather than guessing that a portable ID survived import.
     */
    suspend fun importPortableSnapshot(
        snapshot: PortableWorkspaceSnapshot,
        mode: WorkspaceImportMode,
    ): WorkspaceImportResult
}
