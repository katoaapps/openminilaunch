package com.katoaapps.openminilaunch.ui.conversations

import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.model.IntegratedContactCallEndpoint
import com.katoaapps.openminilaunch.model.IntegratedContactEndpoint
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.model.launcherShortcutSelectionKey
import com.katoaapps.openminilaunch.platform.DeviceActions

/** Why a saved provider-owned conversation can or cannot receive a new message. */
internal sealed interface FavoriteIntegratedContactAvailability {
    val providerLabel: String

    data class Available(
        override val providerLabel: String,
    ) : FavoriteIntegratedContactAvailability

    data class AppNotInstalled(
        override val providerLabel: String,
        val installAvailable: Boolean,
    ) : FavoriteIntegratedContactAvailability

    data class ConversationUnavailable(
        override val providerLabel: String,
    ) : FavoriteIntegratedContactAvailability
}

internal data class FavoriteIntegratedCallAvailability(
    val appLabel: String,
    val available: Boolean,
)

internal fun DeviceActions.resolveIntegratedContact(
    endpoint: IntegratedContactEndpoint.ConversationShortcut,
): LauncherShortcutTarget? =
    (resolveLauncherSelection(
        launcherShortcutSelectionKey(
            endpoint.userSerial,
            endpoint.packageName,
            endpoint.shortcutId,
        ),
    ) as? LauncherShortcutTarget)?.takeIf(LauncherShortcutTarget::isAvailable)

internal fun DeviceActions.integratedContactAvailability(
    contact: FavoriteIntegratedContact,
): FavoriteIntegratedContactAvailability {
    val endpoint = contact.endpoints.firstOrNull()
        ?: return FavoriteIntegratedContactAvailability.ConversationUnavailable(
            providerLabel = contact.displayName,
        )
    val provider = messagingProviderAvailability(endpoint.packageName)
    return when (endpoint) {
        is IntegratedContactEndpoint.ConversationShortcut -> when {
            resolveIntegratedContact(endpoint) != null ->
                FavoriteIntegratedContactAvailability.Available(provider.label)
            !provider.installed -> FavoriteIntegratedContactAvailability.AppNotInstalled(
                providerLabel = provider.label,
                installAvailable = provider.installUrl != null,
            )
            else -> FavoriteIntegratedContactAvailability.ConversationUnavailable(provider.label)
        }
        is IntegratedContactEndpoint.ProviderAddress -> if (provider.installed) {
            FavoriteIntegratedContactAvailability.Available(provider.label)
        } else {
            FavoriteIntegratedContactAvailability.AppNotInstalled(
                providerLabel = provider.label,
                installAvailable = provider.installUrl != null,
            )
        }
    }
}

internal fun DeviceActions.integratedCallAvailability(
    contact: FavoriteIntegratedContact,
): FavoriteIntegratedCallAvailability? = when (val endpoint = contact.callEndpoint) {
    is IntegratedContactCallEndpoint.PhoneNumber -> FavoriteIntegratedCallAvailability(
        appLabel = endpoint.packageName?.let(::appLabel) ?: defaultCallingAppLabel(),
        available = isCallingAppAvailable(endpoint.number, endpoint.packageName),
    )
    null -> null
}
