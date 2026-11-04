package com.katoaapps.openminilaunch.model

internal const val MAX_APP_FOLDER_NAME_LENGTH = 80

/** Persisted definition for a user-created collection shown from an App Grid slot. */
data class AppFolderConfig(
    val id: String,
    val customName: String? = null,
    val orderedTargetKeys: List<String>,
)

internal fun AppFolderConfig.normalized(): AppFolderConfig = copy(
    id = id.trim(),
    customName = customName?.trim()?.take(MAX_APP_FOLDER_NAME_LENGTH)?.takeIf(String::isNotEmpty),
    orderedTargetKeys = orderedTargetKeys.map(String::trim).filter(String::isNotEmpty).distinct(),
)

internal fun Iterable<AppFolderConfig>.normalizedAppFolders(): List<AppFolderConfig> =
    map(AppFolderConfig::normalized)
        .filter { it.id.isNotBlank() && it.orderedTargetKeys.size >= 2 }
        .distinctBy(AppFolderConfig::id)
