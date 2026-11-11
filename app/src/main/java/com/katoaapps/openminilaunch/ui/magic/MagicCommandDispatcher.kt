package com.katoaapps.openminilaunch.ui.magic

import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.magic.MagicBoxRoute
import com.katoaapps.openminilaunch.features.messaging.MessageDraft
import com.katoaapps.openminilaunch.features.messaging.ConversationShortcutDraft
import com.katoaapps.openminilaunch.features.messaging.MessagingSendRoute
import com.katoaapps.openminilaunch.features.messaging.messagingSendRoute
import com.katoaapps.openminilaunch.platform.DeviceActions

/** Executes a parsed Magic Box command while the composable owns UI handoff state. */
internal fun dispatchMagicCommand(
    route: MagicBoxRoute,
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
    val payload = rawText.trim()
    var keepDraftAfterExternalHandoff = false
    val handled = when (route) {
        MagicBoxRoute.TODO -> payload.isNotBlank().also {
            if (it) {
                store.addTodo(payload)
                onTodoAdded(payload)
            }
        }
        MagicBoxRoute.NOTE -> payload.isNotBlank() && actions.createNote(payload).also { opened ->
            keepDraftAfterExternalHandoff = opened
        }
        // Calendar commands are parsed and reviewed by MagicBox before platform handoff.
        MagicBoxRoute.EVENT -> false
        MagicBoxRoute.MESSAGE -> (selectedRecipient != null && payload.isNotBlank()).also { ready ->
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
                            providerRouteOverridden = recipient.providerRouteOverridden,
                        )
                        onMessage(
                            draft,
                            if (
                                recipient.providerPackage != null ||
                                recipient.providerRouteOverridden
                            ) {
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
        MagicBoxRoute.CALL,
        MagicBoxRoute.APP,
        MagicBoxRoute.ASSISTANT,
        -> false
        MagicBoxRoute.SEARCH -> rawText.isNotBlank() && actions.webSearch(rawText, store.preferredWebPackage).also {
            if (it) store.addSearchQuery(rawText)
        }
    }
    // Todo completion is owned by the UI host. Home can dismiss immediately while its
    // widget animation continues; Assistant keeps its window alive until its feedback
    // animation has finished.
    if (handled && route != MagicBoxRoute.MESSAGE && route != MagicBoxRoute.TODO) {
        if (keepDraftAfterExternalHandoff) onExternalDraftOpened() else onDismiss()
    }
}
