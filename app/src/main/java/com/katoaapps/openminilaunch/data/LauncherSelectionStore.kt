package com.katoaapps.openminilaunch.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.katoaapps.openminilaunch.model.AppFolderConfig
import com.katoaapps.openminilaunch.model.AppGridAssignment
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.model.MAX_APP_FOLDER_NAME_LENGTH
import com.katoaapps.openminilaunch.model.MAX_DRAWER_APPS
import com.katoaapps.openminilaunch.model.configurableAppGridSlots
import com.katoaapps.openminilaunch.model.normalizedAppFolders
import com.katoaapps.openminilaunch.model.normalizedAppGridAssignments
import com.katoaapps.openminilaunch.model.normalizedAppGridOrder
import com.katoaapps.openminilaunch.model.placePinnedTarget
import com.katoaapps.openminilaunch.model.pruneAppGridTargets
import com.katoaapps.openminilaunch.model.targetKeyOrNull
import java.util.UUID

internal class LauncherSelectionStore(private val persistence: LauncherStorePersistence) {
    val appGridAssignments = mutableStateMapOf<AppGridSlot, AppGridAssignment>()
    val appFolders = mutableStateListOf<AppFolderConfig>()
    val appGridOrder = mutableStateListOf<AppGridSlot>()
    val confirmedAppGridChoices = mutableStateListOf<AppGridSlot>()
    val drawerTargets = mutableStateListOf<String>()
    val libraryShortcutTargets = mutableStateListOf<String>()

    fun restore(snapshot: LauncherStoreSnapshot) {
        replaceAppGridState(snapshot.appGridAssignments, snapshot.appFolders)

        appGridOrder.clear()
        appGridOrder.addAll(normalizedAppGridOrder(snapshot.appGridOrder))

        confirmedAppGridChoices.clear()
        confirmedAppGridChoices.addAll(
            snapshot.confirmedAppGridChoices
                .filter { it in configurableAppGridSlots }
                .distinct(),
        )

        drawerTargets.clear()
        drawerTargets.addAll(snapshot.drawerTargets.normalizedTargetKeys(MAX_DRAWER_APPS))

        libraryShortcutTargets.clear()
        libraryShortcutTargets.addAll(snapshot.libraryShortcutTargets.normalizedTargetKeys())
    }

    fun placePinnedAppGridTarget(slot: AppGridSlot, targetKey: String) {
        if (slot !in configurableAppGridSlots || targetKey.isBlank()) return
        val currentAssignment = appGridAssignment(slot)
        val placement = currentAssignment.placePinnedTarget(
            targetKey = targetKey,
            folder = (currentAssignment as? AppGridAssignment.Folder)?.folderId?.let(::appFolder),
            newFolderId = { UUID.randomUUID().toString() },
        )
        placement.updatedFolder?.let { updatedFolder ->
            val folderIndex = appFolders.indexOfFirst { it.id == updatedFolder.id }
            if (folderIndex >= 0) appFolders[folderIndex] = updatedFolder else appFolders += updatedFolder
        }
        replaceAppGridAssignment(slot, placement.assignment)
        confirmAppGridSlot(slot)
        saveSelections()
    }

    fun appGridAssignment(slot: AppGridSlot): AppGridAssignment =
        appGridAssignments[slot] ?: AppGridAssignment.DefaultAction

    fun appGridTargetKey(slot: AppGridSlot): String? = appGridAssignment(slot).targetKeyOrNull()

    fun appFolder(folderId: String): AppFolderConfig? = appFolders.firstOrNull { it.id == folderId }

    fun appGridSelectionKeys(slot: AppGridSlot): List<String> = when (val assignment = appGridAssignment(slot)) {
        AppGridAssignment.DefaultAction -> emptyList()
        is AppGridAssignment.Target -> listOf(assignment.selectionKey)
        is AppGridAssignment.Folder -> appFolder(assignment.folderId)?.orderedTargetKeys.orEmpty()
    }

    fun saveAppGridSelection(
        slot: AppGridSlot,
        selectedTargetKeys: List<String>,
        folderName: String?,
    ) {
        if (slot !in configurableAppGridSlots) return
        val targets = selectedTargetKeys.map(String::trim).filter(String::isNotEmpty).distinct()
        val assignment = when (targets.size) {
            0 -> null
            1 -> AppGridAssignment.Target(targets.single())
            else -> {
                val existingFolder = (appGridAssignments[slot] as? AppGridAssignment.Folder)
                    ?.folderId
                    ?.let(::appFolder)
                val folder = AppFolderConfig(
                    id = existingFolder?.id ?: UUID.randomUUID().toString(),
                    customName = folderName
                        ?.trim()
                        ?.take(MAX_APP_FOLDER_NAME_LENGTH)
                        ?.takeIf(String::isNotEmpty),
                    orderedTargetKeys = targets,
                )
                val existingIndex = appFolders.indexOfFirst { it.id == folder.id }
                if (existingIndex >= 0) appFolders[existingIndex] = folder else appFolders += folder
                AppGridAssignment.Folder(folder.id)
            }
        }
        replaceAppGridAssignment(slot, assignment)
        confirmAppGridSlot(slot)
        saveSelections()
    }

    fun confirmSystemDefaults() {
        configurableAppGridSlots.forEach { slot ->
            if (slot !in confirmedAppGridChoices) {
                replaceAppGridAssignment(slot, null)
                confirmedAppGridChoices += slot
            }
        }
        saveSelections()
    }

    fun hasConfirmedAll(): Boolean = configurableAppGridSlots.all(confirmedAppGridChoices::contains)

    fun setAppGridOrder(order: List<AppGridSlot>) {
        if (order.size != AppGridSlot.entries.size || order.toSet() != AppGridSlot.entries.toSet()) return
        appGridOrder.clear()
        appGridOrder.addAll(normalizedAppGridOrder(order))
        persistence.saveAppGridOrder(appGridOrder)
    }

    fun replaceFromBackup(
        restoredAssignments: Map<AppGridSlot, AppGridAssignment>,
        restoredFolders: List<AppFolderConfig>,
        restoredOrder: List<AppGridSlot>,
        restoredConfirmedChoices: List<AppGridSlot>,
        restoredDrawerTargets: List<String>,
        restoredLibraryTargets: List<String>,
    ) {
        replaceAppGridState(restoredAssignments, restoredFolders)

        appGridOrder.clear()
        appGridOrder.addAll(normalizedAppGridOrder(restoredOrder))

        confirmedAppGridChoices.clear()
        confirmedAppGridChoices.addAll(
            restoredConfirmedChoices.filter { it in configurableAppGridSlots }.distinct(),
        )

        drawerTargets.clear()
        drawerTargets.addAll(restoredDrawerTargets.normalizedTargetKeys(MAX_DRAWER_APPS))

        libraryShortcutTargets.clear()
        libraryShortcutTargets.addAll(restoredLibraryTargets.normalizedTargetKeys())

        saveSelections()
        persistence.saveAppGridOrder(appGridOrder)
    }

    fun setDrawerTargets(targetKeys: List<String>): Boolean {
        val normalized = targetKeys.map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .take(MAX_DRAWER_APPS)
        if (normalized == drawerTargets.toList()) return false
        drawerTargets.clear()
        drawerTargets.addAll(normalized)
        saveSelections()
        return true
    }

    fun addDrawerTarget(targetKey: String): Boolean {
        val normalized = targetKey.trim()
        if (
            normalized.isEmpty() ||
            normalized in drawerTargets ||
            drawerTargets.size >= MAX_DRAWER_APPS
        ) return false
        drawerTargets += normalized
        saveSelections()
        return true
    }

    fun replaceDrawerTarget(index: Int, targetKey: String): Boolean {
        val normalized = targetKey.trim()
        if (
            index !in drawerTargets.indices ||
            normalized.isEmpty() ||
            drawerTargets.withIndex().any { (otherIndex, saved) ->
                otherIndex != index && saved == normalized
            }
        ) return false
        if (drawerTargets[index] == normalized) return true
        drawerTargets[index] = normalized
        saveSelections()
        return true
    }

    fun addLibraryShortcut(targetKey: String) {
        val normalized = targetKey.trim()
        if (normalized.isNotEmpty() && normalized !in libraryShortcutTargets) {
            libraryShortcutTargets += normalized
            saveSelections()
        }
    }

    /** Converts package-only settings to personal-profile activity keys without assigning work copies. */
    fun migrateSelections(normalize: (String) -> String?) {
        var changed = false
        appGridAssignments.keys.toList().forEach { slot ->
            val assignment = appGridAssignments[slot] as? AppGridAssignment.Target ?: return@forEach
            val migrated = normalize(assignment.selectionKey) ?: return@forEach
            if (migrated != assignment.selectionKey) {
                appGridAssignments[slot] = AppGridAssignment.Target(migrated)
                changed = true
            }
        }
        appFolders.indices.forEach { index ->
            val folder = appFolders[index]
            val migratedTargets = folder.orderedTargetKeys
                .map { saved -> normalize(saved) ?: saved }
                .distinct()
            if (migratedTargets != folder.orderedTargetKeys) {
                appFolders[index] = folder.copy(orderedTargetKeys = migratedTargets)
                changed = true
            }
        }
        val migratedDrawer = drawerTargets
            .map { saved -> normalize(saved) ?: saved }
            .distinct()
            .take(MAX_DRAWER_APPS)
        if (migratedDrawer != drawerTargets.toList()) {
            drawerTargets.clear()
            drawerTargets.addAll(migratedDrawer)
            changed = true
        }
        if (changed) saveSelections()
    }

    fun pruneLauncherTargets(shouldRemove: (String) -> Boolean): Boolean {
        val prunedGrid = pruneAppGridTargets(
            assignments = appGridAssignments,
            folders = appFolders,
            shouldRemove = shouldRemove,
        )
        val retainedDrawerTargets = drawerTargets.filterNot(shouldRemove)
        val retainedLibraryTargets = libraryShortcutTargets.filterNot(shouldRemove)
        val changed =
            prunedGrid.assignments != appGridAssignments.toMap() ||
                prunedGrid.folders != appFolders.toList() ||
                retainedDrawerTargets != drawerTargets.toList() ||
                retainedLibraryTargets != libraryShortcutTargets.toList()
        if (!changed) return false

        appGridAssignments.clear()
        appGridAssignments.putAll(prunedGrid.assignments)
        appFolders.clear()
        appFolders.addAll(prunedGrid.folders)
        drawerTargets.clear()
        drawerTargets.addAll(retainedDrawerTargets)
        libraryShortcutTargets.clear()
        libraryShortcutTargets.addAll(retainedLibraryTargets)
        saveSelections()
        return true
    }

    private fun confirmAppGridSlot(slot: AppGridSlot) {
        if (slot !in confirmedAppGridChoices) confirmedAppGridChoices += slot
    }

    private fun replaceAppGridState(
        restoredAssignments: Map<AppGridSlot, AppGridAssignment>,
        restoredFolders: List<AppFolderConfig>,
    ) {
        val candidateFolders = restoredFolders.normalizedAppFolders()
        val assignments = restoredAssignments.normalizedAppGridAssignments(
            candidateFolders.mapTo(mutableSetOf(), AppFolderConfig::id),
        )
        val assignedFolderIds = assignments.values.mapNotNullTo(mutableSetOf()) {
            (it as? AppGridAssignment.Folder)?.folderId
        }
        appFolders.clear()
        appFolders.addAll(candidateFolders.filter { it.id in assignedFolderIds })
        appGridAssignments.clear()
        appGridAssignments.putAll(assignments)
    }

    private fun replaceAppGridAssignment(slot: AppGridSlot, assignment: AppGridAssignment?) {
        val previousFolderId = (appGridAssignments[slot] as? AppGridAssignment.Folder)?.folderId
        if (assignment == null || assignment == AppGridAssignment.DefaultAction) {
            appGridAssignments.remove(slot)
        } else {
            appGridAssignments[slot] = assignment
        }
        if (previousFolderId != null && previousFolderId != (assignment as? AppGridAssignment.Folder)?.folderId) {
            val stillAssigned = appGridAssignments.values.any {
                it is AppGridAssignment.Folder && it.folderId == previousFolderId
            }
            if (!stillAssigned) appFolders.removeAll { it.id == previousFolderId }
        }
    }

    private fun saveSelections() {
        persistence.saveLauncherSelections(
            appGridAssignments,
            appFolders,
            confirmedAppGridChoices,
            drawerTargets,
            libraryShortcutTargets,
        )
    }

    private fun Iterable<String>.normalizedTargetKeys(limit: Int = Int.MAX_VALUE): List<String> =
        map(String::trim).filter(String::isNotEmpty).distinct().take(limit)
}
