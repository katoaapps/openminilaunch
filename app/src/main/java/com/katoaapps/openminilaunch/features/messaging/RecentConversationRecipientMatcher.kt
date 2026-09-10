package com.katoaapps.openminilaunch.features.messaging

import com.katoaapps.openminilaunch.model.ContactResult
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import java.text.Normalizer
import java.util.Locale

/**
 * Safely bridges a conversation shortcut label to an Android phone contact.
 *
 * LauncherApps deliberately hides a shortcut's intent and recipient identifier. An exact,
 * unique display-name match is therefore the only provider-independent recipient mapping Mink
 * can make without guessing. Multiple phone rows remain ambiguous and are not selected.
 */
internal fun uniqueContactForConversation(
    conversationLabel: String,
    contacts: List<ContactResult>,
): ContactResult? {
    val normalizedLabel = normalizedContactName(conversationLabel)
    if (normalizedLabel.isEmpty()) return null

    return contacts
        .filter { normalizedContactName(it.name) == normalizedLabel }
        .distinctBy { "${it.contactUri}:${it.phone.filter(Char::isDigit)}" }
        .singleOrNull()
}

internal sealed interface RecentConversationSelection {
    data class ShortcutDraft(
        val shortcut: LauncherShortcutTarget,
    ) : RecentConversationSelection

    data class DraftRecipient(
        val contact: ContactResult,
        val packageName: String,
    ) : RecentConversationSelection

    data class DirectOpen(
        val shortcut: LauncherShortcutTarget,
    ) : RecentConversationSelection
}

/** Chooses draft mode only when Mink can identify and address the recipient without guessing. */
internal fun resolveRecentConversationSelection(
    shortcut: LauncherShortcutTarget,
    canSearchContacts: Boolean,
    canDraftToShortcut: (LauncherShortcutTarget) -> Boolean,
    canAddressDraft: (String) -> Boolean,
    searchContacts: (String) -> List<ContactResult>,
): RecentConversationSelection {
    if (!shortcut.isWorkProfile && canDraftToShortcut(shortcut)) {
        return RecentConversationSelection.ShortcutDraft(shortcut)
    }
    val matchedContact = if (
        canSearchContacts &&
        !shortcut.isWorkProfile &&
        canAddressDraft(shortcut.packageName)
    ) {
        uniqueContactForConversation(shortcut.label, searchContacts(shortcut.label))
    } else {
        null
    }
    return matchedContact?.let {
        RecentConversationSelection.DraftRecipient(it, shortcut.packageName)
    } ?: RecentConversationSelection.DirectOpen(shortcut)
}

private fun normalizedContactName(value: String): String = Normalizer
    .normalize(value, Normalizer.Form.NFKC)
    .trim()
    .replace(Regex("\\s+"), " ")
    .lowercase(Locale.ROOT)
