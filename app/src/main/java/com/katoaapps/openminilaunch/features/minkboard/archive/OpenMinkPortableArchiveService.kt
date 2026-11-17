package com.katoaapps.openminilaunch.features.minkboard.archive

import com.katoaapps.minkboard.core.WorkspaceArchiveStore
import com.katoaapps.minkboard.core.WorkspaceImportMode
import com.katoaapps.minkboard.core.selectBoardsForExport
import com.katoaapps.minkboard.data.PassphrasePortableArchiveCodec
import com.katoaapps.minkboard.model.BoardId

/**
 * OpenMink's host boundary for portable MinkBoard files.
 *
 * The shared modules own the archive schema, validation, encryption, and additive ID remapping.
 * OpenMink owns only the product workflow and never treats a portable board archive as a launcher
 * settings backup.
 */
internal class OpenMinkPortableArchiveService(
    private val archiveStore: WorkspaceArchiveStore,
    private val codec: PassphrasePortableArchiveCodec = PassphrasePortableArchiveCodec(),
) {
    fun isSupportedArchive(bytes: ByteArray): Boolean = codec.isSupportedEnvelope(bytes)

    /** Takes ownership of [passphrase] and clears it before returning. */
    suspend fun exportBoards(
        boardIds: Set<BoardId>,
        passphrase: CharArray,
    ): ByteArray = try {
        val snapshot = archiveStore.createPortableSnapshot(SOURCE_APP)
            .selectBoardsForExport(boardIds)
        codec.encode(snapshot, passphrase)
    } finally {
        passphrase.fill('\u0000')
    }

    /**
     * Adds the archive's boards without replacing existing data.
     *
     * Takes ownership of [archiveBytes] and [passphrase], clearing both before returning.
     */
    suspend fun importBoards(
        archiveBytes: ByteArray,
        passphrase: CharArray,
    ): BoardId? = try {
        require(codec.isSupportedEnvelope(archiveBytes)) {
            "Choose an encrypted .minkboard archive"
        }
        val snapshot = codec.decode(archiveBytes, passphrase)
        // Merge is the core's additive import primitive: all incoming IDs are remapped and no
        // existing OpenMink board is replaced. The product UI intentionally calls this Import.
        archiveStore.importPortableSnapshot(snapshot, WorkspaceImportMode.Merge).firstBoardId
    } finally {
        archiveBytes.fill(0)
        passphrase.fill('\u0000')
    }

    private companion object {
        const val SOURCE_APP = "Mink Launcher Open Source"
    }
}
