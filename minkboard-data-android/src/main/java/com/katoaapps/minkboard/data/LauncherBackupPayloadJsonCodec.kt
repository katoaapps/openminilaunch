package com.katoaapps.minkboard.data

import com.katoaapps.minkboard.core.LauncherBoardBackupDesignation
import com.katoaapps.minkboard.core.LauncherMinkBoardBackup
import com.katoaapps.minkboard.core.PortableArchiveValidator
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.PortableWorkspaceSnapshot
import com.katoaapps.minkboard.model.StatusId
import org.json.JSONObject

internal class UnsupportedLauncherBackupPayloadException(message: String) :
    IllegalArgumentException(message)

/** Named-field JSON for the decrypted launcher-only payload. */
internal class LauncherBackupPayloadJsonCodec(
    private val archiveValidator: PortableArchiveValidator = PortableArchiveValidator(),
) {
    private val workspaceJson = PortableArchiveJsonCodec()

    fun encode(backup: LauncherMinkBoardBackup.Present): String {
        val validated = validate(backup)
        return JSONObject()
            .put("format", PAYLOAD_FORMAT)
            .put("schemaVersion", PAYLOAD_SCHEMA_VERSION)
            .put("workspace", JSONObject(workspaceJson.encode(validated.workspace)))
            .put(
                "designation",
                validated.designation?.let(::encodeDesignation) ?: JSONObject.NULL,
            )
            .toString()
    }

    fun decode(raw: String): LauncherMinkBoardBackup.Present {
        val root = JSONObject(raw)
        val format = root.optString("format")
        require(format.isNotBlank()) { "Launcher backup payload format is missing" }
        if (format != PAYLOAD_FORMAT) {
            throw UnsupportedLauncherBackupPayloadException(
                "Unsupported launcher backup payload format",
            )
        }
        require(root.has("schemaVersion")) { "Launcher backup payload schema is missing" }
        if (root.getInt("schemaVersion") != PAYLOAD_SCHEMA_VERSION) {
            throw UnsupportedLauncherBackupPayloadException(
                "Unsupported launcher backup payload schema",
            )
        }
        val workspace = workspaceJson.decode(root.getJSONObject("workspace").toString())
        if (workspace.format != PortableWorkspaceSnapshot.FORMAT ||
            workspace.schemaVersion != PortableWorkspaceSnapshot.SCHEMA_VERSION
        ) {
            throw UnsupportedLauncherBackupPayloadException(
                "Unsupported portable workspace format or schema",
            )
        }
        require(root.has("designation")) {
            "Launcher backup payload designation is missing"
        }
        val designation = if (root.isNull("designation")) {
            null
        } else {
            decodeDesignation(root.getJSONObject("designation"))
        }
        return validate(LauncherMinkBoardBackup.Present(workspace, designation))
    }

    private fun validate(
        backup: LauncherMinkBoardBackup.Present,
    ): LauncherMinkBoardBackup.Present {
        val workspace = archiveValidator.validate(backup.workspace)
        val designation = backup.designation

        if (workspace.boards.isEmpty()) {
            require(designation == null) {
                "An empty launcher backup cannot contain a designation"
            }
            return backup.copy(workspace = workspace)
        }

        requireNotNull(designation) {
            "A populated launcher backup must contain a designation"
        }
        val board = workspace.boards.firstOrNull { it.id == designation.boardId.value }
        requireNotNull(board) {
            "Launcher backup designation board is absent"
        }
        val intake = board.statuses.firstOrNull { it.id == designation.intakeStatusId.value }
        requireNotNull(intake) {
            "Launcher backup designation intake status is absent"
        }
        require(!intake.isDoneStatus) {
            "Launcher backup designation intake status cannot be Done"
        }
        return backup.copy(workspace = workspace)
    }

    private fun encodeDesignation(designation: LauncherBoardBackupDesignation): JSONObject =
        JSONObject()
            .put("boardId", designation.boardId.value)
            .put("intakeStatusId", designation.intakeStatusId.value)

    private fun decodeDesignation(value: JSONObject): LauncherBoardBackupDesignation =
        LauncherBoardBackupDesignation(
            boardId = BoardId(value.getString("boardId")),
            intakeStatusId = StatusId(value.getString("intakeStatusId")),
        )

    companion object {
        const val PAYLOAD_FORMAT = "minkboard-launcher-backup"
        const val PAYLOAD_SCHEMA_VERSION = 1
    }
}
