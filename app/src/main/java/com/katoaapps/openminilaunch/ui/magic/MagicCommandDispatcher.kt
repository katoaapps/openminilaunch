package com.katoaapps.openminilaunch.ui.magic

import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.magic.MAGIC_NOTE_PREFIX
import com.katoaapps.openminilaunch.features.messaging.MessageDraft
import com.katoaapps.openminilaunch.features.messaging.MessagingSendRoute
import com.katoaapps.openminilaunch.features.messaging.messagingSendRoute
import com.katoaapps.openminilaunch.model.ContactResult
import com.katoaapps.openminilaunch.platform.DeviceActions

/** Executes a parsed Magic Box command while the composable owns UI handoff state. */
internal fun dispatchMagicCommand(
    prefix: Char?,
    lockedPrefix: Char?,
    rawText: String,
    selectedContact: ContactResult?,
    store: LauncherStore,
    actions: DeviceActions,
    onTodoAdded: (String) -> Unit,
    onExternalDraftOpened: () -> Unit,
    onMessage: (MessageDraft, MessagingSendRoute) -> Unit,
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
        '@' -> (selectedContact != null && payload.isNotBlank()).also { ready ->
            if (ready) {
                val draft = MessageDraft(checkNotNull(selectedContact), payload)
                onMessage(
                    draft,
                    messagingSendRoute(
                        sendAutomatically = store.sendMessagesAutomatically,
                        preferredPackage = store.preferredMessagingPackage,
                    ),
                )
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
