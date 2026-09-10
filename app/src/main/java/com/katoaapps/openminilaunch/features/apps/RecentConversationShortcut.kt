package com.katoaapps.openminilaunch.features.apps

import android.content.pm.ShortcutInfo
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget

/** Sortable metadata retained only while building the recent-conversation result list. */
internal data class RecentConversationShortcut(
    val target: LauncherShortcutTarget,
    val rank: Int,
    val lastChangedAt: Long,
)

internal fun ShortcutInfo.asRecentConversationShortcut(
    target: LauncherShortcutTarget,
    allowedPackages: Set<String>,
): RecentConversationShortcut? {
    if (`package` !in allowedPackages || !isDynamic || categories.isNullOrEmpty()) return null
    return RecentConversationShortcut(
        target = target,
        rank = rank,
        lastChangedAt = lastChangedTimestamp,
    )
}

internal val recentConversationShortcutOrder: Comparator<RecentConversationShortcut> =
    compareBy<RecentConversationShortcut> { it.rank }
        .thenByDescending { it.lastChangedAt }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.target.label }
