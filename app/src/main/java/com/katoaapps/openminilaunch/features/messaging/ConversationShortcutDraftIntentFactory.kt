package com.katoaapps.openminilaunch.features.messaging

import android.content.ComponentName
import android.content.Intent

/** Builds an Android Direct Share handoff for one app-published conversation shortcut. */
internal class ConversationShortcutDraftIntentFactory {
    fun create(
        packageName: String,
        shortcutId: String,
        shortcutCategories: Set<String>,
        body: String,
    ): Intent? {
        val route = MessagingShortcutDraftCatalog.routeFor(
            packageName = packageName,
            shortcutCategories = shortcutCategories,
        ) ?: return null

        return Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .setComponent(ComponentName(packageName, route.targetActivity))
            .putExtra(Intent.EXTRA_TEXT, body.trim())
            .putExtra(Intent.EXTRA_SHORTCUT_ID, shortcutId)
    }
}
