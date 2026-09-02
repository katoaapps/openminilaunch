package com.katoaapps.openminilaunch.data

import com.katoaapps.openminilaunch.features.demo.DemoHomeProfile
import com.katoaapps.openminilaunch.model.*

import android.content.Context

class LauncherStore private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("mini_launch", Context.MODE_PRIVATE)
    private val persistence = LauncherStorePersistence(prefs)
    private val todoStore: TodoStore = TodoStore(persistence) { demoSearchDataEnabled }
    private val launcherSelections = LauncherSelectionStore(persistence)
    private val searchStore = SearchStore(persistence, MAX_SEARCH_HISTORY)
    private val widgetStore = WidgetStore(persistence, MAX_WIDGETS)
    private val generalPreferences = GeneralPreferenceStore(prefs)
    private val appearancePreferences = AppearancePreferenceStore(appContext, prefs)
    private val messagingPreferences = MessagingPreferenceStore(prefs)
    private val minkDayPreferences = MinkDayPreferenceStore(prefs)
    private val updatePreferences = UpdatePreferenceStore(prefs)
    private val pinShortcutPreferences = PinShortcutPreferenceStore(prefs)
    private val demoPreferences: DemoPreferenceStore = DemoPreferenceStore(appContext, prefs, todoStore)
    internal val demoHomeProfile get() = demoPreferences.profile
    val todos get() = todoStore.items
    /** Stable launcher target keys. Legacy package-only values are migrated after discovery. */
    val shortcutTargets get() = launcherSelections.shortcutTargets
    val shortcutOrder get() = launcherSelections.shortcutOrder
    val confirmedShortcutChoices get() = launcherSelections.confirmedShortcutChoices
    /** Stable launcher target keys, allowing personal and work copies of one package to coexist. */
    val drawerTargets get() = launcherSelections.drawerTargets
    val searchFolders get() = searchStore.folders
    val searchHistory get() = searchStore.history
    val widgetIds get() = widgetStore.ids
    val widgetSizes get() = widgetStore.sizes
    val socialPackages get() = minkDayPreferences.socialPackages
    val usesAutomaticSocialApps get() = minkDayPreferences.usesAutomaticSocialApps
    val onboardingComplete get() = generalPreferences.onboardingComplete
    val hasOpenedClockFromDate get() = generalPreferences.hasOpenedClockFromDate
    val openSoftwareKeyboardOnHome get() = generalPreferences.openSoftwareKeyboardOnHome
    val themePreference get() = appearancePreferences.themePreference
    val hideStatusBar get() = appearancePreferences.hideStatusBar
    val alignHomePanelBottom get() = appearancePreferences.alignHomePanelBottom
    val homePanelColorArgb get() = appearancePreferences.homePanelColorArgb
    val appBackgroundColorArgb get() = appearancePreferences.appBackgroundColorArgb
    val sendMessagesAutomatically get() = messagingPreferences.sendMessagesAutomatically
    val preferredMessagingPackage get() = messagingPreferences.preferredMessagingPackage
    val preferredAiPackage get() = messagingPreferences.preferredAiPackage
    val preferredWebPackage get() = messagingPreferences.preferredWebPackage
    val socialGoalMinutes get() = minkDayPreferences.socialGoalMinutes
    val socialGoalHours get() = minkDayPreferences.socialGoalHours
    val minkAppPauseMode get() = minkDayPreferences.pauseMode
    val demoSearchDataEnabled get() = demoPreferences.enabled
    val githubUpdateChecksEnabled get() = updatePreferences.checksEnabled
    val latestGitHubReleaseTag get() = updatePreferences.latestReleaseTag
    val pinShortcutRequestPresentation get() = pinShortcutPreferences.requestPresentation
    val effectiveHomePanelColorArgb: Int
        get() = demoPreferences.effectivePanelColor(homePanelColorArgb)
    val effectiveAppBackgroundColorArgb: Int?
        get() = demoPreferences.effectiveBackgroundColor(appBackgroundColorArgb)
    val effectiveShortcutOrder: List<Shortcut>
        get() = demoPreferences.effectiveShortcutOrder(shortcutOrder)

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
        todoStore.restoreSaved()
        val saved = persistence.load(MAX_SEARCH_HISTORY, MAX_WIDGETS)
        launcherSelections.restore(saved)
        searchStore.restore(saved)
        widgetStore.restore(saved)
        minkDayPreferences.restore(saved.socialPackages)
        demoPreferences.applyIfEnabled()
    }

    fun addTodo(text: String) = todoStore.add(text)

    fun toggleTodo(id: String) = todoStore.toggle(id)

    fun renameTodo(id: String, text: String) = todoStore.rename(id, text)

    fun deleteTodo(id: String) = todoStore.delete(id)

    fun setTodoOrder(orderedIds: List<String>) = todoStore.setOrder(orderedIds)

    fun assignShortcut(shortcut: Shortcut, targetKey: String) {
        launcherSelections.assignShortcut(shortcut, targetKey)
    }

    fun resetShortcut(shortcut: Shortcut) {
        launcherSelections.resetShortcut(shortcut)
    }

    fun confirmSystemDefaultsForUnselectedShortcuts() {
        launcherSelections.confirmSystemDefaults()
    }

    fun hasConfirmedAllShortcutChoices(): Boolean = launcherSelections.hasConfirmedAll()

    fun moveShortcut(shortcut: Shortcut, targetIndex: Int) {
        launcherSelections.moveShortcut(shortcut, targetIndex)
    }

    fun resetShortcutOrder() {
        launcherSelections.resetShortcutOrder()
    }

    fun setShortcutOrder(order: List<Shortcut>) {
        launcherSelections.setShortcutOrder(order)
    }

    fun toggleDrawerApp(targetKey: String) {
        launcherSelections.toggleDrawerApp(targetKey)
    }

    fun addDrawerTarget(targetKey: String): Boolean = launcherSelections.addDrawerTarget(targetKey)

    fun replaceDrawerTarget(index: Int, targetKey: String): Boolean =
        launcherSelections.replaceDrawerTarget(index, targetKey)

    fun setPinShortcutRequestPresentation(presentation: PinShortcutRequestPresentation) {
        pinShortcutPreferences.updateRequestPresentation(presentation)
    }

    /** Converts package-only settings to personal-profile activity keys without assigning work copies. */
    fun migrateLauncherSelections(normalize: (String) -> String?) {
        launcherSelections.migrateSelections(normalize)
    }

    fun completeOnboarding() {
        generalPreferences.completeOnboarding()
    }

    fun markClockOpenedFromDate() {
        generalPreferences.markClockOpenedFromDate()
    }

    fun updateOpenSoftwareKeyboardOnHome(enabled: Boolean) {
        generalPreferences.updateOpenSoftwareKeyboardOnHome(enabled)
    }

    fun setGitHubUpdateChecksEnabled(enabled: Boolean) {
        updatePreferences.updateChecksEnabled(enabled)
    }

    fun shouldCheckGitHubRelease(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return updatePreferences.shouldCheck(nowMillis)
    }

    fun markGitHubReleaseCheckStarted(nowMillis: Long = System.currentTimeMillis()) {
        updatePreferences.markCheckStarted(nowMillis)
    }

    fun cacheLatestGitHubReleaseTag(tag: String) {
        updatePreferences.cacheLatestReleaseTag(tag)
    }

    fun shouldShowGitHubUpdateReminder(
        releaseTag: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        return updatePreferences.shouldShowReminder(releaseTag, nowMillis)
    }

    fun snoozeGitHubUpdateReminder(
        releaseTag: String,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        updatePreferences.snoozeReminder(releaseTag, nowMillis)
    }

    fun toggleDemoSearchData(): Boolean {
        return demoPreferences.toggle()
    }

    internal fun selectDemoHomeProfile(profile: DemoHomeProfile) {
        demoPreferences.selectProfile(profile)
    }

    fun hasSeenUpdate(updateId: String): Boolean = generalPreferences.hasSeenUpdate(updateId)

    fun markUpdateSeen(updateId: String) {
        generalPreferences.markUpdateSeen(updateId)
    }

    fun setTheme(preference: ThemePreference) {
        appearancePreferences.setTheme(preference)
    }

    fun updateHideStatusBar(enabled: Boolean) {
        appearancePreferences.updateHideStatusBar(enabled)
    }

    fun updateAlignHomePanelBottom(enabled: Boolean) {
        appearancePreferences.updateAlignHomePanelBottom(enabled)
    }

    fun setHomePanelColor(argb: Int) {
        val opaqueArgb = argb or 0xFF000000.toInt()
        if (demoPreferences.setPanelColor(opaqueArgb)) return
        appearancePreferences.setHomePanelColor(opaqueArgb)
    }

    fun setAppBackgroundColor(argb: Int?) {
        val opaqueArgb = argb?.or(0xFF000000.toInt())
        if (demoPreferences.setBackgroundColor(opaqueArgb)) return
        appearancePreferences.setAppBackgroundColor(opaqueArgb)
    }

    fun updateSocialGoalHours(hours: Int) {
        minkDayPreferences.updateGoalHours(hours)
    }

    fun updateMinkAppPauseMode(mode: MinkAppPauseMode) {
        minkDayPreferences.updatePauseMode(mode)
    }

    fun reconcileSocialApps(installedPackages: Set<String>) {
        minkDayPreferences.reconcileApps(installedPackages)
    }

    fun replaceSocialApps(packageNames: Set<String>) {
        minkDayPreferences.replaceApps(packageNames)
    }

    fun clearSocialApps() {
        minkDayPreferences.clearApps()
    }

    fun updateSendMessagesAutomatically(enabled: Boolean) {
        messagingPreferences.updateAutomaticSend(enabled)
    }

    fun setPreferredMessagingApp(packageName: String) {
        messagingPreferences.setPreferredMessagingApp(packageName)
    }

    fun resetPreferredMessagingApp() {
        messagingPreferences.setPreferredMessagingApp(null)
    }

    fun setPreferredAiApp(packageName: String) {
        messagingPreferences.setPreferredAiApp(packageName)
    }

    fun resetPreferredAiApp() {
        messagingPreferences.setPreferredAiApp(null)
    }

    fun setPreferredWebApp(packageName: String) {
        messagingPreferences.setPreferredWebApp(packageName)
    }

    fun resetPreferredWebApp() {
        messagingPreferences.setPreferredWebApp(null)
    }

    fun addSearchFolder(uri: String, label: String) {
        searchStore.addFolder(uri, label)
    }

    fun removeSearchFolder(uri: String) {
        searchStore.removeFolder(uri)
    }

    fun addSearchQuery(query: String) {
        searchStore.addQuery(query)
    }

    fun removeSearchQuery(query: String) {
        searchStore.removeQuery(query)
    }

    fun clearSearchHistory() {
        searchStore.clearHistory()
    }

    fun addWidget(appWidgetId: Int, size: WidgetGridSize) {
        widgetStore.add(appWidgetId, size)
    }

    fun removeWidget(appWidgetId: Int) {
        widgetStore.remove(appWidgetId)
    }

    fun setWidgetSize(appWidgetId: Int, size: WidgetGridSize) {
        widgetStore.setSize(appWidgetId, size)
    }

    fun moveWidget(appWidgetId: Int, direction: Int) {
        widgetStore.move(appWidgetId, direction)
    }

    companion object {
        private const val MAX_SEARCH_HISTORY = 5
        private const val MAX_WIDGETS = 4

        @Volatile
        private var instance: LauncherStore? = null

        /** Shares Compose-backed launcher state across Home, Assistant, See All, and pin requests. */
        fun get(context: Context): LauncherStore = instance ?: synchronized(this) {
            instance ?: LauncherStore(context.applicationContext).also { instance = it }
        }
    }
}
