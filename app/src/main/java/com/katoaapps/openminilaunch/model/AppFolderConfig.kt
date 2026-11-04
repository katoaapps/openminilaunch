package com.katoaapps.openminilaunch.model

internal const val MAX_APP_FOLDER_NAME_LENGTH = 80

enum class AppFolderColorMode {
    DEFAULT,
    THEME,
    CUSTOM,
}

/** Persisted definition for a user-created collection shown from an App Grid slot. */
data class AppFolderConfig(
    val id: String,
    val customName: String? = null,
    val orderedTargetKeys: List<String>,
    val colorMode: AppFolderColorMode = AppFolderColorMode.DEFAULT,
    val colorArgb: Int? = null,
)

internal fun AppFolderConfig.normalized(): AppFolderConfig {
    val normalizedMode = if (colorMode == AppFolderColorMode.CUSTOM && colorArgb == null) {
        AppFolderColorMode.DEFAULT
    } else {
        colorMode
    }
    return copy(
        id = id.trim(),
        customName = customName?.trim()?.take(MAX_APP_FOLDER_NAME_LENGTH)?.takeIf(String::isNotEmpty),
        orderedTargetKeys = orderedTargetKeys.map(String::trim).filter(String::isNotEmpty).distinct(),
        colorMode = normalizedMode,
        colorArgb = colorArgb.takeIf { normalizedMode == AppFolderColorMode.CUSTOM },
    )
}

internal fun appFolderColorMode(
    storedMode: String?,
    colorArgb: Int?,
): AppFolderColorMode = storedMode
    ?.let { runCatching { enumValueOf<AppFolderColorMode>(it) }.getOrNull() }
    ?: if (colorArgb != null) AppFolderColorMode.CUSTOM else AppFolderColorMode.DEFAULT

internal fun Iterable<AppFolderConfig>.normalizedAppFolders(): List<AppFolderConfig> =
    map(AppFolderConfig::normalized)
        .filter { it.id.isNotBlank() && it.orderedTargetKeys.size >= 2 }
        .distinctBy(AppFolderConfig::id)
