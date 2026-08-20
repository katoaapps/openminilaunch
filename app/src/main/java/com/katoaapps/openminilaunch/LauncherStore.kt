package com.katoaapps.openminilaunch

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal fun unfinishedFirst(items: List<TodoItem>): List<TodoItem> {
    val (unfinished, completed) = items.partition { !it.completed }
    return unfinished + completed
}

class LauncherStore(context: Context) {
    private val prefs = context.getSharedPreferences("mini_launch", Context.MODE_PRIVATE)
    val todos = mutableStateListOf<TodoItem>()
    val shortcutPackages = mutableStateMapOf<Shortcut, String>()
    val shortcutOrder = mutableStateListOf<Shortcut>()
    val confirmedShortcutChoices = mutableStateListOf<Shortcut>()
    val drawerPackages = mutableStateListOf<String>()
    val searchFolders = mutableStateListOf<SearchFolder>()
    val searchHistory = mutableStateListOf<String>()
    val widgetIds = mutableStateListOf<Int>()
    val widgetSizes = mutableStateMapOf<Int, WidgetGridSize>()
    val socialPackages = mutableStateListOf<String>()
    var usesAutomaticSocialApps by mutableStateOf(!prefs.contains("social_packages"))
        private set
    var onboardingComplete by mutableStateOf(prefs.getBoolean("onboarding_complete", false))
        private set
    var themePreference by mutableStateOf(
        runCatching { ThemePreference.valueOf(prefs.getString("theme_preference", "SYSTEM") ?: "SYSTEM") }.getOrDefault(ThemePreference.SYSTEM)
    )
        private set
    var homePanelColorArgb by mutableIntStateOf(
        prefs.getInt("home_panel_color", ContextCompat.getColor(context, R.color.mink_forest))
    )
        private set
    var messageSendMode by mutableStateOf(
        runCatching {
            when (val saved = prefs.getString("message_send_mode", "ALWAYS_ASK") ?: "ALWAYS_ASK") {
                "DEFAULT_MESSENGER" -> MessageSendMode.MESSAGING_APP
                else -> MessageSendMode.valueOf(saved)
            }
        }.getOrDefault(MessageSendMode.ALWAYS_ASK)
    )
        private set
    var preferredAiPackage by mutableStateOf(prefs.getString("preferred_ai_package", null))
        private set
    var preferredWebPackage by mutableStateOf(prefs.getString("preferred_web_package", null))
        private set
    var socialGoalMinutes by mutableStateOf(
        prefs.getInt("social_goal_minutes", 60).takeIf { it in SOCIAL_GOAL_OPTIONS } ?: 60
    )
        private set

    init {
        prefs.edit()
            .remove("weather_zip")
            .remove("temperature_unit")
            .remove("weather_temperature_f")
            .remove("weather_summary")
            .remove("weather_fetched_at")
            .apply()
        load()
    }

    private fun load() {
        runCatching {
            val array = JSONArray(prefs.getString("todos", "[]") ?: "[]")
            repeat(array.length()) { index ->
                val item = array.getJSONObject(index)
                todos += TodoItem(item.getString("id"), item.getString("text"), item.optBoolean("completed"))
            }
            keepUnfinishedTodosFirst()
            val shortcuts = JSONObject(prefs.getString("shortcuts", "{}") ?: "{}")
            Shortcut.entries.forEach { shortcut ->
                shortcuts.optString(shortcut.name).takeIf(String::isNotBlank)?.let {
                    shortcutPackages[shortcut] = it
                    if (shortcut !in confirmedShortcutChoices) confirmedShortcutChoices += shortcut
                }
            }
            val savedOrder = JSONArray(prefs.getString("shortcut_order", "[]") ?: "[]")
            repeat(savedOrder.length()) { index ->
                runCatching { Shortcut.valueOf(savedOrder.getString(index)) }
                    .getOrNull()
                    ?.let { if (it !in shortcutOrder) shortcutOrder += it }
            }
            Shortcut.entries.forEach { if (it !in shortcutOrder) shortcutOrder += it }
            val confirmed = JSONArray(prefs.getString("confirmed_shortcut_choices", "[]") ?: "[]")
            repeat(confirmed.length()) { index ->
                runCatching { Shortcut.valueOf(confirmed.getString(index)) }
                    .getOrNull()
                    ?.takeIf { it in configurableShortcuts }
                    ?.let { if (it !in confirmedShortcutChoices) confirmedShortcutChoices += it }
            }
            val drawer = JSONArray(prefs.getString("drawer", "[]") ?: "[]")
            repeat(minOf(drawer.length(), MAX_DRAWER_APPS)) { drawerPackages += drawer.getString(it) }
            val folders = JSONArray(prefs.getString("search_folders", "[]") ?: "[]")
            repeat(folders.length()) { index ->
                val folder = folders.getJSONObject(index)
                searchFolders += SearchFolder(folder.getString("uri"), folder.getString("label"))
            }
            val history = JSONArray(prefs.getString("search_history", "[]") ?: "[]")
            repeat(minOf(history.length(), MAX_SEARCH_HISTORY)) { index ->
                history.optString(index).trim().takeIf(String::isNotEmpty)?.let(searchHistory::add)
            }
            val widgets = JSONArray(prefs.getString("widget_ids", "[]") ?: "[]")
            repeat(minOf(widgets.length(), MAX_WIDGETS)) { index ->
                widgets.optInt(index, -1).takeIf { it >= 0 }?.let(widgetIds::add)
            }
            val sizes = JSONObject(prefs.getString("widget_sizes", "{}") ?: "{}")
            widgetIds.forEach { id ->
                sizes.optJSONObject(id.toString())?.let { saved ->
                    val columns = saved.optInt("columns", 0)
                    val rows = saved.optInt("rows", 0)
                    if (columns in 1..4 && rows in 1..5) widgetSizes[id] = WidgetGridSize(columns, rows)
                }
            }
            socialPackages += prefs.getStringSet("social_packages", emptySet()).orEmpty().sorted()
        }
    }

    fun addTodo(text: String) {
        val clean = text.trim()
        if (clean.isNotEmpty()) {
            val firstCompletedIndex = todos.indexOfFirst(TodoItem::completed)
                .takeUnless { it == -1 }
                ?: todos.size
            todos.add(firstCompletedIndex, TodoItem(UUID.randomUUID().toString(), clean))
            saveTodos()
        }
    }

    fun toggleTodo(id: String) = updateTodo(id) { it.copy(completed = !it.completed) }
    fun renameTodo(id: String, text: String) = updateTodo(id) { it.copy(text = text.trim()) }

    private fun updateTodo(id: String, transform: (TodoItem) -> TodoItem) {
        val index = todos.indexOfFirst { it.id == id }
        if (index >= 0) {
            todos[index] = transform(todos[index])
            keepUnfinishedTodosFirst()
            saveTodos()
        }
    }

    fun deleteTodo(id: String) {
        todos.removeAll { it.id == id }
        saveTodos()
    }

    fun setTodoOrder(orderedIds: List<String>) {
        if (orderedIds.size != todos.size || orderedIds.toSet().size != todos.size) return
        val todosById = todos.associateBy { it.id }
        val reordered = unfinishedFirst(orderedIds.mapNotNull(todosById::get))
        if (reordered.size != todos.size || reordered == todos) return
        todos.clear()
        todos.addAll(reordered)
        saveTodos()
    }

    private fun keepUnfinishedTodosFirst() {
        val ordered = unfinishedFirst(todos)
        if (ordered == todos) return
        todos.clear()
        todos.addAll(ordered)
    }

    fun assignShortcut(shortcut: Shortcut, packageName: String) {
        shortcutPackages[shortcut] = packageName
        if (shortcut !in confirmedShortcutChoices) confirmedShortcutChoices += shortcut
        saveSettings()
    }

    fun resetShortcut(shortcut: Shortcut) {
        shortcutPackages.remove(shortcut)
        if (shortcut !in confirmedShortcutChoices) confirmedShortcutChoices += shortcut
        saveSettings()
    }

    fun confirmSystemDefaultsForUnselectedShortcuts() {
        configurableShortcuts.forEach { shortcut ->
            if (shortcut !in confirmedShortcutChoices) {
                shortcutPackages.remove(shortcut)
                confirmedShortcutChoices += shortcut
            }
        }
        saveSettings()
    }

    fun hasConfirmedAllShortcutChoices(): Boolean = configurableShortcuts.all(confirmedShortcutChoices::contains)

    fun moveShortcut(shortcut: Shortcut, targetIndex: Int) {
        val from = shortcutOrder.indexOf(shortcut)
        if (from < 0 || targetIndex !in shortcutOrder.indices || from == targetIndex) return
        shortcutOrder.removeAt(from)
        shortcutOrder.add(targetIndex, shortcut)
        saveShortcutOrder()
    }

    fun resetShortcutOrder() {
        shortcutOrder.clear()
        shortcutOrder.addAll(Shortcut.entries)
        saveShortcutOrder()
    }

    fun setShortcutOrder(order: List<Shortcut>) {
        if (order.size != Shortcut.entries.size || order.toSet() != Shortcut.entries.toSet()) return
        shortcutOrder.clear()
        shortcutOrder.addAll(order)
        saveShortcutOrder()
    }

    fun toggleDrawerApp(packageName: String) {
        if (packageName in drawerPackages) drawerPackages.remove(packageName)
        else if (drawerPackages.size < MAX_DRAWER_APPS) drawerPackages += packageName
        saveSettings()
    }

    fun completeOnboarding() {
        onboardingComplete = true
        prefs.edit().putBoolean("onboarding_complete", true).apply()
    }

    fun hasSeenUpdate(updateId: String): Boolean = updateId in (prefs.getStringSet("seen_updates", emptySet()) ?: emptySet())

    fun markUpdateSeen(updateId: String) {
        val seen = (prefs.getStringSet("seen_updates", emptySet()) ?: emptySet()).toMutableSet()
        seen += updateId
        prefs.edit().putStringSet("seen_updates", seen).apply()
    }

    fun setTheme(preference: ThemePreference) {
        themePreference = preference
        prefs.edit().putString("theme_preference", preference.name).apply()
    }

    fun setHomePanelColor(argb: Int) {
        homePanelColorArgb = argb or 0xFF000000.toInt()
        prefs.edit().putInt("home_panel_color", homePanelColorArgb).apply()
    }

    fun updateSocialGoalMinutes(minutes: Int) {
        if (minutes !in SOCIAL_GOAL_OPTIONS) return
        socialGoalMinutes = minutes
        prefs.edit().putInt("social_goal_minutes", minutes).apply()
    }

    fun reconcileSocialApps(installedPackages: Set<String>) {
        if (usesAutomaticSocialApps) return
        val changed = socialPackages.removeAll { it !in installedPackages }
        if (changed) prefs.edit().putStringSet("social_packages", socialPackages.toSet()).apply()
    }

    fun replaceSocialApps(packageNames: Set<String>) {
        usesAutomaticSocialApps = false
        socialPackages.clear()
        socialPackages.addAll(packageNames.sorted())
        prefs.edit().putStringSet("social_packages", socialPackages.toSet()).apply()
    }

    fun clearSocialApps() {
        usesAutomaticSocialApps = true
        socialPackages.clear()
        prefs.edit().remove("social_packages").apply()
    }

    fun updateMessageSendMode(mode: MessageSendMode) {
        messageSendMode = mode
        prefs.edit().putString("message_send_mode", mode.name).apply()
    }

    fun setPreferredAiApp(packageName: String) {
        preferredAiPackage = packageName
        prefs.edit().putString("preferred_ai_package", packageName).apply()
    }

    fun resetPreferredAiApp() {
        preferredAiPackage = null
        prefs.edit().remove("preferred_ai_package").apply()
    }

    fun setPreferredWebApp(packageName: String) {
        preferredWebPackage = packageName
        prefs.edit().putString("preferred_web_package", packageName).apply()
    }

    fun resetPreferredWebApp() {
        preferredWebPackage = null
        prefs.edit().remove("preferred_web_package").apply()
    }

    fun addSearchFolder(uri: String, label: String) {
        if (searchFolders.none { it.uri == uri }) {
            searchFolders += SearchFolder(uri, label)
            saveSearchFolders()
        }
    }

    fun removeSearchFolder(uri: String) {
        searchFolders.removeAll { it.uri == uri }
        saveSearchFolders()
    }

    fun addSearchQuery(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        searchHistory.removeAll { it.equals(clean, ignoreCase = true) }
        searchHistory.add(0, clean)
        while (searchHistory.size > MAX_SEARCH_HISTORY) searchHistory.removeAt(searchHistory.lastIndex)
        saveSearchHistory()
    }

    fun removeSearchQuery(query: String) {
        searchHistory.removeAll { it == query }
        saveSearchHistory()
    }

    fun clearSearchHistory() {
        searchHistory.clear()
        saveSearchHistory()
    }

    fun addWidget(appWidgetId: Int, size: WidgetGridSize) {
        if (appWidgetId !in widgetIds && widgetIds.size < MAX_WIDGETS) {
            widgetIds += appWidgetId
            widgetSizes[appWidgetId] = size
            saveWidgets()
        }
    }

    fun removeWidget(appWidgetId: Int) {
        widgetIds.remove(appWidgetId)
        widgetSizes.remove(appWidgetId)
        saveWidgets()
    }

    fun setWidgetSize(appWidgetId: Int, size: WidgetGridSize) {
        if (appWidgetId !in widgetIds || size.columns !in 1..4 || size.rows !in 1..5) return
        widgetSizes[appWidgetId] = size
        saveWidgets()
    }

    fun moveWidget(appWidgetId: Int, direction: Int) {
        val from = widgetIds.indexOf(appWidgetId)
        val target = from + direction
        if (from < 0 || target !in widgetIds.indices) return
        widgetIds.removeAt(from)
        widgetIds.add(target, appWidgetId)
        saveWidgets()
    }

    private fun saveTodos() {
        val value = JSONArray().apply {
            todos.forEach { put(JSONObject().put("id", it.id).put("text", it.text).put("completed", it.completed)) }
        }
        prefs.edit().putString("todos", value.toString()).apply()
    }

    private fun saveSettings() {
        val shortcuts = JSONObject().apply { shortcutPackages.forEach { (key, value) -> put(key.name, value) } }
        val drawer = JSONArray().apply { drawerPackages.forEach(::put) }
        val confirmed = JSONArray().apply { confirmedShortcutChoices.forEach { put(it.name) } }
        prefs.edit()
            .putString("shortcuts", shortcuts.toString())
            .putString("confirmed_shortcut_choices", confirmed.toString())
            .putString("drawer", drawer.toString())
            .apply()
    }

    private fun saveSearchFolders() {
        val folders = JSONArray().apply {
            searchFolders.forEach { put(JSONObject().put("uri", it.uri).put("label", it.label)) }
        }
        prefs.edit().putString("search_folders", folders.toString()).apply()
    }

    private fun saveSearchHistory() {
        val history = JSONArray().apply { searchHistory.forEach(::put) }
        prefs.edit().putString("search_history", history.toString()).apply()
    }

    private fun saveShortcutOrder() {
        val order = JSONArray().apply { shortcutOrder.forEach { put(it.name) } }
        prefs.edit().putString("shortcut_order", order.toString()).apply()
    }

    private fun saveWidgets() {
        val widgets = JSONArray().apply { widgetIds.forEach(::put) }
        val sizes = JSONObject().apply {
            widgetIds.forEach { id ->
                widgetSizes[id]?.let { size ->
                    put(id.toString(), JSONObject().put("columns", size.columns).put("rows", size.rows))
                }
            }
        }
        prefs.edit()
            .putString("widget_ids", widgets.toString())
            .putString("widget_sizes", sizes.toString())
            .apply()
    }

    private companion object {
        const val MAX_SEARCH_HISTORY = 5
        const val MAX_WIDGETS = 4
    }
}
