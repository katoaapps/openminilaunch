package com.katoaapps.minkboard.model

/**
 * The decrypted, portable representation of a MinkBoard workspace.
 *
 * These records deliberately use strings for IDs and dates. They are a versioned interchange
 * contract, not the local Room schema, and must pass PortableArchiveValidator before import.
 */
data class PortableWorkspaceSnapshot(
    val format: String = FORMAT,
    val schemaVersion: Int = SCHEMA_VERSION,
    val exportedAtEpochMillis: Long,
    val sourceApp: String,
    val boards: List<PortableBoardSnapshot>,
) {
    companion object {
        const val FORMAT = "minkboard-portable"
        const val SCHEMA_VERSION = 1
    }
}

data class PortableBoardSnapshot(
    val id: String,
    val name: String,
    val isLocked: Boolean,
    val position: Int,
    val createdAtEpochMillis: Long,
    val statuses: List<PortableStatusSnapshot>,
)

data class PortableStatusSnapshot(
    val id: String,
    val name: String,
    val colorArgb: Int,
    val isDoneStatus: Boolean,
    val position: Int,
    val cards: List<PortableCardSnapshot>,
)

data class PortableCardSnapshot(
    val id: String,
    val title: String,
    val details: String,
    val dueDateIso: String?,
    val isMajor: Boolean,
    val position: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val subtasks: List<PortableSubtaskSnapshot>,
)

data class PortableSubtaskSnapshot(
    val id: String,
    val text: String,
    val isCompleted: Boolean,
    val position: Int,
)
