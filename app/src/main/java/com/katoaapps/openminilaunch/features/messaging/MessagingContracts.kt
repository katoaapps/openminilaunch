package com.katoaapps.openminilaunch.features.messaging

import com.katoaapps.openminilaunch.model.ContactResult

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** Describes how much information Mink can carry into a provider-owned composer. */
internal enum class MessagingSupportTier {
    CONTACT_AND_DRAFT,
    RECIPIENT_IN_APP,
    CONDITIONAL,
}

/** Decides what pressing Send does before Android or a provider is invoked. */
internal enum class MessagingSendRoute {
    DIRECT_SMS,
    PREFERRED_DRAFT,
    PROVIDER_PICKER,
}

/**
 * "Automatically" means skipping Mink's provider picker. Only System Messages can send in the
 * background. Every other integrated provider still opens its own draft composer.
 */
internal fun messagingSendRoute(
    sendAutomatically: Boolean,
    preferredPackage: String?,
): MessagingSendRoute = when {
    !sendAutomatically -> MessagingSendRoute.PROVIDER_PICKER
    preferredPackage.isNullOrBlank() -> MessagingSendRoute.DIRECT_SMS
    else -> MessagingSendRoute.PREFERRED_DRAFT
}

/** Maps settings saved by older releases into the current two-control messaging model. */
internal fun restoredAutomaticMessageSend(saved: Boolean?, legacyMode: String?): Boolean =
    saved ?: when (legacyMode) {
        "DIRECT_SMS", "PREFERRED_APP", "DEFAULT_MESSENGER", "MESSAGING_APP" -> true
        else -> false
    }

/** The Android or provider-owned contract used to prepare a message draft. */
internal enum class MessagingDraftKind {
    WHATSAPP,
    TELEGRAM,
    LINE,
    SMS_URI,
    GENERIC_SHARE,
}

/**
 * Static provider metadata used even when an app is not installed.
 *
 * packageName is the canonical package used for store links and ghost entries. Alternate package
 * names represent official builds of the same provider. The provider id remains stable across all
 * variants. Store and documentation URLs preserve the source used to verify each integration even
 * when the runtime picker does not open those links.
 */
internal data class MessagingDraftProvider(
    val id: String,
    @param:StringRes val labelRes: Int,
    val packageName: String,
    val alternatePackageNames: List<String> = emptyList(),
    val kind: MessagingDraftKind,
    val supportTier: MessagingSupportTier,
    @param:DrawableRes val bundledIconRes: Int? = null,
    val storeUrl: String,
    val documentationUrl: String,
)

internal val MessagingDraftProvider.packageNames: List<String>
    get() = listOf(packageName) + alternatePackageNames

internal fun MessagingDraftProvider.resolveInstalledPackage(
    isInstalled: (String) -> Boolean,
): String? = packageNames.firstOrNull(isInstalled)

/** Resolved provider state used by both Settings and the one-time send chooser. */
internal data class MessagingProviderOption(
    val id: String,
    val label: String,
    val preferencePackageName: String?,
    val installedPackageName: String?,
    val supportTier: MessagingSupportTier,
    @param:DrawableRes val bundledIconRes: Int?,
    val installed: Boolean,
    val selectable: Boolean,
    val systemDefault: Boolean = false,
)

internal enum class PreferredMessageDraftResult {
    OPENED,
    OPENED_WITH_RECIPIENT_PICKER,
    FALLBACK_OPENED,
    FAILED,
}

/** Contact and body retained by Magic Mode until a direct send or provider draft is complete. */
internal data class MessageDraft(
    val contact: ContactResult,
    val body: String,
)

internal fun defaultMessageDraftResult(
    integratedPackage: String?,
    opened: Boolean,
): PreferredMessageDraftResult = when {
    !opened -> PreferredMessageDraftResult.FAILED
    integratedPackage.isNullOrBlank() -> PreferredMessageDraftResult.OPENED
    else -> PreferredMessageDraftResult.FALLBACK_OPENED
}
