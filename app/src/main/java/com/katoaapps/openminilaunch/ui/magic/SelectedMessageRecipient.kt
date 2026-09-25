package com.katoaapps.openminilaunch.ui.magic

import com.katoaapps.openminilaunch.model.CommunicationRecipient
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import org.json.JSONObject

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

internal fun SelectedMessageRecipient.snapshot(): String = JSONObject().apply {
    when (this@snapshot) {
        is SelectedMessageRecipient.AddressRecipient -> {
            put("type", "address")
            put("address", recipient.address)
            put("name", recipient.displayName)
            put("detail", recipient.detail)
            put("entered", recipient.userEntered)
            put("package", providerPackage.orEmpty())
        }
        is SelectedMessageRecipient.ConversationRecipient -> {
            put("type", "conversation")
            put("label", conversationShortcut.label)
            put("package", conversationShortcut.packageName)
            put("id", conversationShortcut.shortcutId)
            put("serial", conversationShortcut.userSerial)
            put("work", conversationShortcut.isWorkProfile)
            put("available", conversationShortcut.isAvailable)
            put("key", conversationShortcut.selectionKey)
        }
    }
}.toString()

internal fun recipientFromSnapshot(snapshot: String): SelectedMessageRecipient? = runCatching {
    val json = JSONObject(snapshot)
    when (json.getString("type")) {
        "address" -> SelectedMessageRecipient.AddressRecipient(
            recipient = CommunicationRecipient(
                address = json.getString("address"),
                displayName = json.getString("name"),
                detail = json.getString("detail"),
                userEntered = json.optBoolean("entered"),
            ),
            providerPackage = json.optString("package").takeIf(String::isNotBlank),
        )
        "conversation" -> SelectedMessageRecipient.ConversationRecipient(
            LauncherShortcutTarget(
                label = json.getString("label"),
                packageName = json.getString("package"),
                shortcutId = json.getString("id"),
                userSerial = json.getLong("serial"),
                isWorkProfile = json.getBoolean("work"),
                isAvailable = json.optBoolean("available", true),
                selectionKey = json.getString("key"),
            ),
        )
        else -> null
    }
}.getOrNull()
