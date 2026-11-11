package com.katoaapps.openminilaunch.model

/** What occupies an App Grid slot after its built-in default is considered. */
sealed interface AppGridAssignment {
    data object DefaultAction : AppGridAssignment

    data class Target(val selectionKey: String) : AppGridAssignment

    data class Folder(val folderId: String) : AppGridAssignment
}

internal data class AppGridTargetPlacement(
    val assignment: AppGridAssignment,
    val updatedFolder: AppFolderConfig? = null,
)

/**
 * Resolves an app-initiated Add to Home request for an App Grid slot.
 *
 * An empty/default slot becomes a direct target. Adding a different target to an occupied direct
 * slot creates a folder instead of destroying the user's existing choice. Existing folders retain
 * their identity and gain the new target.
 */
internal fun AppGridAssignment.placePinnedTarget(
    targetKey: String,
    folder: AppFolderConfig?,
    newFolderId: () -> String,
): AppGridTargetPlacement {
    val normalizedTarget = targetKey.trim()
    require(normalizedTarget.isNotEmpty()) { "Pinned target key cannot be blank" }
    return when (this) {
        AppGridAssignment.DefaultAction -> AppGridTargetPlacement(
            AppGridAssignment.Target(normalizedTarget),
        )
        is AppGridAssignment.Target -> {
            if (selectionKey == normalizedTarget) {
                AppGridTargetPlacement(this)
            } else {
                val createdFolderId = newFolderId().trim()
                require(createdFolderId.isNotEmpty()) { "Created folder ID cannot be blank" }
                val createdFolder = AppFolderConfig(
                    id = createdFolderId,
                    orderedTargetKeys = listOf(selectionKey, normalizedTarget),
                )
                AppGridTargetPlacement(
                    assignment = AppGridAssignment.Folder(createdFolder.id),
                    updatedFolder = createdFolder,
                )
            }
        }
        is AppGridAssignment.Folder -> {
            if (folder?.id != folderId) {
                AppGridTargetPlacement(AppGridAssignment.Target(normalizedTarget))
            } else if (folder.orderedTargetKeys.size >= MAX_APP_FOLDER_TARGETS) {
                AppGridTargetPlacement(this)
            } else {
                AppGridTargetPlacement(
                    assignment = this,
                    updatedFolder = folder.copy(
                        orderedTargetKeys = (folder.orderedTargetKeys + normalizedTarget).distinct(),
                    ),
                )
            }
        }
    }
}

internal fun AppGridAssignment.targetKeyOrNull(): String? =
    (this as? AppGridAssignment.Target)?.selectionKey

/**
 * Best-effort representation for builds that predate folders.
 *
 * Older OpenMink versions only understand one target per App Grid slot, so a
 * folder is represented by its first item instead of disappearing entirely.
 */
internal fun AppGridAssignment.legacyTargetKey(
    folderById: (String) -> AppFolderConfig?,
): String? = when (this) {
    AppGridAssignment.DefaultAction -> null
    is AppGridAssignment.Target -> selectionKey
    is AppGridAssignment.Folder -> folderById(folderId)?.orderedTargetKeys?.firstOrNull()
}

internal fun Map<AppGridSlot, AppGridAssignment>.normalizedAppGridAssignments(
    folderIds: Set<String>,
): Map<AppGridSlot, AppGridAssignment> = buildMap {
    this@normalizedAppGridAssignments.forEach { (slot, assignment) ->
        if (slot !in configurableAppGridSlots) return@forEach
        when (assignment) {
            AppGridAssignment.DefaultAction -> Unit
            is AppGridAssignment.Target -> assignment.selectionKey.trim()
                .takeIf(String::isNotEmpty)
                ?.let { put(slot, AppGridAssignment.Target(it)) }
            is AppGridAssignment.Folder -> assignment.folderId.trim()
                .takeIf(folderIds::contains)
                ?.let { put(slot, AppGridAssignment.Folder(it)) }
        }
    }
}

internal data class PrunedAppGridTargets(
    val assignments: Map<AppGridSlot, AppGridAssignment>,
    val folders: List<AppFolderConfig>,
)

/** Removes matching live-install targets and collapses folders that no longer need a container. */
internal fun pruneAppGridTargets(
    assignments: Map<AppGridSlot, AppGridAssignment>,
    folders: List<AppFolderConfig>,
    shouldRemove: (String) -> Boolean,
): PrunedAppGridTargets {
    val foldersById = folders.associateBy(AppFolderConfig::id)
    val retainedFolders = linkedMapOf<String, AppFolderConfig>()
    val retainedAssignments = buildMap {
        assignments.forEach { (slot, assignment) ->
            when (assignment) {
                AppGridAssignment.DefaultAction -> Unit
                is AppGridAssignment.Target -> {
                    if (!shouldRemove(assignment.selectionKey)) put(slot, assignment)
                }
                is AppGridAssignment.Folder -> {
                    val folder = foldersById[assignment.folderId] ?: return@forEach
                    val retainedTargets = folder.orderedTargetKeys.filterNot(shouldRemove)
                    when (retainedTargets.size) {
                        0 -> Unit
                        1 -> put(slot, AppGridAssignment.Target(retainedTargets.single()))
                        else -> {
                            val retainedFolder = folder.copy(orderedTargetKeys = retainedTargets)
                            retainedFolders[retainedFolder.id] = retainedFolder
                            put(slot, AppGridAssignment.Folder(retainedFolder.id))
                        }
                    }
                }
            }
        }
    }
    return PrunedAppGridTargets(retainedAssignments, retainedFolders.values.toList())
}
