package com.katoaapps.openminilaunch.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.appearance.BundledHomeThemeCatalog
import com.katoaapps.openminilaunch.features.appearance.BundledWallpaperCatalog
import com.katoaapps.openminilaunch.features.appearance.CustomFontRepository
import com.katoaapps.openminilaunch.features.appearance.HomeWallpaperRepository
import com.katoaapps.openminilaunch.features.appearance.WATERMELON_PILL_COLOR_ARGB
import com.katoaapps.openminilaunch.features.demo.DemoHomeProfile
import com.katoaapps.openminilaunch.features.contacts.FavoriteAttentionColorStore
import com.katoaapps.openminilaunch.features.contacts.FavoriteContactOrderStore
import com.katoaapps.openminilaunch.features.contacts.FavoriteIntegratedContactStore
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.features.minkspace.todo.TodoItem
import com.katoaapps.openminilaunch.features.minkspace.todo.TodoStore
import com.katoaapps.openminilaunch.features.profile.ProfileRepository
import com.katoaapps.openminilaunch.model.*

class LauncherStore private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("mini_launch", Context.MODE_PRIVATE)
    private val persistence = LauncherStorePersistence(prefs)
    private val todoStore: TodoStore = TodoStore(persistence) { demoSearchDataEnabled }
    private val launcherSelections = LauncherSelectionStore(persistence)
    private val searchStore = SearchStore(persistence, MAX_SEARCH_HISTORY)
    private val widgetStore = WidgetStore(persistence, MAX_WIDGET_ENTRIES)
    private val generalPreferences = GeneralPreferenceStore(prefs)
    private val launcherKeyboardPreferenceStore = LauncherKeyboardPreferenceStore(prefs)
    private val appearancePreferences = AppearancePreferenceStore(
        appContext,
        prefs,
        isFreshInstall = !generalPreferences.onboardingComplete,
    )
    private val iconAppearancePreferences = IconAppearancePreferenceStore(prefs)
    private val fontAppearancePreferences = FontAppearancePreferenceStore(prefs)
    private val messagingPreferences = MessagingPreferenceStore(prefs)
    private val callingPreferences = CallingPreferenceStore(prefs)
    private val conversationAttentionPreferences = ConversationAttentionPreferenceStore(prefs)
    private val minkDayPreferences = MinkDayPreferenceStore(prefs)
    private val updatePreferences = UpdatePreferenceStore(prefs)
    private val appDiscoveryPreferences = AppDiscoveryPreferenceStore(prefs)
    private val appGridPreferences = AppGridPreferenceStore(prefs)
    private val appBubblePreferences = AppBubblePreferenceStore(prefs)
    private val privateContainerPreferences = PrivateContainerPreferenceStore(prefs)
    private val quickDrawerPreferences = QuickDrawerPreferenceStore(prefs)
    private val minkSpacePreferences = MinkSpacePreferenceStore(prefs)
    private val minkSpaceWidgetStore = MinkSpaceWidgetStore(prefs)
    private val homeWallpaperRepository = HomeWallpaperRepository(appContext)
    private val customFontRepository = CustomFontRepository(appContext)
    var customFontRevision by mutableIntStateOf(0)
        private set
    internal val profileRepository = ProfileRepository(appContext)
    internal val favoriteIntegratedContactStore = FavoriteIntegratedContactStore(appContext)
    internal val favoriteContactOrderStore = FavoriteContactOrderStore(appContext)
    internal val favoriteAttentionColorStore = FavoriteAttentionColorStore(appContext)
    internal val minkPadPreferences = com.katoaapps.openminilaunch.features.minkpad.minkPadRepository(appContext).preferences
    private val demoPreferences: DemoPreferenceStore = DemoPreferenceStore(appContext, prefs, todoStore)
    internal val demoHomeProfile get() = demoPreferences.profile
    val todos get() = todoStore.items
    internal val savedTodosForBackup get() = persistence.loadTodos()
    /** Typed App Grid contents. Legacy package-only target values migrate after discovery. */
    val appGridAssignments get() = launcherSelections.appGridAssignments
    val appFolders get() = launcherSelections.appFolders
    val appGridOrder get() = launcherSelections.appGridOrder
    val confirmedAppGridChoices get() = launcherSelections.confirmedAppGridChoices
    /** Stable launcher target keys, allowing personal and work copies of one package to coexist. */
    val drawerTargets get() = launcherSelections.drawerTargets
    /** App-published shortcuts shown in Magic Box search and the All Apps carousel. */
    val libraryShortcutTargets get() = launcherSelections.libraryShortcutTargets
    val pinnedLauncherSelectionKeys: List<String>
        get() = (
            appGridAssignments.values.mapNotNull(AppGridAssignment::targetKeyOrNull) +
                appGridAssignments.values.filterIsInstance<AppGridAssignment.Folder>()
                    .flatMap { appFolder(it.folderId)?.orderedTargetKeys.orEmpty() } +
                drawerTargets + libraryShortcutTargets +
                listOfNotNull(privateContainerGatewaySelectionKey)
            ).distinct()
    val searchFolders get() = searchStore.folders
    val searchHistory get() = searchStore.history
    internal val widgetEntries get() = widgetStore.entries
    val socialPackages get() = minkDayPreferences.socialPackages
    val usesAutomaticSocialApps get() = minkDayPreferences.usesAutomaticSocialApps
    val onboardingComplete get() = generalPreferences.onboardingComplete
    val hasOpenedClockFromDate get() = generalPreferences.hasOpenedClockFromDate
    val openSoftwareKeyboardOnHome get() = generalPreferences.openSoftwareKeyboardOnHome
    internal val launcherKeyboardPreferences get() = launcherKeyboardPreferenceStore.preferences
    val themePreference get() = appearancePreferences.themePreference
    val palettePreference get() = appearancePreferences.palettePreference
    val hideStatusBar get() = appearancePreferences.hideStatusBar
    val alignHomePanelBottom get() = appearancePreferences.alignHomePanelBottom
    val twoPanelModeForLargeDisplays get() = appearancePreferences.twoPanelModeForLargeDisplays
    val showClock get() = appearancePreferences.showClock
    val showDate get() = appearancePreferences.showDate
    val showBatteryPercentage get() = appearancePreferences.showBatteryPercentage
    val use24HourClock get() = appearancePreferences.use24HourClock
    val homePanelColorArgb get() = appearancePreferences.homePanelColorArgb
    val homePanelTransparency get() = appearancePreferences.homePanelTransparency
    val appBackgroundColorArgb get() = appearancePreferences.appBackgroundColorArgb
    val appBackgroundImageEnabled: Boolean
        get() = !demoPreferences.enabled &&
            appearancePreferences.appBackgroundImageEnabled &&
            homeWallpaperRepository.hasWallpaper()
    val hasSavedAppBackgroundImage: Boolean
        get() = !demoPreferences.enabled && homeWallpaperRepository.hasWallpaper()
    internal val bundledHomeTheme
        get() = if (demoPreferences.enabled) null
        else BundledHomeThemeCatalog.find(appearancePreferences.appBackgroundPresetId)
    internal val appBackgroundPreset get() = bundledHomeTheme?.wallpaper
    val appBackgroundImageRevision get() = appearancePreferences.appBackgroundImageRevision
    val useSystemHomeWallpaper: Boolean
        get() = !demoPreferences.enabled && appearancePreferences.useSystemHomeWallpaper
    internal val appBackgroundImageFile get() = homeWallpaperRepository.wallpaperFile
    val iconAppearance get() = iconAppearancePreferences.appearance
    val fontAppearance get() = fontAppearancePreferences.appearance
    internal val customFontFile get() = customFontRepository.fontFile
    val hasCustomFont get() = customFontRepository.hasFont
    val sendMessagesAutomatically get() = messagingPreferences.sendMessagesAutomatically
    val preferredMessagingPackage get() = messagingPreferences.preferredMessagingPackage
    val callAutomatically get() = callingPreferences.callAutomatically
    val preferredCallingPackage get() = callingPreferences.preferredCallingPackage
    val preferredAiPackage get() = messagingPreferences.preferredAiPackage
    val preferredWebPackage get() = messagingPreferences.preferredWebPackage
    val socialGoalMinutes get() = minkDayPreferences.socialGoalMinutes
    val socialGoalHours get() = minkDayPreferences.socialGoalHours
    val minkAppPauseMode get() = minkDayPreferences.pauseMode
    val demoSearchDataEnabled get() = demoPreferences.enabled
    val githubUpdateChecksEnabled get() = updatePreferences.checksEnabled
    val latestGitHubReleaseTag get() = updatePreferences.latestReleaseTag
    val includeAppShortcutsInDiscovery get() = appDiscoveryPreferences.includeAppShortcuts
    val folderIndicatorStyle get() = appGridPreferences.folderIndicatorStyle
    val folderAnimationStyle get() = appGridPreferences.folderAnimationStyle
    val automaticAppBubbleTargets get() = appBubblePreferences.automaticTargetKeys
    val privateContainerGatewaySelectionKey get() = privateContainerPreferences.gatewaySelectionKey
    val showMinkPadInQuickDrawer get() = quickDrawerPreferences.showMinkPadButton
    val showPrivateSpaceInQuickDrawer get() = quickDrawerPreferences.showPrivateSpaceButton
    val showWorkProfileInQuickDrawer get() = quickDrawerPreferences.showWorkProfileButton
    val quickDrawerColorArgb get() = quickDrawerPreferences.colorArgb
    val quickDrawerConfig: QuickDrawerConfig
        get() = QuickDrawerConfig(
            orderedTargetKeys = drawerTargets.toList(),
            showMinkPad = showMinkPadInQuickDrawer,
            showPrivateSpace = showPrivateSpaceInQuickDrawer,
            showWorkProfile = showWorkProfileInQuickDrawer,
            colorArgb = quickDrawerColorArgb,
        )
    internal val minkSpaceMiniAppOrder get() = minkSpacePreferences.order
    internal val enabledMinkSpaceMiniApps get() = minkSpacePreferences.enabledMiniApps
    internal val minkSpaceWidgetId get() = minkSpaceWidgetStore.appWidgetId
    val effectiveHomePanelColorArgb: Int
        get() = demoPreferences.effectivePanelColor(
            bundledHomeTheme?.panelColorArgb ?: homePanelColorArgb,
        )
    val watermelonModeEnabled: Boolean
        get() = appearancePreferences.watermelonModeEnabled
    val effectiveAppBackgroundColorArgb: Int?
        get() = demoPreferences.effectiveBackgroundColor(
            bundledHomeTheme?.backgroundColorArgb ?: appBackgroundColorArgb,
        )
    val visibleAppBackgroundColorArgb: Int?
        get() = if (appBackgroundImageEnabled) {
            appearancePreferences.appBackgroundImageAppearance?.averageColorArgb
        } else if (appBackgroundPreset != null) {
            appearancePreferences.appBackgroundPresetAppearance?.averageColorArgb
        } else if (useSystemHomeWallpaper) {
            homeWallpaperRepository.systemWallpaperAppearance?.averageColorArgb
        } else {
            effectiveAppBackgroundColorArgb
        }
    val visibleAppHeaderColorArgb: Int?
        get() = if (appBackgroundImageEnabled) {
            appearancePreferences.appBackgroundImageAppearance?.headerColorArgb
        } else if (appBackgroundPreset != null) {
            appearancePreferences.appBackgroundPresetAppearance?.headerColorArgb
        } else if (useSystemHomeWallpaper) {
            homeWallpaperRepository.systemWallpaperAppearance?.headerColorArgb
        } else {
            effectiveAppBackgroundColorArgb
        }
    val effectiveAppGridOrder: List<AppGridSlot>
        get() = demoPreferences.effectiveAppGridOrder(appGridOrder)

    init {
        if (fontAppearance.source == FontSource.CUSTOM && !hasCustomFont) {
            fontAppearancePreferences.useSource(FontSource.DEFAULT)
        }
        if (
            appearancePreferences.appBackgroundImageEnabled &&
            appearancePreferences.appBackgroundImageAppearance == null
        ) {
            homeWallpaperRepository.readStoredAppearance()
                ?.let(appearancePreferences::useAppBackgroundImage)
        }
        val savedPreset = BundledWallpaperCatalog.find(appearancePreferences.appBackgroundPresetId)
        if (savedPreset != null && appearancePreferences.appBackgroundPresetAppearance == null) {
            homeWallpaperRepository.readBundledAppearance(savedPreset.drawableRes)
                .getOrNull()
                ?.let { appearance ->
                    appearancePreferences.useAppBackgroundPreset(savedPreset.id, appearance)
                }
        }
        prefs.edit()
            .remove("calendar_input_language")
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
        val saved = persistence.load(MAX_SEARCH_HISTORY, MAX_WIDGET_ENTRIES)
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

    internal fun replaceTodosFromBackup(items: List<TodoItem>) {
        todoStore.replaceFromBackup(items)
    }

    fun placePinnedAppGridTarget(slot: AppGridSlot, targetKey: String) {
        launcherSelections.placePinnedAppGridTarget(slot, targetKey)
    }

    fun appGridAssignment(slot: AppGridSlot): AppGridAssignment =
        launcherSelections.appGridAssignment(slot)

    fun appGridTargetKey(slot: AppGridSlot): String? = launcherSelections.appGridTargetKey(slot)

    fun appFolder(folderId: String): AppFolderConfig? = launcherSelections.appFolder(folderId)

    fun appGridSelectionKeys(slot: AppGridSlot): List<String> = launcherSelections.appGridSelectionKeys(slot)

    fun saveAppGridSelection(
        slot: AppGridSlot,
        selectedTargetKeys: List<String>,
    ) {
        launcherSelections.saveAppGridSelection(slot, selectedTargetKeys)
    }

    fun updateAppFolderAppearance(
        folderId: String,
        customName: String?,
        colorMode: AppFolderColorMode,
        colorArgb: Int?,
    ) {
        launcherSelections.updateAppFolderAppearance(
            folderId,
            customName,
            colorMode,
            colorArgb,
        )
    }

    fun pruneLauncherTargets(shouldRemove: (String) -> Boolean): Boolean =
        launcherSelections.pruneLauncherTargets(shouldRemove)

    fun confirmSystemDefaultsForUnselectedShortcuts() {
        launcherSelections.confirmSystemDefaults()
    }

    fun hasConfirmedAllShortcutChoices(): Boolean = launcherSelections.hasConfirmedAll()

    fun setAppGridOrder(order: List<AppGridSlot>) {
        launcherSelections.setAppGridOrder(order)
    }

    internal fun replaceLauncherSelectionsFromBackup(
        appGridAssignments: Map<AppGridSlot, AppGridAssignment>,
        appFolders: List<AppFolderConfig>,
        appGridOrder: List<AppGridSlot>,
        confirmedAppGridChoices: List<AppGridSlot>,
        drawerTargets: List<String>,
        libraryShortcutTargets: List<String>,
    ) {
        launcherSelections.replaceFromBackup(
            restoredAssignments = appGridAssignments,
            restoredFolders = appFolders,
            restoredOrder = appGridOrder,
            restoredConfirmedChoices = confirmedAppGridChoices,
            restoredDrawerTargets = drawerTargets,
            restoredLibraryTargets = libraryShortcutTargets,
        )
    }

    fun setDrawerTargets(targetKeys: List<String>): Boolean =
        launcherSelections.setDrawerTargets(targetKeys)

    fun addDrawerTarget(targetKey: String): Boolean = launcherSelections.addDrawerTarget(targetKey)

    fun replaceDrawerTarget(index: Int, targetKey: String): Boolean =
        launcherSelections.replaceDrawerTarget(index, targetKey)

    fun addLibraryShortcut(targetKey: String) {
        launcherSelections.addLibraryShortcut(targetKey)
    }

    fun updateIncludeAppShortcutsInDiscovery(enabled: Boolean) {
        appDiscoveryPreferences.updateIncludeAppShortcuts(enabled)
    }

    fun updateFolderIndicatorStyle(style: AppFolderIndicatorStyle) {
        appGridPreferences.updateFolderIndicatorStyle(style)
    }

    fun updateFolderAnimationStyle(style: AppFolderAnimationStyle) {
        appGridPreferences.updateFolderAnimationStyle(style)
    }

    fun setAppBubbleAutomatic(targetKey: String, enabled: Boolean) {
        appBubblePreferences.setAutomatic(targetKey, enabled)
    }

    fun setPrivateContainerGateway(selectionKey: String) {
        privateContainerPreferences.setGateway(selectionKey)
    }

    fun clearPrivateContainerGateway() {
        privateContainerPreferences.clearGateway()
    }

    fun updateShowMinkPadInQuickDrawer(enabled: Boolean) {
        quickDrawerPreferences.updateShowMinkPadButton(enabled)
    }

    fun updateShowPrivateSpaceInQuickDrawer(enabled: Boolean) {
        quickDrawerPreferences.updateShowPrivateSpaceButton(enabled)
    }

    fun updateShowWorkProfileInQuickDrawer(enabled: Boolean) {
        quickDrawerPreferences.updateShowWorkProfileButton(enabled)
    }

    fun setQuickDrawerColor(argb: Int?) {
        quickDrawerPreferences.setColor(argb)
    }

    internal fun replaceAutomaticAppBubbleTargets(targetKeys: Collection<String>) {
        appBubblePreferences.replaceAutomaticTargets(targetKeys)
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

    internal fun updateLauncherKeyboardPreferences(preferences: LauncherKeyboardPreferences) {
        launcherKeyboardPreferenceStore.update(preferences)
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

    fun setPalette(preference: PalettePreference) {
        appearancePreferences.setPalette(preference)
    }

    fun updateHideStatusBar(enabled: Boolean) {
        appearancePreferences.updateHideStatusBar(enabled)
    }

    fun updateAlignHomePanelBottom(enabled: Boolean) {
        appearancePreferences.updateAlignHomePanelBottom(enabled)
    }

    fun updateTwoPanelModeForLargeDisplays(enabled: Boolean) {
        appearancePreferences.updateTwoPanelModeForLargeDisplays(enabled)
    }

    fun maybeEnableTwoPanelForDisplay(context: Context) {
        appearancePreferences.maybeEnableTwoPanelForDisplay(context)
    }

    fun maybeEnableTwoPanelForUnfoldedBookDisplay(unfoldedBookDisplay: Boolean) {
        appearancePreferences.maybeEnableTwoPanelForUnfoldedBookDisplay(unfoldedBookDisplay)
    }

    fun updateShowClock(enabled: Boolean) {
        appearancePreferences.updateShowClock(enabled)
    }

    fun updateShowDate(enabled: Boolean) {
        appearancePreferences.updateShowDate(enabled)
    }

    fun updateShowBatteryPercentage(enabled: Boolean) {
        appearancePreferences.updateShowBatteryPercentage(enabled)
    }

    fun updateUse24HourClock(enabled: Boolean) {
        appearancePreferences.updateUse24HourClock(enabled)
    }

    internal fun setMinkSpaceMiniAppEnabled(miniApp: MinkSpaceMiniApp, enabled: Boolean) {
        minkSpacePreferences.setEnabled(miniApp, enabled)
    }

    internal fun setMinkSpaceMiniAppOrder(order: List<MinkSpaceMiniApp>) {
        minkSpacePreferences.updateOrder(order)
    }

    internal fun replaceMinkSpacePreferencesFromBackup(
        order: List<MinkSpaceMiniApp>,
        enabledMiniApps: Set<MinkSpaceMiniApp>,
    ) {
        minkSpacePreferences.replaceFromBackup(order, enabledMiniApps)
    }

    internal fun setMinkSpaceWidget(appWidgetId: Int) {
        minkSpaceWidgetStore.replace(appWidgetId)
    }

    internal fun clearMinkSpaceWidget() {
        minkSpaceWidgetStore.clear()
    }

    fun setHomePanelColor(argb: Int) {
        val opaqueArgb = argb or 0xFF000000.toInt()
        if (demoPreferences.setPanelColor(opaqueArgb)) return
        appearancePreferences.setHomePanelColor(opaqueArgb)
    }

    fun activateWatermelonMode() {
        appearancePreferences.setHomePanelColor(WATERMELON_PILL_COLOR_ARGB, watermelonMode = true)
    }

    fun setHomePanelTransparency(transparency: Float) {
        appearancePreferences.updateHomePanelTransparency(transparency)
    }

    fun setAppBackgroundColor(argb: Int?) {
        val opaqueArgb = argb?.or(0xFF000000.toInt())
        if (demoPreferences.setBackgroundColor(opaqueArgb)) return
        appearancePreferences.setAppBackgroundColor(opaqueArgb)
    }

    fun setCustomThemeBackgroundColor(argb: Int) {
        appearancePreferences.updateAppBackgroundColor(argb or 0xFF000000.toInt())
    }

    internal fun useAppBackgroundImage(appearance: HomeWallpaperAppearance) {
        if (homeWallpaperRepository.hasWallpaper()) {
            if (appearancePreferences.appBackgroundColorArgb == null) {
                appearancePreferences.updateAppBackgroundColor(
                    ContextCompat.getColor(appContext, R.color.mink_forest),
                )
            }
            appearancePreferences.useAppBackgroundImage(appearance)
        }
    }

    internal fun useAppBackgroundPreset(id: String, appearance: HomeWallpaperAppearance) {
        if (BundledWallpaperCatalog.find(id) != null) {
            appearancePreferences.useAppBackgroundPreset(id, appearance)
        }
    }

    internal fun restoreAppBackgroundPreset(id: String): Boolean {
        val wallpaper = BundledWallpaperCatalog.find(id) ?: return false
        val appearance = homeWallpaperRepository.readBundledAppearance(wallpaper.drawableRes).getOrNull()
            ?: return false
        appearancePreferences.useAppBackgroundPreset(id, appearance)
        homeWallpaperRepository.applyBundledToSystem(wallpaper.drawableRes)
        return true
    }

    fun useMinkIcons() {
        iconAppearancePreferences.useMinkIcons()
    }

    fun useSystemIcons() {
        iconAppearancePreferences.useSystemIcons()
    }

    fun useIconPack(packageName: String) {
        iconAppearancePreferences.useIconPack(packageName)
    }

    fun setMinkIconColor(argb: Int?) {
        iconAppearancePreferences.setMinkIconColor(argb)
    }

    fun useFont(source: FontSource) {
        if (source != FontSource.CUSTOM || hasCustomFont) {
            fontAppearancePreferences.useSource(source)
        }
    }

    fun importCustomFont(uri: android.net.Uri): Result<Unit> = customFontRepository.import(uri).map { name ->
        fontAppearancePreferences.useCustomFont(name)
        customFontRevision++
    }

    fun setFontSizeScale(scale: Float) {
        fontAppearancePreferences.setSizeScale(scale)
    }

    fun setFontColor(argb: Int?) {
        fontAppearancePreferences.setColor(argb)
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

    fun updateCallAutomatically(enabled: Boolean) {
        callingPreferences.updateCallAutomatically(enabled)
    }

    fun setPreferredCallingApp(packageName: String) {
        callingPreferences.setPreferredCallingApp(packageName)
    }

    fun resetPreferredCallingApp() {
        callingPreferences.setPreferredCallingApp(null)
    }

    fun acknowledgedConversationActivityAt(conversationId: String): Long =
        conversationAttentionPreferences.acknowledgedThrough(conversationId)

    fun acknowledgeConversationActivity(conversationId: String, activityAt: Long?) {
        conversationAttentionPreferences.acknowledge(conversationId, activityAt)
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

    internal fun addWidget(
        appWidgetId: Int,
        size: WidgetGridSize,
        provider: WidgetProviderIdentity,
        replacementEntryId: String? = null,
    ) {
        widgetStore.add(appWidgetId, size, provider, replacementEntryId)
    }

    internal fun removeWidget(entryId: String): WidgetEntry? = widgetStore.remove(entryId)

    fun setWidgetSize(appWidgetId: Int, size: WidgetGridSize) {
        widgetStore.setSize(appWidgetId, size)
    }

    internal fun updateWidgetProvider(appWidgetId: Int, provider: WidgetProviderIdentity) {
        widgetStore.updateProvider(appWidgetId, provider)
    }

    fun setWidgetOrder(orderedEntryIds: List<String>) {
        widgetStore.setOrder(orderedEntryIds)
    }

    internal fun importWidgetRestoreRecipes(
        recipes: List<WidgetRestoreRecipe>,
    ): WidgetRecipeImportResult = widgetStore.importRestoreRecipes(recipes)

    internal fun portableWidgetRestoreRecipes(): List<WidgetRestoreRecipe> {
        val manager = android.appwidget.AppWidgetManager.getInstance(appContext)
        return widgetEntries.mapNotNull { entry ->
            when (entry) {
                is WidgetEntry.RestorePlaceholder -> entry.recipe
                is WidgetEntry.Bound -> {
                    val provider = manager.getAppWidgetInfo(entry.appWidgetId)
                        ?.toWidgetProviderIdentity(appContext.packageManager)
                        ?: entry.provider
                        ?: return@mapNotNull null
                    WidgetRestoreRecipe(
                        entryId = entry.entryId,
                        provider = provider,
                        gridSize = entry.gridSize ?: WidgetGridSize(columns = 2, rows = 2),
                    )
                }
            }
        }.take(MAX_WIDGET_ENTRIES)
    }

    companion object {
        private const val MAX_SEARCH_HISTORY = 5
        @Volatile
        private var instance: LauncherStore? = null

        /** Shares Compose-backed launcher state across Home, Assistant, See All, and pin requests. */
        fun get(context: Context): LauncherStore = instance ?: synchronized(this) {
            instance ?: LauncherStore(context.applicationContext).also { instance = it }
        }
    }
}
