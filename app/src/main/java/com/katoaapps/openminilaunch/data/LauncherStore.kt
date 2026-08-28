package com.katoaapps.openminilaunch.data

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.demo.DemoHomeData
import com.katoaapps.openminilaunch.features.demo.DemoHomeProfile
import com.katoaapps.openminilaunch.features.messaging.restoredAutomaticMessageSend
import com.katoaapps.openminilaunch.model.*

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
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("mini_launch", Context.MODE_PRIVATE)
    internal var demoHomeProfile by mutableStateOf(
        DemoHomeProfile.fromStoredName(prefs.getString(DEMO_HOME_PROFILE_KEY, null))
    )
        private set
    private var demoHomePanelColorArgb by mutableIntStateOf(
        ContextCompat.getColor(appContext, demoHomeProfile.panelColorRes)
    )
    private var demoAppBackgroundColorArgb by mutableStateOf<Int?>(
        ContextCompat.getColor(appContext, demoHomeProfile.backgroundColorRes)
    )
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
    var onboardingComplete by mutableStateOf(prefs.getBoolean(ONBOARDING_COMPLETE_KEY, false))
        private set
    var hasOpenedClockFromDate by mutableStateOf(prefs.getBoolean(CLOCK_DATE_OPENED_KEY, false))
        private set
    var openSoftwareKeyboardOnHome by mutableStateOf(
        prefs.getBoolean(OPEN_SOFTWARE_KEYBOARD_ON_HOME_KEY, true)
    )
        private set
    var themePreference by mutableStateOf(
        runCatching { ThemePreference.valueOf(prefs.getString("theme_preference", "SYSTEM") ?: "SYSTEM") }.getOrDefault(ThemePreference.SYSTEM)
    )
        private set
    var homePanelColorArgb by mutableIntStateOf(
        prefs.getInt("home_panel_color", ContextCompat.getColor(context, R.color.mink_forest))
    )
        private set
    var appBackgroundColorArgb by mutableStateOf(
        if (prefs.contains(APP_BACKGROUND_COLOR_KEY)) prefs.getInt(APP_BACKGROUND_COLOR_KEY, 0) else null
    )
        private set
    var sendMessagesAutomatically by mutableStateOf(
        restoredAutomaticMessageSend(
            saved = prefs.getBoolean(SEND_MESSAGES_AUTOMATICALLY_KEY, false)
                .takeIf { prefs.contains(SEND_MESSAGES_AUTOMATICALLY_KEY) },
            legacyMode = prefs.getString(LEGACY_MESSAGE_SEND_MODE_KEY, null),
        )
    )
        private set
    // A null package represents System Messages. Integrated providers persist the exact installed
    // package so every draft intent can remain explicit.
    var preferredMessagingPackage by mutableStateOf(prefs.getString(PREFERRED_MESSAGING_PACKAGE_KEY, null))
        private set
    var preferredAiPackage by mutableStateOf(prefs.getString("preferred_ai_package", null))
        private set
    var preferredWebPackage by mutableStateOf(prefs.getString("preferred_web_package", null))
        private set
    var socialGoalMinutes by mutableStateOf(
        normalizedSocialGoalMinutes(prefs.getInt("social_goal_minutes", 60))
    )
        private set
    val socialGoalHours: Int
        get() = socialGoalMinutes / 60
    var minkAppPauseMode by mutableStateOf(
        runCatching {
            MinkAppPauseMode.valueOf(
                prefs.getString(MINK_APP_PAUSE_MODE_KEY, MinkAppPauseMode.NEVER.name)
                    ?: MinkAppPauseMode.NEVER.name,
            )
        }.getOrDefault(MinkAppPauseMode.NEVER)
    )
        private set
    var demoSearchDataEnabled by mutableStateOf(prefs.getBoolean(DEMO_SEARCH_DATA_KEY, false))
        private set
    var githubUpdateChecksEnabled by mutableStateOf(
        prefs.getBoolean(GITHUB_UPDATE_CHECKS_ENABLED_KEY, true)
    )
        private set
    var latestGitHubReleaseTag by mutableStateOf(
        prefs.getString(LATEST_GITHUB_RELEASE_TAG_KEY, null)
    )
        private set
    val effectiveHomePanelColorArgb: Int
        get() = if (demoSearchDataEnabled) demoHomePanelColorArgb else homePanelColorArgb
    val effectiveAppBackgroundColorArgb: Int?
        get() = if (demoSearchDataEnabled) demoAppBackgroundColorArgb else appBackgroundColorArgb
    val effectiveShortcutOrder: List<Shortcut>
        get() = if (demoSearchDataEnabled) DemoHomeData.shortcutOrder(demoHomeProfile) else shortcutOrder

    init {
        prefs.edit()
            .remove("weather_zip")
            .remove("temperature_unit")
            .remove("weather_temperature_f")
            .remove("weather_summary")
            .remove("weather_fetched_at")
            .putBoolean(SEND_MESSAGES_AUTOMATICALLY_KEY, sendMessagesAutomatically)
            .putInt("social_goal_minutes", socialGoalMinutes)
            .remove(LEGACY_MESSAGE_SEND_MODE_KEY)
            .apply()
        load()
    }

    private fun load() {
        restoreSavedTodos()
        runCatching {
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
        if (demoSearchDataEnabled) applyDemoHomeProfile(demoHomeProfile)
    }

    private fun restoreSavedTodos() {
        val saved = runCatching {
            val array = JSONArray(prefs.getString("todos", "[]") ?: "[]")
            buildList {
                repeat(array.length()) { index ->
                    val item = array.getJSONObject(index)
                    add(TodoItem(item.getString("id"), item.getString("text"), item.optBoolean("completed")))
                }
            }
        }.getOrDefault(emptyList())
        todos.clear()
        todos.addAll(unfinishedFirst(saved))
    }

    private fun showDemoTodos() {
        todos.clear()
        todos.addAll(DemoHomeData.todos(demoHomeProfile))
    }

    private fun applyDemoHomeProfile(profile: DemoHomeProfile) {
        demoHomePanelColorArgb = ContextCompat.getColor(appContext, profile.panelColorRes)
        demoAppBackgroundColorArgb = ContextCompat.getColor(appContext, profile.backgroundColorRes)
        showDemoTodos()
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
        prefs.edit().putBoolean(ONBOARDING_COMPLETE_KEY, true).apply()
    }

    fun markClockOpenedFromDate() {
        hasOpenedClockFromDate = true
        prefs.edit().putBoolean(CLOCK_DATE_OPENED_KEY, true).apply()
    }

    fun updateOpenSoftwareKeyboardOnHome(enabled: Boolean) {
        openSoftwareKeyboardOnHome = enabled
        prefs.edit().putBoolean(OPEN_SOFTWARE_KEYBOARD_ON_HOME_KEY, enabled).apply()
    }

    fun setGitHubUpdateChecksEnabled(enabled: Boolean) {
        githubUpdateChecksEnabled = enabled
        val editor = prefs.edit().putBoolean(GITHUB_UPDATE_CHECKS_ENABLED_KEY, enabled)
        if (enabled) editor.remove(LAST_GITHUB_RELEASE_CHECK_KEY)
        editor.apply()
    }

    fun shouldCheckGitHubRelease(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!githubUpdateChecksEnabled) return false
        val lastCheck = prefs.getLong(LAST_GITHUB_RELEASE_CHECK_KEY, 0L)
        return nowMillis < lastCheck || nowMillis - lastCheck >= GITHUB_UPDATE_CHECK_INTERVAL_MILLIS
    }

    fun markGitHubReleaseCheckStarted(nowMillis: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(LAST_GITHUB_RELEASE_CHECK_KEY, nowMillis).apply()
    }

    fun cacheLatestGitHubReleaseTag(tag: String) {
        latestGitHubReleaseTag = tag
        prefs.edit().putString(LATEST_GITHUB_RELEASE_TAG_KEY, tag).apply()
    }

    fun toggleDemoSearchData(): Boolean {
        demoSearchDataEnabled = !demoSearchDataEnabled
        if (demoSearchDataEnabled) {
            applyDemoHomeProfile(demoHomeProfile)
        } else {
            restoreSavedTodos()
        }
        prefs.edit().putBoolean(DEMO_SEARCH_DATA_KEY, demoSearchDataEnabled).apply()
        return demoSearchDataEnabled
    }

    internal fun selectDemoHomeProfile(profile: DemoHomeProfile) {
        if (!demoSearchDataEnabled || profile == demoHomeProfile) return
        demoHomeProfile = profile
        applyDemoHomeProfile(profile)
        prefs.edit().putString(DEMO_HOME_PROFILE_KEY, profile.name).apply()
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
        val opaqueArgb = argb or 0xFF000000.toInt()
        if (demoSearchDataEnabled) {
            demoHomePanelColorArgb = opaqueArgb
            return
        }
        homePanelColorArgb = opaqueArgb
        prefs.edit().putInt("home_panel_color", homePanelColorArgb).apply()
    }

    fun setAppBackgroundColor(argb: Int?) {
        val opaqueArgb = argb?.or(0xFF000000.toInt())
        if (demoSearchDataEnabled) {
            demoAppBackgroundColorArgb = opaqueArgb
            return
        }
        appBackgroundColorArgb = opaqueArgb
        prefs.edit().apply {
            appBackgroundColorArgb?.let { putInt(APP_BACKGROUND_COLOR_KEY, it) }
                ?: remove(APP_BACKGROUND_COLOR_KEY)
        }.apply()
    }

    fun updateSocialGoalHours(hours: Int) {
        if (hours !in MIN_SOCIAL_GOAL_HOURS..MAX_SOCIAL_GOAL_HOURS) return
        socialGoalMinutes = hours * 60
        prefs.edit().putInt("social_goal_minutes", socialGoalMinutes).apply()
    }

    fun updateMinkAppPauseMode(mode: MinkAppPauseMode) {
        minkAppPauseMode = mode
        prefs.edit().putString(MINK_APP_PAUSE_MODE_KEY, mode.name).apply()
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

    fun updateSendMessagesAutomatically(enabled: Boolean) {
        sendMessagesAutomatically = enabled
        prefs.edit().putBoolean(SEND_MESSAGES_AUTOMATICALLY_KEY, enabled).apply()
    }

    fun setPreferredMessagingApp(packageName: String) {
        preferredMessagingPackage = packageName
        prefs.edit().putString(PREFERRED_MESSAGING_PACKAGE_KEY, packageName).apply()
    }

    fun resetPreferredMessagingApp() {
        preferredMessagingPackage = null
        prefs.edit().remove(PREFERRED_MESSAGING_PACKAGE_KEY).apply()
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
        if (demoSearchDataEnabled) return
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
        const val APP_BACKGROUND_COLOR_KEY = "app_background_color"
        const val CLOCK_DATE_OPENED_KEY = "clock_date_opened"
        const val DEMO_HOME_PROFILE_KEY = "demo_home_profile"
        const val DEMO_SEARCH_DATA_KEY = "demo_search_data_enabled"
        const val GITHUB_UPDATE_CHECKS_ENABLED_KEY = "github_update_checks_enabled"
        const val LAST_GITHUB_RELEASE_CHECK_KEY = "last_github_release_check"
        const val LATEST_GITHUB_RELEASE_TAG_KEY = "latest_github_release_tag"
        const val MINK_APP_PAUSE_MODE_KEY = "mink_app_pause_mode"
        const val ONBOARDING_COMPLETE_KEY = "onboarding_complete_v2"
        const val OPEN_SOFTWARE_KEYBOARD_ON_HOME_KEY = "open_software_keyboard_on_home"
        const val PREFERRED_MESSAGING_PACKAGE_KEY = "preferred_messaging_package"
        const val SEND_MESSAGES_AUTOMATICALLY_KEY = "send_messages_automatically"
        const val LEGACY_MESSAGE_SEND_MODE_KEY = "message_send_mode"
        const val GITHUB_UPDATE_CHECK_INTERVAL_MILLIS = 12 * 60 * 60 * 1_000L
        const val MAX_SEARCH_HISTORY = 5
        const val MAX_WIDGETS = 4

    }
}

internal fun normalizedSocialGoalMinutes(savedMinutes: Int): Int {
    val roundedHours = ((savedMinutes.coerceAtLeast(0) + 30) / 60)
        .coerceIn(MIN_SOCIAL_GOAL_HOURS, MAX_SOCIAL_GOAL_HOURS)
    return roundedHours * 60
}
