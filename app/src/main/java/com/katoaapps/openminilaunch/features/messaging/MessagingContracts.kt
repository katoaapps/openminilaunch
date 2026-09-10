package com.katoaapps.openminilaunch.features.messaging

import com.katoaapps.openminilaunch.model.CommunicationRecipient
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget

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

/** The Android or provider-owned contract used to prepare a message draft. */
internal enum class MessagingDraftKind {
    WHATSAPP,
    TELEGRAM,
    LINE,
    SIGNAL_CONTACT,
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
    val systemDefault: Boolean = false,
    val supportsRecentChatDrafts: Boolean = false,
    /** Installed, but Mink could not pre-verify this provider's current draft route. */
    val betaCompatibility: Boolean = false,
)

internal val MessagingProviderOption.isDraftReady: Boolean
    get() = supportTier == MessagingSupportTier.CONTACT_AND_DRAFT || supportsRecentChatDrafts

internal enum class PreferredMessageDraftResult {
    OPENED,
    OPENED_WITH_RECIPIENT_PICKER,
    FALLBACK_OPENED,
    FAILED,
}

internal enum class ConversationShortcutDraftResult {
    DRAFT_OPENED,
    CONVERSATION_OPENED,
    FAILED,
}

/** Recipient and body retained by Magic Mode until a direct send or provider draft is complete. */
internal data class MessageDraft(
    val recipient: CommunicationRecipient,
    val body: String,
    /** Explicit app selected through a recent conversation; null keeps normal message settings. */
    val conversationPackage: String? = null,
)

/** A provider-published conversation selected before the user types the message body. */
internal data class ConversationShortcutDraft(
    val shortcut: LauncherShortcutTarget,
    val body: String,
)
