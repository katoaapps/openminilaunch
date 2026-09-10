package com.katoaapps.openminilaunch.features.messaging

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.CommunicationRecipient

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import androidx.core.net.toUri

/**
 * Owns installed-provider discovery and Android intent handoffs for Magic Box messages.
 *
 * DeviceActions delegates here so package-specific behavior stays separate from unrelated
 * launcher actions. The callbacks preserve DeviceActions' shared label cache and activity start
 * behavior.
 */
internal class MessagingDeviceActions(
    private val context: Context,
    private val appLabel: (String) -> String,
    private val startActivity: (Intent, Boolean) -> Boolean,
) {
    private val messageIntents = MessagingIntentFactory(context)
    private val conversationShortcutIntents = ConversationShortcutDraftIntentFactory()

    /** Resolves the curated catalog and keeps missing providers as disabled picker rows. */
    fun providerOptions(): List<MessagingProviderOption> {
        val defaultPackage = defaultMessagingPackage()
        val systemOption = MessagingProviderOption(
            id = MessagingProviderCatalog.SYSTEM_DEFAULT_PROVIDER_ID,
            label = defaultPackage?.let(appLabel) ?: context.getString(R.string.system_messages),
            preferencePackageName = null,
            installedPackageName = defaultPackage,
            supportTier = MessagingSupportTier.CONTACT_AND_DRAFT,
            bundledIconRes = null,
            installed = true,
            systemDefault = true,
        )
        val providerOptions = MessagingProviderCatalog.providers.map { provider ->
            // The first installed package wins when a provider publishes more than one official
            // build. Keep this order stable because it also controls preference migration.
            val installedPackage = provider.resolveInstalledPackage(::isPackageInstalled)
            val supportedRoute = installedPackage?.let { verifiedRoutes(provider, it).firstOrNull() }
            val supportsRecentChatDrafts = installedPackage?.let(::supportsConversationShortcutDrafts)
                ?: false
            MessagingProviderOption(
                id = provider.id,
                label = installedPackage?.let(appLabel) ?: context.getString(provider.labelRes),
                preferencePackageName = installedPackage ?: provider.packageName,
                installedPackageName = installedPackage,
                supportTier = if (
                    provider.kind == MessagingDraftKind.SIGNAL_CONTACT &&
                    supportedRoute?.carriesRecipient == false
                ) {
                    MessagingSupportTier.RECIPIENT_IN_APP
                } else {
                    provider.supportTier
                },
                bundledIconRes = provider.bundledIconRes,
                installed = installedPackage != null,
                supportsRecentChatDrafts = supportsRecentChatDrafts,
                betaCompatibility = installedPackage != null && supportedRoute == null,
            )
        }
        return listOf(systemOption) + providerOptions.sortedWith(
            compareBy<MessagingProviderOption> { !it.isDraftReady }
                .thenBy { it.label.lowercase() },
        )
    }

    fun defaultMessagingAppLabel(): String = defaultMessagingPackage()
        ?.let(appLabel)
        ?: context.getString(R.string.system_messages)

    fun defaultMessagingPackage(): String? = Telephony.Sms.getDefaultSmsPackage(context)

    fun recentConversationPackages(
        sendAutomatically: Boolean,
        preferredPackage: String?,
    ): Set<String> = resolveRecentConversationPackages(
        sendAutomatically = sendAutomatically,
        preferredPackage = preferredPackage,
        defaultSmsPackage = defaultMessagingPackage(),
        isInstalled = ::isPackageInstalled,
    )

    /** True only when the installed app can receive both a phone number and draft text. */
    fun canAddressConversationDraft(packageName: String): Boolean {
        val defaultPackage = defaultMessagingPackage()
        if (packageName == defaultPackage) return true
        val provider = MessagingProviderCatalog.providerForPackage(packageName) ?: return false
        return verifiedRoutes(provider, packageName).any { it.carriesRecipient }
    }

    /** True when the installed provider still exposes its verified Direct Share activity. */
    fun canDraftToConversationShortcut(
        packageName: String,
        shortcutId: String,
        shortcutCategories: Set<String>,
    ): Boolean = resolvedConversationShortcutIntent(
        packageName = packageName,
        shortcutId = shortcutId,
        shortcutCategories = shortcutCategories,
        body = ROUTE_PROBE_BODY,
    ) != null

    private fun supportsConversationShortcutDrafts(packageName: String): Boolean {
        val route = MessagingShortcutDraftCatalog.routeForPackage(packageName) ?: return false
        return canDraftToConversationShortcut(
            packageName = packageName,
            shortcutId = ROUTE_PROBE_SHORTCUT_ID,
            shortcutCategories = route.shortcutCategories,
        )
    }

    /** Opens the exact provider conversation with the user's text retained as a draft. */
    fun openConversationShortcutDraft(
        packageName: String,
        shortcutId: String,
        shortcutCategories: Set<String>,
        body: String,
    ): Boolean {
        val intent = resolvedConversationShortcutIntent(
            packageName = packageName,
            shortcutId = shortcutId,
            shortcutCategories = shortcutCategories,
            body = body,
        ) ?: return false
        return startActivity(intent, false)
    }

    private fun resolvedConversationShortcutIntent(
        packageName: String,
        shortcutId: String,
        shortcutCategories: Set<String>,
        body: String,
    ): Intent? = conversationShortcutIntents.create(
        packageName = packageName,
        shortcutId = shortcutId,
        shortcutCategories = shortcutCategories,
        body = body,
    )?.takeIf(::canResolve)

    /**
     * Opens a provider-owned draft and falls back to the system SMS composer if every route for
     * the saved integration is missing or no longer accepts its documented intent.
     */
    fun openPreferredMessageDraft(
        recipient: CommunicationRecipient,
        body: String,
        preferredPackage: String?,
    ): PreferredMessageDraftResult {
        val defaultPackage = defaultMessagingPackage()
        val integratedPackage = preferredPackage?.takeIf {
            it.isNotBlank() && it != defaultPackage
        }
        if (integratedPackage == null) {
            return defaultMessageDraftResult(
                integratedPackage = null,
                opened = openDefaultMessageDraft(recipient, body),
            )
        }
        val provider = MessagingProviderCatalog.providerForPackage(integratedPackage)
        if (provider != null) {
            val openedRoute = messageIntents.routes(
                provider,
                recipient.address,
                body,
                integratedPackage,
                forcedRecipient = recipient.userEntered,
            ).firstOrNull { route ->
                canResolve(route.intent) && startActivity(route.intent, false)
            }
            if (openedRoute != null) {
                return if (openedRoute.carriesRecipient) {
                    PreferredMessageDraftResult.OPENED
                } else {
                    PreferredMessageDraftResult.OPENED_WITH_RECIPIENT_PICKER
                }
            }
        }
        return defaultMessageDraftResult(
            integratedPackage = integratedPackage,
            opened = openDefaultMessageDraft(recipient, body),
        )
    }

    /** Android's share sheet carries the body, but messaging apps choose their own recipient. */
    fun chooseMessagingApp(body: String): Boolean {
        val genericShare = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, body.trim())
        return hasHandler(genericShare) && startActivity(genericShare, true)
    }

    private fun isPackageInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0)
    }.isSuccess

    private fun openDefaultMessageDraft(
        recipient: CommunicationRecipient,
        body: String,
    ): Boolean {
        val draft = Intent(
            Intent.ACTION_SENDTO,
            "smsto:${Uri.encode(recipient.address)}".toUri(),
        ).putExtra("sms_body", body.trim())
        val defaultPackage = defaultMessagingPackage()
        if (!defaultPackage.isNullOrBlank()) {
            val explicit = Intent(draft).setPackage(defaultPackage)
            if (canResolve(explicit) && startActivity(explicit, false)) return true
        }
        return canResolve(draft) && startActivity(draft, false)
    }

    private fun canResolve(intent: Intent): Boolean =
        intent.resolveActivity(context.packageManager) != null

    private fun hasHandler(intent: Intent): Boolean =
        context.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        ).isNotEmpty()

    private fun verifiedRoutes(
        provider: MessagingDraftProvider,
        packageName: String,
    ): List<ProviderMessageIntent> = messageIntents.routes(
        provider = provider,
        recipient = ROUTE_PROBE_PHONE,
        body = ROUTE_PROBE_BODY,
        packageName = packageName,
    ).filter { canResolve(it.intent) }

    private companion object {
        const val ROUTE_PROBE_PHONE = "+15551234567"
        const val ROUTE_PROBE_BODY = "MinkLauncher"
        const val ROUTE_PROBE_SHORTCUT_ID = "minklauncher-conversation"
    }
}
