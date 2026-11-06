package com.katoaapps.openminilaunch.features.backup

import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.features.minkspace.todo.TodoItem
import com.katoaapps.openminilaunch.model.AppFolderConfig
import com.katoaapps.openminilaunch.model.AppFolderAnimationStyle
import com.katoaapps.openminilaunch.model.AppFolderColorMode
import com.katoaapps.openminilaunch.model.AppFolderIndicatorStyle
import com.katoaapps.openminilaunch.model.AppGridAssignment
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.model.FontAppearance
import com.katoaapps.openminilaunch.model.FontSource
import com.katoaapps.openminilaunch.model.IconAppearance
import com.katoaapps.openminilaunch.model.IconSource
import com.katoaapps.openminilaunch.model.MAX_APP_FOLDER_NAME_LENGTH
import com.katoaapps.openminilaunch.model.MAX_SOCIAL_GOAL_HOURS
import com.katoaapps.openminilaunch.model.MIN_SOCIAL_GOAL_HOURS
import com.katoaapps.openminilaunch.model.MinkAppPauseMode
import com.katoaapps.openminilaunch.model.LauncherKeyboardColorSource
import com.katoaapps.openminilaunch.model.LauncherKeyboardLayout
import com.katoaapps.openminilaunch.model.LauncherKeyboardPreferences
import com.katoaapps.openminilaunch.model.LauncherKeyboardStyle
import com.katoaapps.openminilaunch.model.normalizeLauncherKeyboardHeight
import com.katoaapps.openminilaunch.model.PalettePreference
import com.katoaapps.openminilaunch.model.QuickDrawerConfig
import com.katoaapps.openminilaunch.model.ThemePreference
import com.katoaapps.openminilaunch.model.MAX_WIDGET_ENTRIES
import com.katoaapps.openminilaunch.model.WidgetGridSize
import com.katoaapps.openminilaunch.model.WidgetProviderIdentity
import com.katoaapps.openminilaunch.model.WidgetRestoreRecipe
import com.katoaapps.openminilaunch.model.appGridSlotFromStoredName
import com.katoaapps.openminilaunch.model.appFolderColorMode
import com.katoaapps.openminilaunch.model.configurableAppGridSlots
import com.katoaapps.openminilaunch.model.legacyTargetKey
import com.katoaapps.openminilaunch.model.normalizeFontSizeScale
import com.katoaapps.openminilaunch.model.normalizeHomePanelTransparency
import com.katoaapps.openminilaunch.model.normalizedAppGridOrder
import org.json.JSONArray
import org.json.JSONObject

internal object LauncherBackupCodec {
    private const val MAX_APP_FOLDERS = 64
    private const val MAX_TARGETS = 1_000
    private const val MAX_TODOS = 5_000
    private const val MAX_TARGET_LENGTH = 2_048
    private const val MAX_TODO_TEXT_LENGTH = 20_000

    fun encode(backup: LauncherBackup): String = JSONObject().apply {
        put("format", LAUNCHER_BACKUP_FORMAT)
        put("schemaVersion", LAUNCHER_BACKUP_SCHEMA_VERSION)
        put("sourceAppVersion", backup.sourceAppVersion)
        put("exportedAtMillis", backup.exportedAtMillis)
        put("launcher", encodeLauncher(backup.launcher))
        put("settings", encodeSettings(backup.settings))
        put("todos", JSONArray().apply {
            backup.todos.forEach { todo ->
                put(JSONObject().apply {
                    put("id", todo.id)
                    put("text", todo.text)
                    put("completed", todo.completed)
                })
            }
        })
        put("widgets", encodeWidgetRecipes(backup.widgetRecipes))
        backup.integratedContacts?.let {
            put("favoriteContacts", FavoriteContactBackupCodec.encodeContacts(it))
        }
        backup.integratedContactOrder?.let { order ->
            put("favoriteContactOrder", FavoriteContactBackupCodec.encodeOrder(order))
        }
        backup.profile?.let { put("profile", ProfileBackupCodec.encode(it, backup.profileLinks)) }
        put("notIncluded", PORTABILITY_EXCLUSIONS.stringsJsonArray())
    }.toString(2)

    fun decode(json: String): LauncherBackup {
        val root = JSONObject(json)
        require(root.optString("format") == LAUNCHER_BACKUP_FORMAT) { "Not an OpenMink backup" }
        require(root.optInt("schemaVersion", -1) in 1..LAUNCHER_BACKUP_MAX_READABLE_SCHEMA_VERSION) {
            "Unsupported OpenMink backup version"
        }
        val profile = root.optJSONObject("profile")?.let(ProfileBackupCodec::decode)
        val launcher = decodeLauncher(root.getJSONObject("launcher"))

        return LauncherBackup(
            sourceAppVersion = root.optString("sourceAppVersion").safeText(80),
            exportedAtMillis = root.optLong("exportedAtMillis", 0L),
            launcher = launcher,
            settings = decodeSettings(root.getJSONObject("settings"), launcher.drawerTargets),
            todos = decodeTodos(root.optJSONArray("todos") ?: JSONArray()),
            profile = profile?.first,
            profileLinks = profile?.second.orEmpty(),
            widgetRecipes = decodeWidgetRecipes(root.optJSONArray("widgets")),
            integratedContacts = if (root.has("favoriteContacts")) {
                FavoriteContactBackupCodec.decodeContacts(root.optJSONArray("favoriteContacts"))
            } else null,
            integratedContactOrder = if (root.has("favoriteContactOrder")) {
                FavoriteContactBackupCodec.decodeOrder(root.optJSONArray("favoriteContactOrder"))
            } else null,
        )
    }

    private fun encodeWidgetRecipes(recipes: List<WidgetRestoreRecipe>) = JSONArray().apply {
        recipes.distinctBy(WidgetRestoreRecipe::entryId)
            .take(MAX_WIDGET_ENTRIES)
            .forEach { recipe ->
                put(
                    JSONObject().apply {
                        put("entryId", recipe.entryId)
                        put("providerPackage", recipe.provider.packageName)
                        put("providerClass", recipe.provider.className)
                        put("appLabel", recipe.provider.appLabel)
                        put("widgetLabel", recipe.provider.widgetLabel)
                        put("columns", recipe.gridSize.columns)
                        put("rows", recipe.gridSize.rows)
                    },
                )
            }
    }

    private fun decodeWidgetRecipes(array: JSONArray?): List<WidgetRestoreRecipe> = buildList {
        val source = array ?: return@buildList
        repeat(minOf(source.length(), MAX_WIDGET_ENTRIES)) { index ->
            val saved = source.optJSONObject(index) ?: return@repeat
            val entryId = saved.optString("entryId").safeText(128)
            val packageName = saved.optString("providerPackage").safeText(255)
            val className = saved.optString("providerClass").safeText(512)
            val columns = saved.optInt("columns", 0)
            val rows = saved.optInt("rows", 0)
            if (
                entryId.isBlank() || packageName.isBlank() || className.isBlank() ||
                columns !in 1..4 || rows !in 1..5 || any { it.entryId == entryId }
            ) {
                return@repeat
            }
            add(
                WidgetRestoreRecipe(
                    entryId = entryId,
                    provider = WidgetProviderIdentity(
                        packageName = packageName,
                        className = className,
                        appLabel = saved.optString("appLabel").safeText(160),
                        widgetLabel = saved.optString("widgetLabel").safeText(160),
                    ),
                    gridSize = WidgetGridSize(columns, rows),
                ),
            )
        }
    }

    private fun encodeLauncher(layout: LauncherBackupLayout) = JSONObject().apply {
        val foldersById = layout.appFolders.associateBy(AppFolderConfig::id)
        put("shortcutTargets", JSONObject().apply {
            layout.appGridAssignments.forEach { (slot, assignment) ->
                assignment.legacyTargetKey(foldersById::get)?.let { put(slot.name, it) }
            }
        })
        put("appGridAssignments", JSONObject().apply {
            layout.appGridAssignments.forEach { (slot, assignment) ->
                assignment.toJson()?.let { put(slot.name, it) }
            }
        })
        put("appFolders", JSONArray().apply {
            layout.appFolders.forEach { folder ->
                put(JSONObject().apply {
                    put("id", folder.id)
                    folder.customName?.let { put("customName", it) }
                    put("targetKeys", folder.orderedTargetKeys.stringsJsonArray())
                    if (folder.colorMode != AppFolderColorMode.DEFAULT) {
                        put("colorMode", folder.colorMode.name)
                    }
                    putNullable("colorArgb", folder.colorArgb)
                })
            }
        })
        put("shortcutOrder", layout.appGridOrder.namesJsonArray())
        put("confirmedShortcutChoices", layout.confirmedAppGridChoices.namesJsonArray())
        put("drawerTargets", layout.drawerTargets.stringsJsonArray())
        put("libraryShortcutTargets", layout.libraryShortcutTargets.stringsJsonArray())
    }

    private fun decodeLauncher(json: JSONObject): LauncherBackupLayout {
        val targetJson = json.optJSONObject("shortcutTargets") ?: JSONObject()
        val legacyAssignments = buildMap<AppGridSlot, AppGridAssignment> {
            AppGridSlot.entries.forEach { slot ->
                targetJson.optString(slot.name).safeTarget()?.let { targetKey ->
                    put(slot, AppGridAssignment.Target(targetKey))
                }
            }
        }
        val assignmentJson = json.optJSONObject("appGridAssignments")
        val appGridAssignments = if (assignmentJson != null) {
            buildMap<AppGridSlot, AppGridAssignment> {
                configurableAppGridSlots.forEach { slot ->
                    assignmentJson.optJSONObject(slot.name)
                        ?.toAppGridAssignment()
                        ?.let { put(slot, it) }
                }
            }
        } else {
            legacyAssignments
        }
        val appFolders = json.optJSONArray("appFolders").appFolderConfigs()
        val savedOrder = json.optJSONArray("shortcutOrder").appGridSlotValues()
        val appGridOrder = normalizedAppGridOrder(savedOrder)

        return LauncherBackupLayout(
            appGridAssignments = appGridAssignments,
            appFolders = appFolders,
            appGridOrder = appGridOrder,
            confirmedAppGridChoices = json.optJSONArray("confirmedShortcutChoices")
                .appGridSlotValues()
                .filter { it in configurableAppGridSlots }
                .distinct(),
            drawerTargets = json.optJSONArray("drawerTargets").safeTargets(),
            libraryShortcutTargets = json.optJSONArray("libraryShortcutTargets").safeTargets(),
        )
    }

    private fun encodeSettings(settings: LauncherBackupSettings) = JSONObject().apply {
        put("appearance", JSONObject().apply {
            put("theme", settings.themePreference.name)
            put("palette", settings.palettePreference.name)
            put("hideStatusBar", settings.hideStatusBar)
            put("alignHomePanelBottom", settings.alignHomePanelBottom)
            settings.twoPanelModeForLargeDisplays?.let { put("twoPanelModeForLargeDisplays", it) }
            put("showClock", settings.showClock)
            put("showDate", settings.showDate)
            put("showBatteryPercentage", settings.showBatteryPercentage)
            put("use24HourClock", settings.use24HourClock)
            put("homePanelColorArgb", settings.homePanelColorArgb)
            put("watermelonModeEnabled", settings.watermelonModeEnabled)
            put("homePanelTransparency", settings.homePanelTransparency)
            putNullable("appBackgroundColorArgb", settings.appBackgroundColorArgb)
            putNullable("appBackgroundPresetId", settings.appBackgroundPresetId)
            put("iconSource", settings.iconAppearance.source.name)
            putNullable("iconPackPackage", settings.iconAppearance.iconPackPackage)
            putNullable("minkIconColorArgb", settings.iconAppearance.minkIconColorArgb)
            put("fontSource", settings.fontAppearance.source.name)
            put("fontSizeScale", settings.fontAppearance.sizeScale.toDouble())
            putNullable("fontColorArgb", settings.fontAppearance.colorArgb)
            putNullable("customFontName", settings.fontAppearance.customFontName)
        })
        put("magicBox", JSONObject().apply {
            put("openSoftwareKeyboardOnHome", settings.openSoftwareKeyboardOnHome)
            put("includeAppShortcutsInDiscovery", settings.includeAppShortcutsInDiscovery)
            put("automaticAppBubbleTargets", settings.automaticAppBubbleTargets.stringsJsonArray())
            putNullable("preferredAiPackage", settings.preferredAiPackage)
            putNullable("preferredWebPackage", settings.preferredWebPackage)
            settings.launcherKeyboardPreferences?.let { keyboard ->
                put("launcherKeyboard", JSONObject().apply {
                    put("style", keyboard.style.name)
                    put("layout", keyboard.layout.name)
                    put("colorSource", keyboard.colorSource.name)
                    put("customPrimaryArgb", keyboard.customPrimaryArgb)
                    put("customSecondaryArgb", keyboard.customSecondaryArgb)
                    keyboard.keyColorOverrideArgb?.let { put("keyColorOverrideArgb", it) }
                    put("splitOnWideDisplays", keyboard.splitOnWideDisplays)
                    put("hapticFeedbackEnabled", keyboard.hapticFeedbackEnabled)
                    put("heightDp", keyboard.heightDp.toDouble())
                })
            }
        })
        settings.minkSpaceMiniAppOrder?.let { order ->
            put("minkSpace", JSONObject().apply {
                put("order", order.map(MinkSpaceMiniApp::stableId).stringsJsonArray())
                put(
                    "enabled",
                    settings.enabledMinkSpaceMiniApps
                        .orEmpty()
                        .map(MinkSpaceMiniApp::stableId)
                        .stringsJsonArray(),
                )
            })
        }
        put("messaging", JSONObject().apply {
            put("sendMessagesAutomatically", settings.sendMessagesAutomatically)
            putNullable("preferredMessagingPackage", settings.preferredMessagingPackage)
        })
        put("calling", JSONObject().apply {
            put("callAutomatically", settings.callAutomatically)
            putNullable("preferredCallingPackage", settings.preferredCallingPackage)
        })
        settings.quickDrawer?.let { quickDrawer ->
            put("quickDrawer", JSONObject().apply {
                put("orderedTargetKeys", quickDrawer.orderedTargetKeys.stringsJsonArray())
                put("showMinkPad", quickDrawer.showMinkPad)
                put("showPrivateSpace", quickDrawer.showPrivateSpace)
                put("showWorkProfile", quickDrawer.showWorkProfile)
                putNullable("colorArgb", quickDrawer.colorArgb)
            })
        }
        if (settings.folderIndicatorStyle != null || settings.folderAnimationStyle != null) {
            put("appGrid", JSONObject().apply {
                settings.folderIndicatorStyle?.let { put("folderIndicatorStyle", it.name) }
                settings.folderAnimationStyle?.let { put("folderAnimationStyle", it.name) }
            })
        }
        put("minkDay", JSONObject().apply {
            put("usesAutomaticSocialApps", settings.usesAutomaticSocialApps)
            put("socialPackages", settings.socialPackages.stringsJsonArray())
            put("socialGoalHours", settings.socialGoalHours)
            put("pauseMode", settings.minkAppPauseMode.name)
        })
        put("githubUpdateChecksEnabled", settings.githubUpdateChecksEnabled)
        settings.minkPadPreferences?.let { put("minkPad", MinkPadBackupCodec.encode(it)) }
    }

    private fun decodeSettings(
        json: JSONObject,
        legacyDrawerTargets: List<String>,
    ): LauncherBackupSettings {
        val appearance = json.getJSONObject("appearance")
        val magicBox = json.getJSONObject("magicBox")
        val launcherKeyboard = magicBox.optJSONObject("launcherKeyboard")
        val messaging = json.getJSONObject("messaging")
        val calling = json.optJSONObject("calling")
        val quickDrawer = json.optJSONObject("quickDrawer")
        val appGrid = json.optJSONObject("appGrid")
        val minkDay = json.getJSONObject("minkDay")
        val minkSpace = json.optJSONObject("minkSpace")
        val minkSpaceOrder = minkSpace?.optJSONArray("order")?.minkSpaceMiniApps()
        val enabledMinkSpaceMiniApps = minkSpace?.optJSONArray("enabled")
            ?.minkSpaceMiniApps()
            ?.toSet()
            ?.let { enabled ->
                if (minkSpaceOrder != null && MinkSpaceMiniApp.VCARD !in minkSpaceOrder) {
                    enabled + MinkSpaceMiniApp.VCARD
                } else {
                    enabled
                }
            }

        return LauncherBackupSettings(
            themePreference = appearance.enumValue("theme", ThemePreference.SYSTEM),
            palettePreference = appearance.enumValue("palette", PalettePreference.MINK),
            hideStatusBar = appearance.optBoolean("hideStatusBar", true),
            alignHomePanelBottom = appearance.optBoolean("alignHomePanelBottom", false),
            twoPanelModeForLargeDisplays = if (appearance.has("twoPanelModeForLargeDisplays")) {
                appearance.getBoolean("twoPanelModeForLargeDisplays")
            } else null,
            showClock = appearance.optBoolean("showClock", false),
            showDate = appearance.optBoolean("showDate", true),
            showBatteryPercentage = appearance.optBoolean("showBatteryPercentage", false),
            use24HourClock = appearance.optBoolean("use24HourClock", false),
            homePanelColorArgb = appearance.optInt("homePanelColorArgb"),
            watermelonModeEnabled = appearance.optBoolean("watermelonModeEnabled", false),
            homePanelTransparency = normalizeHomePanelTransparency(
                appearance.optDouble("homePanelTransparency", 0.0).toFloat(),
            ),
            appBackgroundColorArgb = appearance.optNullableInt("appBackgroundColorArgb"),
            appBackgroundPresetId = appearance.optNullableString("appBackgroundPresetId")
                ?.safeText(80),
            iconAppearance = IconAppearance(
                source = appearance.enumValue("iconSource", IconSource.MINK),
                iconPackPackage = appearance.optNullableString("iconPackPackage")?.safePackage(),
                minkIconColorArgb = appearance.optNullableInt("minkIconColorArgb"),
            ),
            fontAppearance = FontAppearance(
                source = appearance.enumValue("fontSource", FontSource.DEFAULT),
                sizeScale = normalizeFontSizeScale(
                    appearance.optDouble("fontSizeScale", 1.0).toFloat(),
                ),
                colorArgb = appearance.optNullableInt("fontColorArgb"),
                customFontName = appearance.optNullableString("customFontName")?.safeText(120),
            ),
            openSoftwareKeyboardOnHome = magicBox.optBoolean("openSoftwareKeyboardOnHome", true),
            launcherKeyboardPreferences = launcherKeyboard?.let { keyboard ->
                LauncherKeyboardPreferences(
                    style = keyboard.enumValue("style", LauncherKeyboardStyle.RAISED_PRIMARY),
                    layout = keyboard.enumValue("layout", LauncherKeyboardLayout.QWERTY),
                    colorSource = keyboard.enumValue("colorSource", LauncherKeyboardColorSource.THEME),
                    customPrimaryArgb = keyboard.optInt("customPrimaryArgb", 0xFF4B250F.toInt()),
                    customSecondaryArgb = keyboard.optInt("customSecondaryArgb", 0xFFFFE9CB.toInt()),
                    keyColorOverrideArgb = keyboard.optNullableInt("keyColorOverrideArgb"),
                    splitOnWideDisplays = keyboard.optBoolean("splitOnWideDisplays", true),
                    hapticFeedbackEnabled = keyboard.optBoolean("hapticFeedbackEnabled", true),
                    heightDp = normalizeLauncherKeyboardHeight(
                        keyboard.optDouble("heightDp", 276.0).toFloat(),
                    ),
                )
            },
            includeAppShortcutsInDiscovery = magicBox.optBoolean("includeAppShortcutsInDiscovery", false),
            automaticAppBubbleTargets = magicBox.optJSONArray("automaticAppBubbleTargets").safeTargets(),
            minkSpaceMiniAppOrder = minkSpaceOrder,
            enabledMinkSpaceMiniApps = enabledMinkSpaceMiniApps,
            // Automatic messaging is the product default. Backups created before this
            // preference existed must inherit that default instead of silently opting out.
            sendMessagesAutomatically = messaging.optBoolean("sendMessagesAutomatically", true),
            preferredMessagingPackage = messaging.optNullableString("preferredMessagingPackage")?.safePackage(),
            callAutomatically = calling?.optBoolean("callAutomatically", false) ?: false,
            preferredCallingPackage = calling?.optNullableString("preferredCallingPackage")?.safePackage(),
            quickDrawer = quickDrawer?.let { drawer ->
                QuickDrawerConfig(
                    orderedTargetKeys = drawer.optJSONArray("orderedTargetKeys")
                        ?.safeTargets()
                        ?: legacyDrawerTargets,
                    showMinkPad = drawer.optBoolean("showMinkPad", true),
                    showPrivateSpace = drawer.optBoolean("showPrivateSpace", true),
                    showWorkProfile = drawer.optBoolean("showWorkProfile", true),
                    colorArgb = drawer.optNullableInt("colorArgb"),
                )
            },
            folderIndicatorStyle = appGrid
                ?.enumValueOrNull<AppFolderIndicatorStyle>("folderIndicatorStyle"),
            folderAnimationStyle = appGrid
                ?.enumValueOrNull<AppFolderAnimationStyle>("folderAnimationStyle"),
            preferredAiPackage = magicBox.optNullableString("preferredAiPackage")?.safePackage(),
            preferredWebPackage = magicBox.optNullableString("preferredWebPackage")?.safePackage(),
            usesAutomaticSocialApps = minkDay.optBoolean("usesAutomaticSocialApps", true),
            socialPackages = minkDay.optJSONArray("socialPackages").safePackages(),
            socialGoalHours = minkDay.optInt("socialGoalHours", 1)
                .coerceIn(MIN_SOCIAL_GOAL_HOURS, MAX_SOCIAL_GOAL_HOURS),
            minkAppPauseMode = minkDay.enumValue("pauseMode", MinkAppPauseMode.NEVER),
            githubUpdateChecksEnabled = json.optBoolean("githubUpdateChecksEnabled", true),
            minkPadPreferences = json.optJSONObject("minkPad")?.let(MinkPadBackupCodec::decode),
        )
    }

    private fun decodeTodos(array: JSONArray): List<TodoItem> = buildList {
        repeat(minOf(array.length(), MAX_TODOS)) { index ->
            val item = array.optJSONObject(index) ?: return@repeat
            val text = item.optString("text").safeText(MAX_TODO_TEXT_LENGTH)
            if (text.isNotBlank()) {
                add(
                    TodoItem(
                        id = item.optString("id").safeText(128),
                        text = text,
                        completed = item.optBoolean("completed", false),
                    ),
                )
            }
        }
    }

    private inline fun <reified T : Enum<T>> JSONArray?.enumValues(): List<T> = buildList {
        val source = this@enumValues ?: return@buildList
        repeat(minOf(source.length(), MAX_ENUM_VALUES)) { index ->
            runCatching { enumValueOf<T>(source.optString(index)) }.getOrNull()?.let(::add)
        }
    }

    private fun JSONArray?.appGridSlotValues(): List<AppGridSlot> = buildList {
        val source = this@appGridSlotValues ?: return@buildList
        repeat(minOf(source.length(), MAX_ENUM_VALUES)) { index ->
            val value = appGridSlotFromStoredName(source.optString(index))
            value?.let { if (it !in this) add(it) }
        }
    }

    private fun JSONObject.toAppGridAssignment(): AppGridAssignment? = when (optString("type")) {
        "target" -> optString("selectionKey").safeTarget()?.let(AppGridAssignment::Target)
        "folder" -> optString("folderId")
            .safeText(128)
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

    private fun JSONArray?.appFolderConfigs(): List<AppFolderConfig> = buildList {
        val source = this@appFolderConfigs ?: return@buildList
        repeat(minOf(source.length(), MAX_APP_FOLDERS)) { index ->
            val folder = source.optJSONObject(index) ?: return@repeat
            val id = folder.optString("id").safeText(128)
            if (id.isBlank() || any { it.id == id }) return@repeat
            val colorArgb = folder.optNullableInt("colorArgb")
            add(
                AppFolderConfig(
                    id = id,
                    customName = folder.optString("customName")
                        .ifBlank { folder.optString("name") }
                        .safeText(MAX_APP_FOLDER_NAME_LENGTH)
                        .takeIf(String::isNotBlank),
                    orderedTargetKeys = folder.optJSONArray("targetKeys").safeTargets(),
                    colorMode = appFolderColorMode(folder.optString("colorMode").takeIf(String::isNotBlank), colorArgb),
                    colorArgb = colorArgb,
                ),
            )
        }
    }

    private inline fun <reified T : Enum<T>> JSONObject.enumValue(key: String, fallback: T): T =
        runCatching { enumValueOf<T>(optString(key)) }.getOrDefault(fallback)

    private inline fun <reified T : Enum<T>> JSONObject.enumValueOrNull(key: String): T? =
        optString(key)
            .takeIf(String::isNotBlank)
            ?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }

    private fun JSONArray?.safeTargets(): List<String> = safeStrings(MAX_TARGET_LENGTH)

    private fun JSONArray?.minkSpaceMiniApps(): List<MinkSpaceMiniApp> = buildList {
        val source = this@minkSpaceMiniApps ?: return@buildList
        repeat(minOf(source.length(), MAX_ENUM_VALUES)) { index ->
            MinkSpaceMiniApp.fromStableId(source.optString(index))?.let { miniApp ->
                if (miniApp.readyForHome && miniApp !in this) add(miniApp)
            }
        }
    }

    private fun JSONArray?.safePackages(): List<String> = safeStrings(255)

    private fun JSONArray?.safeStrings(maxLength: Int): List<String> = buildList {
        val source = this@safeStrings ?: return@buildList
        repeat(minOf(source.length(), MAX_TARGETS)) { index ->
            source.optString(index).safeText(maxLength).takeIf(String::isNotBlank)?.let(::add)
        }
    }.distinct()

    private fun String.safeTarget(): String? = safeText(MAX_TARGET_LENGTH).takeIf(String::isNotBlank)

    private fun String.safePackage(): String? = safeText(255).takeIf(String::isNotBlank)

    private fun String.safeText(maxLength: Int): String = trim().take(maxLength)

    private fun JSONObject.putNullable(key: String, value: Any?) {
        put(key, value ?: JSONObject.NULL)
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (isNull(key)) null else optInt(key)

    private fun <T : Enum<T>> List<T>.namesJsonArray() = JSONArray().apply {
        this@namesJsonArray.forEach { put(it.name) }
    }

    private fun List<String>.stringsJsonArray() = JSONArray().apply {
        this@stringsJsonArray.forEach(::put)
    }

    private val PORTABILITY_EXCLUSIONS = listOf(
        "androidPermissionsAndSystemRoles",
        "accessibilityAndNotificationAccess",
        "widgetProviderConfigurationAndContent",
        "documentFolderAccess",
        "wallpaperImage",
        "recentQueryHistory",
        "demoData",
        "profilePhoto",
        "minkSpaceCalculatorHistory",
        "minkSpaceMediaSelectionsAndUriGrants",
        "activeMediaSessions",
        "workAndPrivateProfileState",
        "devicePrivateAreaGateway",
        "activeBubbleNotifications",
        "updateCacheAndReminders",
        "minkPadHostsAndSession",
    )

    private const val MAX_ENUM_VALUES = 100
}
