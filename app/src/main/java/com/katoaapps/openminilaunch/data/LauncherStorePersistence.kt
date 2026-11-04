package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import com.katoaapps.openminilaunch.features.minkspace.todo.TodoItem
import com.katoaapps.openminilaunch.model.AppFolderConfig
import com.katoaapps.openminilaunch.model.AppGridAssignment
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.model.MAX_DRAWER_APPS
import com.katoaapps.openminilaunch.model.SearchFolder
import com.katoaapps.openminilaunch.model.WidgetGridSize
import com.katoaapps.openminilaunch.model.appGridSlotFromStoredName
import com.katoaapps.openminilaunch.model.configurableAppGridSlots
import com.katoaapps.openminilaunch.model.legacyTargetKey
import com.katoaapps.openminilaunch.model.normalizedAppGridOrder
import org.json.JSONArray
import org.json.JSONObject

/** JSON serialization for LauncherStore. This keeps storage details out of its state API. */
internal class LauncherStorePersistence(private val prefs: SharedPreferences) {
    fun loadTodos(): List<TodoItem> = runCatching {
        val array = JSONArray(prefs.getString(TODOS_KEY, "[]") ?: "[]")
        buildList {
            repeat(array.length()) { index ->
                val item = array.getJSONObject(index)
                add(TodoItem(item.getString("id"), item.getString("text"), item.optBoolean("completed")))
            }
        }
    }.getOrDefault(emptyList())

    fun load(maxSearchHistory: Int, maxWidgets: Int): LauncherStoreSnapshot = runCatching {
        val shortcuts = JSONObject(prefs.getString(SHORTCUTS_KEY, "{}") ?: "{}")
        val legacyAppGridAssignments = buildMap<AppGridSlot, AppGridAssignment> {
            AppGridSlot.entries.forEach { slot ->
                shortcuts.optString(slot.name).takeIf(String::isNotBlank)?.let { targetKey ->
                    put(slot, AppGridAssignment.Target(targetKey))
                }
            }
        }
        val appGridAssignments = if (prefs.contains(APP_GRID_ASSIGNMENTS_KEY)) {
            val assignments = JSONObject(prefs.getString(APP_GRID_ASSIGNMENTS_KEY, "{}") ?: "{}")
            buildMap<AppGridSlot, AppGridAssignment> {
                configurableAppGridSlots.forEach { slot ->
                    assignments.optJSONObject(slot.name)
                        ?.toAppGridAssignment()
                        ?.let { put(slot, it) }
                }
            }
        } else {
            legacyAppGridAssignments
        }
        val appFolders = JSONArray(prefs.getString(APP_FOLDERS_KEY, "[]") ?: "[]")
            .appFolderConfigs()

        val savedOrder = JSONArray(prefs.getString(SHORTCUT_ORDER_KEY, "[]") ?: "[]")
        val appGridOrder = normalizedAppGridOrder(buildList {
            repeat(savedOrder.length()) { index ->
                appGridSlotFromStoredName(savedOrder.optString(index))
                    ?.let { if (it !in this) add(it) }
            }
            AppGridSlot.entries.forEach { if (it !in this) add(it) }
        })

        val confirmed = JSONArray(prefs.getString(CONFIRMED_SHORTCUTS_KEY, "[]") ?: "[]")
        val confirmedChoices = buildList {
            addAll(appGridAssignments.keys.filter { it in configurableAppGridSlots })
            repeat(confirmed.length()) { index ->
                appGridSlotFromStoredName(confirmed.optString(index))
                    ?.takeIf { it in configurableAppGridSlots }
                    ?.let { if (it !in this) add(it) }
            }
        }

        val drawer = JSONArray(prefs.getString(DRAWER_KEY, "[]") ?: "[]")
        val drawerTargets = buildList {
            repeat(minOf(drawer.length(), MAX_DRAWER_APPS)) { add(drawer.getString(it)) }
        }

        val libraryShortcuts = JSONArray(prefs.getString(LIBRARY_SHORTCUTS_KEY, "[]") ?: "[]")
        val libraryShortcutTargets = buildList {
            repeat(libraryShortcuts.length()) { index ->
                libraryShortcuts.optString(index).takeIf(String::isNotBlank)?.let { if (it !in this) add(it) }
            }
        }

        val folders = JSONArray(prefs.getString(SEARCH_FOLDERS_KEY, "[]") ?: "[]")
        val searchFolders = buildList {
            repeat(folders.length()) { index ->
                val folder = folders.getJSONObject(index)
                add(SearchFolder(folder.getString("uri"), folder.getString("label")))
            }
        }

        val history = JSONArray(prefs.getString(SEARCH_HISTORY_KEY, "[]") ?: "[]")
        val searchHistory = buildList {
            repeat(minOf(history.length(), maxSearchHistory)) { index ->
                history.optString(index).trim().takeIf(String::isNotEmpty)?.let(::add)
            }
        }

        val widgets = JSONArray(prefs.getString(WIDGET_IDS_KEY, "[]") ?: "[]")
        val widgetIds = buildList {
            repeat(minOf(widgets.length(), maxWidgets)) { index ->
                widgets.optInt(index, -1).takeIf { it >= 0 }?.let(::add)
            }
        }
        val sizes = JSONObject(prefs.getString(WIDGET_SIZES_KEY, "{}") ?: "{}")
        val widgetSizes = buildMap {
            widgetIds.forEach { id ->
                sizes.optJSONObject(id.toString())?.let { saved ->
                    val columns = saved.optInt("columns", 0)
                    val rows = saved.optInt("rows", 0)
                    if (columns in 1..4 && rows in 1..5) put(id, WidgetGridSize(columns, rows))
                }
            }
        }

        LauncherStoreSnapshot(
            appGridAssignments = appGridAssignments,
            appFolders = appFolders,
            appGridOrder = appGridOrder,
            confirmedAppGridChoices = confirmedChoices,
            drawerTargets = drawerTargets,
            libraryShortcutTargets = libraryShortcutTargets,
            searchFolders = searchFolders,
            searchHistory = searchHistory,
            widgetIds = widgetIds,
            widgetSizes = widgetSizes,
            socialPackages = prefs.getStringSet(SOCIAL_PACKAGES_KEY, emptySet()).orEmpty().sorted(),
        )
    }.getOrDefault(LauncherStoreSnapshot())

    fun saveTodos(todos: List<TodoItem>) {
        val value = JSONArray().apply {
            todos.forEach { put(JSONObject().put("id", it.id).put("text", it.text).put("completed", it.completed)) }
        }
        prefs.edit().putString(TODOS_KEY, value.toString()).apply()
    }

    fun saveLauncherSelections(
        appGridAssignments: Map<AppGridSlot, AppGridAssignment>,
        appFolders: List<AppFolderConfig>,
        confirmedAppGridChoices: List<AppGridSlot>,
        drawerTargets: List<String>,
        libraryShortcutTargets: List<String>,
    ) {
        val assignments = JSONObject().apply {
            appGridAssignments.forEach { (slot, assignment) ->
                assignment.toJson()?.let { put(slot.name, it) }
            }
        }
        val foldersById = appFolders.associateBy(AppFolderConfig::id)
        // Keep the original target-only object current so pre-folder builds can restore
        // a folder slot as its first app instead of treating the slot as empty.
        val shortcuts = JSONObject().apply {
            appGridAssignments.forEach { (slot, assignment) ->
                assignment.legacyTargetKey(foldersById::get)?.let { put(slot.name, it) }
            }
        }
        val folders = JSONArray().apply {
            appFolders.forEach { folder ->
                put(JSONObject().apply {
                    put("id", folder.id)
                    folder.customName?.let { put("customName", it) }
                    put("targetKeys", JSONArray(folder.orderedTargetKeys))
                })
            }
        }
        val drawer = JSONArray().apply { drawerTargets.forEach(::put) }
        val confirmed = JSONArray().apply { confirmedAppGridChoices.forEach { put(it.name) } }
        val libraryShortcuts = JSONArray().apply { libraryShortcutTargets.forEach(::put) }
        prefs.edit()
            .putString(APP_GRID_ASSIGNMENTS_KEY, assignments.toString())
            .putString(APP_FOLDERS_KEY, folders.toString())
            .putString(SHORTCUTS_KEY, shortcuts.toString())
            .putString(CONFIRMED_SHORTCUTS_KEY, confirmed.toString())
            .putString(DRAWER_KEY, drawer.toString())
            .putString(LIBRARY_SHORTCUTS_KEY, libraryShortcuts.toString())
            .apply()
    }

    fun saveSearchFolders(folders: List<SearchFolder>) {
        val value = JSONArray().apply {
            folders.forEach { put(JSONObject().put("uri", it.uri).put("label", it.label)) }
        }
        prefs.edit().putString(SEARCH_FOLDERS_KEY, value.toString()).apply()
    }

    fun saveSearchHistory(history: List<String>) {
        prefs.edit().putString(SEARCH_HISTORY_KEY, JSONArray().apply { history.forEach(::put) }.toString()).apply()
    }

    fun saveAppGridOrder(order: List<AppGridSlot>) {
        prefs.edit().putString(
            SHORTCUT_ORDER_KEY,
            JSONArray().apply { order.forEach { put(it.name) } }.toString(),
        ).apply()
    }

    fun saveWidgets(widgetIds: List<Int>, widgetSizes: Map<Int, WidgetGridSize>) {
        val widgets = JSONArray().apply { widgetIds.forEach(::put) }
        val sizes = JSONObject().apply {
            widgetIds.forEach { id ->
                widgetSizes[id]?.let { size ->
                    put(id.toString(), JSONObject().put("columns", size.columns).put("rows", size.rows))
                }
            }
        }
        prefs.edit()
            .putString(WIDGET_IDS_KEY, widgets.toString())
            .putString(WIDGET_SIZES_KEY, sizes.toString())
            .apply()
    }

    private fun JSONObject.toAppGridAssignment(): AppGridAssignment? = when (optString("type")) {
        "target" -> optString("selectionKey")
            .takeIf(String::isNotBlank)
            ?.let(AppGridAssignment::Target)
        "folder" -> optString("folderId")
            .takeIf(String::isNotBlank)
            ?.let(AppGridAssignment::Folder)
        else -> null
    }

    private fun AppGridAssignment.toJson(): JSONObject? = when (this) {
        AppGridAssignment.DefaultAction -> null
        is AppGridAssignment.Target -> JSONObject()
            .put("type", "target")
            .put("selectionKey", selectionKey)
        is AppGridAssignment.Folder -> JSONObject()
            .put("type", "folder")
            .put("folderId", folderId)
    }

    private fun JSONArray.appFolderConfigs(): List<AppFolderConfig> = buildList {
        repeat(length()) { index ->
            val folder = optJSONObject(index) ?: return@repeat
            val id = folder.optString("id").trim()
            if (id.isEmpty() || any { it.id == id }) return@repeat
            val targets = folder.optJSONArray("targetKeys") ?: JSONArray()
            add(
                AppFolderConfig(
                    id = id,
                    customName = folder.optString("customName")
                        .ifBlank { folder.optString("name") }
                        .trim()
                        .takeIf(String::isNotEmpty),
                    orderedTargetKeys = buildList {
                        repeat(targets.length()) { targetIndex ->
                            targets.optString(targetIndex).trim().takeIf(String::isNotEmpty)?.let { key ->
                                if (key !in this) add(key)
                            }
                        }
                    },
                ),
            )
        }
    }

    private companion object {
        const val APP_FOLDERS_KEY = "app_folders"
        const val APP_GRID_ASSIGNMENTS_KEY = "app_grid_assignments"
        const val CONFIRMED_SHORTCUTS_KEY = "confirmed_shortcut_choices"
        const val DRAWER_KEY = "drawer"
        const val LIBRARY_SHORTCUTS_KEY = "library_shortcuts"
        const val SEARCH_FOLDERS_KEY = "search_folders"
        const val SEARCH_HISTORY_KEY = "search_history"
        const val SHORTCUT_ORDER_KEY = "shortcut_order"
        const val SHORTCUTS_KEY = "shortcuts"
        const val SOCIAL_PACKAGES_KEY = "social_packages"
        const val TODOS_KEY = "todos"
        const val WIDGET_IDS_KEY = "widget_ids"
        const val WIDGET_SIZES_KEY = "widget_sizes"
    }
}

internal data class LauncherStoreSnapshot(
    val appGridAssignments: Map<AppGridSlot, AppGridAssignment> = emptyMap(),
    val appFolders: List<AppFolderConfig> = emptyList(),
    val appGridOrder: List<AppGridSlot> = AppGridSlot.entries,
    val confirmedAppGridChoices: List<AppGridSlot> = emptyList(),
    val drawerTargets: List<String> = emptyList(),
    val libraryShortcutTargets: List<String> = emptyList(),
    val searchFolders: List<SearchFolder> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val widgetIds: List<Int> = emptyList(),
    val widgetSizes: Map<Int, WidgetGridSize> = emptyMap(),
    val socialPackages: List<String> = emptyList(),
)
