package com.katoaapps.openminilaunch.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.katoaapps.openminilaunch.model.MAX_DRAWER_APPS
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.model.configurableAppGridSlots
import com.katoaapps.openminilaunch.model.normalizedAppGridOrder

internal class LauncherSelectionStore(private val persistence: LauncherStorePersistence) {
    val appGridTargets = mutableStateMapOf<AppGridSlot, String>()
    val appGridOrder = mutableStateListOf<AppGridSlot>()
    val confirmedAppGridChoices = mutableStateListOf<AppGridSlot>()
    val drawerTargets = mutableStateListOf<String>()
    val libraryShortcutTargets = mutableStateListOf<String>()

    fun restore(snapshot: LauncherStoreSnapshot) {
        appGridTargets.putAll(snapshot.appGridTargets)
        appGridOrder.addAll(normalizedAppGridOrder(snapshot.appGridOrder))
        confirmedAppGridChoices.addAll(snapshot.confirmedAppGridChoices)
        drawerTargets.addAll(snapshot.drawerTargets)
        libraryShortcutTargets.addAll(snapshot.libraryShortcutTargets)
    }

    fun assignAppGridSlot(slot: AppGridSlot, targetKey: String) {
        appGridTargets[slot] = targetKey
        confirmAppGridSlot(slot)
        saveSelections()
    }

    fun resetAppGridSlot(slot: AppGridSlot) {
        appGridTargets.remove(slot)
        confirmAppGridSlot(slot)
        saveSelections()
    }

    fun confirmSystemDefaults() {
        configurableAppGridSlots.forEach { slot ->
            if (slot !in confirmedAppGridChoices) {
                appGridTargets.remove(slot)
                confirmedAppGridChoices += slot
            }
        }
        saveSelections()
    }

    fun hasConfirmedAll(): Boolean = configurableAppGridSlots.all(confirmedAppGridChoices::contains)

    fun moveAppGridSlot(slot: AppGridSlot, targetIndex: Int) {
        if (slot == AppGridSlot.DRAWER) return
        val reordered = appGridOrder.filterNot { it == AppGridSlot.DRAWER }.toMutableList()
        val from = reordered.indexOf(slot)
        val destination = targetIndex.coerceIn(0, reordered.lastIndex)
        if (from < 0 || from == destination) return
        reordered.removeAt(from)
        reordered.add(destination, slot)
        appGridOrder.clear()
        appGridOrder.addAll(normalizedAppGridOrder(reordered))
        persistence.saveAppGridOrder(appGridOrder)
    }

    fun resetAppGridOrder() {
        appGridOrder.clear()
        appGridOrder.addAll(AppGridSlot.entries)
        persistence.saveAppGridOrder(appGridOrder)
    }

    fun setAppGridOrder(order: List<AppGridSlot>) {
        if (order.size != AppGridSlot.entries.size || order.toSet() != AppGridSlot.entries.toSet()) return
        appGridOrder.clear()
        appGridOrder.addAll(normalizedAppGridOrder(order))
        persistence.saveAppGridOrder(appGridOrder)
    }

    fun replaceFromBackup(
        restoredTargets: Map<AppGridSlot, String>,
        restoredOrder: List<AppGridSlot>,
        restoredConfirmedChoices: List<AppGridSlot>,
        restoredDrawerTargets: List<String>,
        restoredLibraryTargets: List<String>,
    ) {
        appGridTargets.clear()
        appGridTargets.putAll(
            restoredTargets.filter { (slot, target) ->
                slot in configurableAppGridSlots && target.isNotBlank()
            },
        )

        appGridOrder.clear()
        appGridOrder.addAll(normalizedAppGridOrder(restoredOrder))

        confirmedAppGridChoices.clear()
        confirmedAppGridChoices.addAll(
            restoredConfirmedChoices.filter { it in configurableAppGridSlots }.distinct(),
        )

        drawerTargets.clear()
        drawerTargets.addAll(restoredDrawerTargets.filter(String::isNotBlank).distinct().take(MAX_DRAWER_APPS))

        libraryShortcutTargets.clear()
        libraryShortcutTargets.addAll(restoredLibraryTargets.filter(String::isNotBlank).distinct())

        saveSelections()
        persistence.saveAppGridOrder(appGridOrder)
    }

    fun toggleDrawerApp(targetKey: String) {
        if (targetKey in drawerTargets) drawerTargets.remove(targetKey)
        else if (drawerTargets.size < MAX_DRAWER_APPS) drawerTargets += targetKey
        saveSelections()
    }

    fun addDrawerTarget(targetKey: String): Boolean {
        if (drawerTargets.size >= MAX_DRAWER_APPS) return false
        drawerTargets += targetKey
        saveSelections()
        return true
    }

    fun replaceDrawerTarget(index: Int, targetKey: String): Boolean {
        if (index !in drawerTargets.indices) return false
        drawerTargets[index] = targetKey
        saveSelections()
        return true
    }

    fun addLibraryShortcut(targetKey: String) {
        if (targetKey !in libraryShortcutTargets) {
            libraryShortcutTargets += targetKey
            saveSelections()
        }
    }

    /** Converts package-only settings to personal-profile activity keys without assigning work copies. */
    fun migrateSelections(normalize: (String) -> String?) {
        var changed = false
        appGridTargets.keys.toList().forEach { slot ->
            val saved = appGridTargets[slot] ?: return@forEach
            val migrated = normalize(saved) ?: return@forEach
            if (migrated != saved) {
                appGridTargets[slot] = migrated
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

    private fun confirmAppGridSlot(slot: AppGridSlot) {
        if (slot !in confirmedAppGridChoices) confirmedAppGridChoices += slot
    }

    private fun saveSelections() {
        persistence.saveLauncherSelections(
            appGridTargets,
            confirmedAppGridChoices,
            drawerTargets,
            libraryShortcutTargets,
        )
    }
}
