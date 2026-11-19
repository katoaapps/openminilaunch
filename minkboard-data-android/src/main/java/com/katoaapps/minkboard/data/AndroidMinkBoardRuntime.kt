package com.katoaapps.minkboard.data

import android.content.Context
import android.os.SystemClock
import com.katoaapps.minkboard.core.BoardAccessSessionController
import com.katoaapps.minkboard.core.BoardTemplate
import com.katoaapps.minkboard.core.BoardTemplateId
import com.katoaapps.minkboard.core.LauncherBoardIntegrationStore
import com.katoaapps.minkboard.core.LauncherWorkspaceIntegrationStore
import com.katoaapps.minkboard.core.MinkBoardAccess
import com.katoaapps.minkboard.core.MinkBoardApi
import com.katoaapps.minkboard.core.MinkBoardBackup
import com.katoaapps.minkboard.core.MinkBoardBoards
import com.katoaapps.minkboard.core.MinkBoardCards
import com.katoaapps.minkboard.core.MinkBoardFocus
import com.katoaapps.minkboard.core.MinkBoardStatuses
import com.katoaapps.minkboard.core.MinkBoardSubtasks
import com.katoaapps.minkboard.core.MinkBoardTemplateCatalog
import com.katoaapps.minkboard.core.MinkBoardTemplates
import com.katoaapps.minkboard.core.MinkBoardTransfer
import com.katoaapps.minkboard.core.WorkspaceImportMode
import com.katoaapps.minkboard.core.WorkspaceImportResult
import com.katoaapps.minkboard.core.selectBoardsForExport
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot

/**
 * Ready-to-embed Android composition root for the complete MinkBoard public API.
 *
 * The consuming application owns this object's lifecycle. It receives public capabilities rather
 * than Room, DAO, cipher, or entity types. Launcher-only compatibility seams are exposed
 * separately so general consumers never need to know they exist.
 */
class AndroidMinkBoardRuntime private constructor(
    private val roomStore: RoomMinkBoardRepository,
    archiveCodec: PassphrasePortableArchiveCodec,
) : MinkBoardApi {
    override val boards: MinkBoardBoards = roomStore
    override val statuses: MinkBoardStatuses = roomStore
    override val cards: MinkBoardCards = roomStore
    override val subtasks: MinkBoardSubtasks = roomStore
    override val focus: MinkBoardFocus = roomStore
    override val access: MinkBoardAccess = AndroidMinkBoardAccess
    override val templates: MinkBoardTemplateCatalog = DefaultMinkBoardTemplateCatalog
    override val backup: MinkBoardBackup = RoomMinkBoardBackup(roomStore)
    override val transfer: MinkBoardTransfer = RoomMinkBoardTransfer(
        archiveStore = roomStore,
        archiveCodec = archiveCodec,
    )

    /** Optional launcher adapter. General embedded consumers should use [boards] and [cards]. */
    val launcherIntegration: LauncherBoardIntegrationStore = roomStore

    /** Optional atomic whole-launcher backup adapter. */
    val launcherWorkspaceIntegration: LauncherWorkspaceIntegrationStore = roomStore

    companion object {
        fun create(context: Context): AndroidMinkBoardRuntime = AndroidMinkBoardRuntime(
            roomStore = RoomMinkBoardRepository.create(context.applicationContext),
            archiveCodec = PassphrasePortableArchiveCodec(),
        )
    }
}

private object AndroidMinkBoardAccess : MinkBoardAccess {
    override fun createSession(backgroundTimeoutMillis: Long) = BoardAccessSessionController(
        backgroundTimeoutMillis = backgroundTimeoutMillis,
        elapsedRealtimeMillis = SystemClock::elapsedRealtime,
    )
}

private object DefaultMinkBoardTemplateCatalog : MinkBoardTemplateCatalog {
    override val templates: List<BoardTemplate> = MinkBoardTemplates.all

    override fun require(templateId: BoardTemplateId): BoardTemplate =
        MinkBoardTemplates.require(templateId)
}

private class RoomMinkBoardBackup(
    private val archiveStore: RoomMinkBoardRepository,
) : MinkBoardBackup {
    override suspend fun createSnapshot(sourceApp: String): PortableWorkspaceSnapshot =
        archiveStore.createPortableSnapshot(sourceApp)

    override suspend fun importSnapshot(
        snapshot: PortableWorkspaceSnapshot,
    ): WorkspaceImportResult = archiveStore.importPortableSnapshot(
        snapshot = snapshot,
        mode = WorkspaceImportMode.Merge,
    )
}
private class RoomMinkBoardTransfer(
    private val archiveStore: RoomMinkBoardRepository,
    private val archiveCodec: PassphrasePortableArchiveCodec,
) : MinkBoardTransfer {
    override fun isSupportedArchive(archive: ByteArray): Boolean =
        archiveCodec.isSupportedEnvelope(archive)

    override suspend fun exportBoards(
        boardIds: Set<BoardId>,
        sourceApp: String,
        passphrase: CharArray,
    ): ByteArray = archiveCodec.encode(
        snapshot = archiveStore.createPortableSnapshot(sourceApp).selectBoardsForExport(boardIds),
        passphrase = passphrase,
    )

    override suspend fun importBoards(
        archive: ByteArray,
        passphrase: CharArray,
    ): WorkspaceImportResult = archiveStore.importPortableSnapshot(
        snapshot = archiveCodec.decode(archive, passphrase),
        mode = WorkspaceImportMode.Merge,
    )
}
