package com.katoaapps.openminilaunch.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.katoaapps.openminilaunch.model.MAX_DRAWER_APPS
import com.katoaapps.openminilaunch.model.Shortcut
import com.katoaapps.openminilaunch.model.configurableShortcuts

internal class LauncherSelectionStore(private val persistence: LauncherStorePersistence) {
    val shortcutTargets = mutableStateMapOf<Shortcut, String>()
    val shortcutOrder = mutableStateListOf<Shortcut>()
    val confirmedShortcutChoices = mutableStateListOf<Shortcut>()
    val drawerTargets = mutableStateListOf<String>()
    val libraryShortcutTargets = mutableStateListOf<String>()

    fun restore(snapshot: LauncherStoreSnapshot) {
        shortcutTargets.putAll(snapshot.shortcutTargets)
        shortcutOrder.addAll(snapshot.shortcutOrder)
        confirmedShortcutChoices.addAll(snapshot.confirmedShortcutChoices)
        drawerTargets.addAll(snapshot.drawerTargets)
        libraryShortcutTargets.addAll(snapshot.libraryShortcutTargets)
    }

    fun assignShortcut(shortcut: Shortcut, targetKey: String) {
        shortcutTargets[shortcut] = targetKey
        confirmShortcut(shortcut)
        saveSelections()
    }

    fun resetShortcut(shortcut: Shortcut) {
        shortcutTargets.remove(shortcut)
        confirmShortcut(shortcut)
        saveSelections()
    }

    fun confirmSystemDefaults() {
        configurableShortcuts.forEach { shortcut ->
            if (shortcut !in confirmedShortcutChoices) {
                shortcutTargets.remove(shortcut)
                confirmedShortcutChoices += shortcut
            }
        }
        saveSelections()
    }

    fun hasConfirmedAll(): Boolean = configurableShortcuts.all(confirmedShortcutChoices::contains)

    fun moveShortcut(shortcut: Shortcut, targetIndex: Int) {
        val from = shortcutOrder.indexOf(shortcut)
        if (from < 0 || targetIndex !in shortcutOrder.indices || from == targetIndex) return
        shortcutOrder.removeAt(from)
        shortcutOrder.add(targetIndex, shortcut)
        persistence.saveShortcutOrder(shortcutOrder)
    }

    fun resetShortcutOrder() {
        shortcutOrder.clear()
        shortcutOrder.addAll(Shortcut.entries)
        persistence.saveShortcutOrder(shortcutOrder)
    }

    fun setShortcutOrder(order: List<Shortcut>) {
        if (order.size != Shortcut.entries.size || order.toSet() != Shortcut.entries.toSet()) return
        shortcutOrder.clear()
        shortcutOrder.addAll(order)
        persistence.saveShortcutOrder(shortcutOrder)
    }

    fun replaceFromBackup(
        restoredTargets: Map<Shortcut, String>,
        restoredOrder: List<Shortcut>,
        restoredConfirmedChoices: List<Shortcut>,
        restoredDrawerTargets: List<String>,
        restoredLibraryTargets: List<String>,
    ) {
        shortcutTargets.clear()
        shortcutTargets.putAll(
            restoredTargets.filter { (shortcut, target) ->
                shortcut in configurableShortcuts && target.isNotBlank()
            },
        )

        shortcutOrder.clear()
        shortcutOrder.addAll(
            restoredOrder.takeIf { it.size == Shortcut.entries.size && it.toSet() == Shortcut.entries.toSet() }
                ?: Shortcut.entries,
        )

        confirmedShortcutChoices.clear()
        confirmedShortcutChoices.addAll(
            restoredConfirmedChoices.filter { it in configurableShortcuts }.distinct(),
        )

        drawerTargets.clear()
        drawerTargets.addAll(restoredDrawerTargets.filter(String::isNotBlank).distinct().take(MAX_DRAWER_APPS))

        libraryShortcutTargets.clear()
        libraryShortcutTargets.addAll(restoredLibraryTargets.filter(String::isNotBlank).distinct())

        saveSelections()
        persistence.saveShortcutOrder(shortcutOrder)
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
        shortcutTargets.keys.toList().forEach { shortcut ->
            val saved = shortcutTargets[shortcut] ?: return@forEach
            val migrated = normalize(saved) ?: return@forEach
            if (migrated != saved) {
                shortcutTargets[shortcut] = migrated
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

    private fun confirmShortcut(shortcut: Shortcut) {
        if (shortcut !in confirmedShortcutChoices) confirmedShortcutChoices += shortcut
    }

    private fun saveSelections() {
        persistence.saveLauncherSelections(
            shortcutTargets,
            confirmedShortcutChoices,
            drawerTargets,
            libraryShortcutTargets,
        )
    }
}
