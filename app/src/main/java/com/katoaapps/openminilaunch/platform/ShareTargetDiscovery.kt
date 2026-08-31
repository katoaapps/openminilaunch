package com.katoaapps.openminilaunch.platform

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.katoaapps.openminilaunch.model.LaunchableApp

internal class ShareTargetDiscovery(private val context: Context) {
    fun textShareApps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map { LaunchableApp(it.loadLabel(context.packageManager).toString(), it.activityInfo.packageName) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun curatedAiApps(): List<LaunchableApp> {
        val compatible = textShareApps().associateBy { it.packageName }
        return CURATED_AI_PACKAGES.mapNotNull(compatible::get).sortedBy { it.label.lowercase() }
    }

    fun webSearchApps(): List<LaunchableApp> {
        val discoveryIntents = listOf(
            Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, BROWSER_DISCOVERY_QUERY),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
                .addCategory(Intent.CATEGORY_BROWSABLE),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER),
        )
        return discoveryIntents.asSequence()
            .flatMap { intent ->
                context.packageManager.queryIntentActivities(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY,
                ).asSequence()
            }
            .filter { it.activityInfo.packageName != context.packageName }
            .map { LaunchableApp(it.loadLabel(context.packageManager).toString(), it.activityInfo.packageName) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private companion object {
        const val BROWSER_DISCOVERY_QUERY = "MinkLauncher"
        val CURATED_AI_PACKAGES = setOf(
            "com.openai.chatgpt",
            "com.anthropic.claude",
            "ai.perplexity.app.android",
            "com.microsoft.copilot",
            "com.deepseek.chat",
            "com.facebook.stella",
            "com.google.android.apps.bard",
        )
    }
}
