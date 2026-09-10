package com.katoaapps.openminilaunch.ui.magic

import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.magic.MAGIC_NOTE_PREFIX
import com.katoaapps.openminilaunch.features.messaging.MessageDraft
import com.katoaapps.openminilaunch.features.messaging.ConversationShortcutDraft
import com.katoaapps.openminilaunch.features.messaging.MessagingSendRoute
import com.katoaapps.openminilaunch.features.messaging.messagingSendRoute
import com.katoaapps.openminilaunch.platform.DeviceActions

/** Executes a parsed Magic Box command while the composable owns UI handoff state. */
internal fun dispatchMagicCommand(
    prefix: Char?,
    lockedPrefix: Char?,
    rawText: String,
    selectedRecipient: SelectedMessageRecipient?,
    store: LauncherStore,
    actions: DeviceActions,
    onTodoAdded: (String) -> Unit,
    onExternalDraftOpened: () -> Unit,
    onMessage: (MessageDraft, MessagingSendRoute) -> Unit,
    onConversationShortcutMessage: (ConversationShortcutDraft) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val payload = if (lockedPrefix != null) rawText.trim() else rawText.drop(1).trim()
    var keepDraftAfterExternalHandoff = false
    val handled = when (prefix) {
        '-' -> payload.isNotBlank().also {
            if (it) {
                store.addTodo(payload)
                onTodoAdded(payload)
            }
        }
        MAGIC_NOTE_PREFIX -> payload.isNotBlank() && actions.createNote(payload).also { opened ->
            keepDraftAfterExternalHandoff = opened
        }
        '+' -> payload.isNotBlank() && actions.createEvent(payload)
        '@' -> (selectedRecipient != null && payload.isNotBlank()).also { ready ->
            if (ready) {
                when (val recipient = checkNotNull(selectedRecipient)) {
                    is SelectedMessageRecipient.ConversationRecipient -> {
                        onConversationShortcutMessage(
                            ConversationShortcutDraft(recipient.conversationShortcut, payload),
                        )
                    }
                    is SelectedMessageRecipient.AddressRecipient -> {
                        val draft = MessageDraft(
                            recipient = recipient.recipient,
                            body = payload,
                            conversationPackage = recipient.providerPackage,
                        )
                        onMessage(
                            draft,
                            if (recipient.providerPackage != null) {
                                MessagingSendRoute.PREFERRED_DRAFT
                            } else {
                                messagingSendRoute(
                                    sendAutomatically = store.sendMessagesAutomatically,
                                    preferredPackage = store.preferredMessagingPackage,
                                )
                            },
                        )
                    }
                }
            }
        }
        '#', '?' -> false
        else -> rawText.isNotBlank() && actions.webSearch(rawText, store.preferredWebPackage).also {
            if (it) store.addSearchQuery(rawText)
        }
    }
    if (handled && prefix != '@') {
        if (keepDraftAfterExternalHandoff) onExternalDraftOpened() else onDismiss()
    }
}
