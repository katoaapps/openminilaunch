package com.katoaapps.openminilaunch.ui.conversations

import com.katoaapps.openminilaunch.features.contacts.FAVORITE_INTEGRATED_CONTACT_NAME_MAX_CHARS
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContactNameSource
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
        val shortcut: LauncherShortcutTarget? = null,
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

/** Applies live provider labels only while the user has not deliberately chosen an alias. */
internal fun DeviceActions.presentedIntegratedContact(
    contact: FavoriteIntegratedContact,
): FavoriteIntegratedContact {
    if (contact.nameSource == FavoriteIntegratedContactNameSource.USER) return contact
    val shortcut = contact.endpoints
        .filterIsInstance<IntegratedContactEndpoint.ConversationShortcut>()
        .firstNotNullOfOrNull(::resolveIntegratedContact)
    val providerName = shortcut?.label
        ?.trim()
        ?.take(FAVORITE_INTEGRATED_CONTACT_NAME_MAX_CHARS)
        ?.takeIf(String::isNotBlank)
        ?: return contact
    return if (providerName == contact.displayName) contact else contact.copy(displayName = providerName)
}

internal fun DeviceActions.integratedContactAvailability(
    contact: FavoriteIntegratedContact,
): FavoriteIntegratedContactAvailability {
    val endpoint = contact.endpoints.firstOrNull()
        ?: return FavoriteIntegratedContactAvailability.ConversationUnavailable(
            providerLabel = contact.displayName,
        )
    val provider = messagingProviderAvailability(endpoint.packageName)
    return when (endpoint) {
        is IntegratedContactEndpoint.ConversationShortcut -> when (
            val shortcut = resolveIntegratedContact(endpoint)
        ) {
            null -> if (!provider.installed) {
                FavoriteIntegratedContactAvailability.AppNotInstalled(
                    providerLabel = provider.label,
                    installAvailable = provider.installUrl != null,
                )
            } else {
                FavoriteIntegratedContactAvailability.ConversationUnavailable(provider.label)
            }
            else -> FavoriteIntegratedContactAvailability.Available(provider.label, shortcut)
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
