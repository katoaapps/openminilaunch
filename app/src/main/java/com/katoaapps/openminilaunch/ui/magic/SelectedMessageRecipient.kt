package com.katoaapps.openminilaunch.ui.magic

import com.katoaapps.openminilaunch.model.CommunicationRecipient
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget

/** One mutually exclusive recipient selection retained while the user writes an @ message. */
internal sealed interface SelectedMessageRecipient {
    val recipient: CommunicationRecipient?
    val conversationShortcut: LauncherShortcutTarget?
    val providerPackage: String?
    val label: String

    data class AddressRecipient(
        override val recipient: CommunicationRecipient,
        override val providerPackage: String? = null,
    ) : SelectedMessageRecipient {
        override val conversationShortcut: LauncherShortcutTarget? = null
        override val label: String = recipient.displayName
    }

    data class ConversationRecipient(
        override val conversationShortcut: LauncherShortcutTarget,
    ) : SelectedMessageRecipient {
        override val recipient: CommunicationRecipient? = null
        override val providerPackage: String = conversationShortcut.packageName
        override val label: String = conversationShortcut.label
    }
}

internal fun SelectedMessageRecipient.detail(appLabel: (String) -> String): String? =
    providerPackage?.let(appLabel) ?: recipient?.detail
