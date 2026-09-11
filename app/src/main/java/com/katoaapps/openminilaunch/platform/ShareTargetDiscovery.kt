package com.katoaapps.openminilaunch.platform

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.ai.AiHandoffMode
import com.katoaapps.openminilaunch.features.ai.AiProviderCatalog
import com.katoaapps.openminilaunch.features.ai.AiProviderOption
import com.katoaapps.openminilaunch.features.ai.packageNames
import com.katoaapps.openminilaunch.model.LaunchableApp

internal class ShareTargetDiscovery(private val context: Context) {
    fun compatibleAiApps(): List<LaunchableApp> = (
        textShareApps() + AiProviderCatalog.launchOnlyPackages.mapNotNull(::launchOnlyAiApp)
    )
        .distinctBy(LaunchableApp::packageName)
        .sortedBy { it.label.lowercase() }

    private fun textShareApps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map { LaunchableApp(it.loadLabel(context.packageManager).toString(), it.activityInfo.packageName) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun aiProviderOptions(): List<AiProviderOption> {
        val textSharePackages = textShareApps().map(LaunchableApp::packageName).toSet()
        return AiProviderCatalog.providers.map { provider ->
            val installedPackage = provider.packageNames.firstOrNull(::isInstalled)
            val canHandoff = when (provider.handoffMode) {
                AiHandoffMode.TEXT_SHARE -> installedPackage in textSharePackages
                AiHandoffMode.COPY_AND_LAUNCH -> installedPackage != null &&
                    context.packageManager.getLaunchIntentForPackage(installedPackage) != null
            }
            AiProviderOption(
                id = provider.id,
                label = installedPackage?.let(::installedAppLabel)
                    ?: context.getString(provider.labelRes),
                installedPackageName = installedPackage,
                handoffMode = provider.handoffMode,
                installed = installedPackage != null,
                canHandoff = canHandoff,
                installUrl = provider.installUrl,
                bundledIconRes = provider.bundledIconRes,
            )
        }.sortedBy { it.label.lowercase() }
    }

    private fun isInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0)
    }.isSuccess

    private fun installedAppLabel(packageName: String): String {
        val packageManager = context.packageManager
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        return packageManager.getApplicationLabel(applicationInfo).toString()
    }

    private fun launchOnlyAiApp(packageName: String): LaunchableApp? {
        val packageManager = context.packageManager
        if (packageManager.getLaunchIntentForPackage(packageName) == null) return null
        val appLabel = runCatching { installedAppLabel(packageName) }.getOrNull() ?: return null
        return LaunchableApp(
            label = context.getString(R.string.ai_copy_and_paste_app_label, appLabel),
            packageName = packageName,
        )
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
    }
}
