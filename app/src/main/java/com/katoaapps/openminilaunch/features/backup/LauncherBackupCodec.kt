package com.katoaapps.openminilaunch.features.backup

import com.katoaapps.openminilaunch.model.IconAppearance
import com.katoaapps.openminilaunch.model.IconSource
import com.katoaapps.openminilaunch.model.MAX_SOCIAL_GOAL_HOURS
import com.katoaapps.openminilaunch.model.MIN_SOCIAL_GOAL_HOURS
import com.katoaapps.openminilaunch.model.MinkAppPauseMode
import com.katoaapps.openminilaunch.model.PinShortcutRequestPresentation
import com.katoaapps.openminilaunch.model.Shortcut
import com.katoaapps.openminilaunch.model.ThemePreference
import com.katoaapps.openminilaunch.model.TodoItem
import org.json.JSONArray
import org.json.JSONObject

internal object LauncherBackupCodec {
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
        put("notIncluded", PORTABILITY_EXCLUSIONS.stringsJsonArray())
    }.toString(2)

    fun decode(json: String): LauncherBackup {
        val root = JSONObject(json)
        require(root.optString("format") == LAUNCHER_BACKUP_FORMAT) { "Not an OpenMink backup" }
        require(root.optInt("schemaVersion", -1) == LAUNCHER_BACKUP_SCHEMA_VERSION) {
            "Unsupported OpenMink backup version"
        }

        return LauncherBackup(
            sourceAppVersion = root.optString("sourceAppVersion").safeText(80),
            exportedAtMillis = root.optLong("exportedAtMillis", 0L),
            launcher = decodeLauncher(root.getJSONObject("launcher")),
            settings = decodeSettings(root.getJSONObject("settings")),
            todos = decodeTodos(root.optJSONArray("todos") ?: JSONArray()),
        )
    }

    private fun encodeLauncher(layout: LauncherBackupLayout) = JSONObject().apply {
        put("shortcutTargets", JSONObject().apply {
            layout.shortcutTargets.forEach { (shortcut, target) -> put(shortcut.name, target) }
        })
        put("shortcutOrder", layout.shortcutOrder.namesJsonArray())
        put("confirmedShortcutChoices", layout.confirmedShortcutChoices.namesJsonArray())
        put("drawerTargets", layout.drawerTargets.stringsJsonArray())
        put("libraryShortcutTargets", layout.libraryShortcutTargets.stringsJsonArray())
    }

    private fun decodeLauncher(json: JSONObject): LauncherBackupLayout {
        val targetJson = json.optJSONObject("shortcutTargets") ?: JSONObject()
        val shortcutTargets = buildMap {
            Shortcut.entries.forEach { shortcut ->
                targetJson.optString(shortcut.name).safeTarget()?.let { put(shortcut, it) }
            }
        }
        val savedOrder = json.optJSONArray("shortcutOrder").enumValues<Shortcut>()
        val shortcutOrder = (savedOrder + Shortcut.entries).distinct()

        return LauncherBackupLayout(
            shortcutTargets = shortcutTargets,
            shortcutOrder = shortcutOrder,
            confirmedShortcutChoices = json.optJSONArray("confirmedShortcutChoices")
                .enumValues<Shortcut>()
                .distinct(),
            drawerTargets = json.optJSONArray("drawerTargets").safeTargets(),
            libraryShortcutTargets = json.optJSONArray("libraryShortcutTargets").safeTargets(),
        )
    }

    private fun encodeSettings(settings: LauncherBackupSettings) = JSONObject().apply {
        put("appearance", JSONObject().apply {
            put("theme", settings.themePreference.name)
            put("hideStatusBar", settings.hideStatusBar)
            put("alignHomePanelBottom", settings.alignHomePanelBottom)
            put("showClock", settings.showClock)
            put("use24HourClock", settings.use24HourClock)
            put("homePanelColorArgb", settings.homePanelColorArgb)
            putNullable("appBackgroundColorArgb", settings.appBackgroundColorArgb)
            put("iconSource", settings.iconAppearance.source.name)
            putNullable("iconPackPackage", settings.iconAppearance.iconPackPackage)
            putNullable("minkIconColorArgb", settings.iconAppearance.minkIconColorArgb)
        })
        put("magicBox", JSONObject().apply {
            put("openSoftwareKeyboardOnHome", settings.openSoftwareKeyboardOnHome)
            put("includeAppShortcutsInDiscovery", settings.includeAppShortcutsInDiscovery)
            putNullable("preferredAiPackage", settings.preferredAiPackage)
            putNullable("preferredWebPackage", settings.preferredWebPackage)
        })
        put("messaging", JSONObject().apply {
            put("sendMessagesAutomatically", settings.sendMessagesAutomatically)
            putNullable("preferredMessagingPackage", settings.preferredMessagingPackage)
        })
        put("minkDay", JSONObject().apply {
            put("usesAutomaticSocialApps", settings.usesAutomaticSocialApps)
            put("socialPackages", settings.socialPackages.stringsJsonArray())
            put("socialGoalHours", settings.socialGoalHours)
            put("pauseMode", settings.minkAppPauseMode.name)
        })
        put("githubUpdateChecksEnabled", settings.githubUpdateChecksEnabled)
        put("pinShortcutRequestPresentation", settings.pinShortcutRequestPresentation.name)
    }

    private fun decodeSettings(json: JSONObject): LauncherBackupSettings {
        val appearance = json.getJSONObject("appearance")
        val magicBox = json.getJSONObject("magicBox")
        val messaging = json.getJSONObject("messaging")
        val minkDay = json.getJSONObject("minkDay")

        return LauncherBackupSettings(
            themePreference = appearance.enumValue("theme", ThemePreference.SYSTEM),
            hideStatusBar = appearance.optBoolean("hideStatusBar", true),
            alignHomePanelBottom = appearance.optBoolean("alignHomePanelBottom", false),
            showClock = appearance.optBoolean("showClock", false),
            use24HourClock = appearance.optBoolean("use24HourClock", false),
            homePanelColorArgb = appearance.optInt("homePanelColorArgb"),
            appBackgroundColorArgb = appearance.optNullableInt("appBackgroundColorArgb"),
            iconAppearance = IconAppearance(
                source = appearance.enumValue("iconSource", IconSource.MINK),
                iconPackPackage = appearance.optNullableString("iconPackPackage")?.safePackage(),
                minkIconColorArgb = appearance.optNullableInt("minkIconColorArgb"),
            ),
            openSoftwareKeyboardOnHome = magicBox.optBoolean("openSoftwareKeyboardOnHome", true),
            includeAppShortcutsInDiscovery = magicBox.optBoolean("includeAppShortcutsInDiscovery", false),
            sendMessagesAutomatically = messaging.optBoolean("sendMessagesAutomatically", false),
            preferredMessagingPackage = messaging.optNullableString("preferredMessagingPackage")?.safePackage(),
            preferredAiPackage = magicBox.optNullableString("preferredAiPackage")?.safePackage(),
            preferredWebPackage = magicBox.optNullableString("preferredWebPackage")?.safePackage(),
            usesAutomaticSocialApps = minkDay.optBoolean("usesAutomaticSocialApps", true),
            socialPackages = minkDay.optJSONArray("socialPackages").safePackages(),
            socialGoalHours = minkDay.optInt("socialGoalHours", 1)
                .coerceIn(MIN_SOCIAL_GOAL_HOURS, MAX_SOCIAL_GOAL_HOURS),
            minkAppPauseMode = minkDay.enumValue("pauseMode", MinkAppPauseMode.NEVER),
            githubUpdateChecksEnabled = json.optBoolean("githubUpdateChecksEnabled", true),
            pinShortcutRequestPresentation = json.enumValue(
                "pinShortcutRequestPresentation",
                PinShortcutRequestPresentation.FULL_PAGE,
            ),
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

    private inline fun <reified T : Enum<T>> JSONObject.enumValue(key: String, fallback: T): T =
        runCatching { enumValueOf<T>(optString(key)) }.getOrDefault(fallback)

    private fun JSONArray?.safeTargets(): List<String> = safeStrings(MAX_TARGET_LENGTH)

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
        "widgets",
        "documentFolderAccess",
        "recentQueryHistory",
        "demoData",
        "updateCacheAndReminders",
    )

    private const val MAX_ENUM_VALUES = 100
}
