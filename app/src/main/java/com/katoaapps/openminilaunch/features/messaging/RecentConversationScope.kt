package com.katoaapps.openminilaunch.features.messaging

import com.katoaapps.openminilaunch.model.LauncherShortcutTarget

/**
 * Chooses which messaging apps may contribute recent-conversation shortcuts.
 *
 * Automatic sending follows the one provider already chosen by the user. Manual sending is a
 * broader discovery mode, so every installed integrated provider and the system SMS app may
 * contribute. Android contacts remain a separate result source in either mode.
 */
internal fun resolveRecentConversationPackages(
    sendAutomatically: Boolean,
    preferredPackage: String?,
    defaultSmsPackage: String?,
    isInstalled: (String) -> Boolean,
): Set<String> {
    val installedDefault = defaultSmsPackage?.takeIf(isInstalled)
    if (sendAutomatically) {
        val installedPreference = preferredPackage?.takeIf(isInstalled)
        return setOfNotNull(installedPreference ?: installedDefault)
    }

    return buildSet {
        installedDefault?.let(::add)
        MessagingProviderCatalog.providers
            .flatMap { it.packageNames }
            .filter(isInstalled)
            .forEach(::add)
    }
}

internal fun filteredRecentConversations(
    shortcuts: List<LauncherShortcutTarget>,
    query: String,
    appLabel: (String) -> String,
    prioritizedPackage: String? = null,
    limit: Int = 5,
): List<LauncherShortcutTarget> {
    val normalizedQuery = query.trim()
    return shortcuts.asSequence()
        .filter { shortcut ->
            normalizedQuery.isEmpty() ||
                shortcut.label.contains(normalizedQuery, ignoreCase = true) ||
                appLabel(shortcut.packageName).contains(normalizedQuery, ignoreCase = true)
        }
        .sortedBy { it.packageName != prioritizedPackage }
        .take(limit)
        .toList()
}
