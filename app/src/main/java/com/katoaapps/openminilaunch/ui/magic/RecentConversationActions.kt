package com.katoaapps.openminilaunch.ui.magic

import android.content.Context
import android.widget.Toast
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.apps.launcherDiscoveryLabel
import com.katoaapps.openminilaunch.features.messaging.ConversationShortcutDraft
import com.katoaapps.openminilaunch.features.messaging.ConversationShortcutDraftResult
import com.katoaapps.openminilaunch.features.messaging.RecentConversationSelection
import com.katoaapps.openminilaunch.features.messaging.resolveRecentConversationSelection
import com.katoaapps.openminilaunch.model.ContactResult
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.platform.DeviceActions

/** Applies a recent-conversation tap without leaking its branching rules into MagicBox. */
internal fun handleRecentConversationTap(
    context: Context,
    shortcut: LauncherShortcutTarget,
    canSearchContacts: Boolean,
    actions: DeviceActions,
    onShortcutDraft: (LauncherShortcutTarget) -> Unit,
    onDraftRecipient: (ContactResult, String) -> Unit,
    onDirectOpen: () -> Unit,
) {
    when (val selection = resolveRecentConversationSelection(
        shortcut = shortcut,
        canSearchContacts = canSearchContacts,
        canDraftToShortcut = actions::canDraftToConversationShortcut,
        canAddressDraft = actions::canAddressConversationDraft,
        searchContacts = { actions.searchContacts(it) },
    )) {
        is RecentConversationSelection.ShortcutDraft -> onShortcutDraft(selection.shortcut)
        is RecentConversationSelection.DraftRecipient -> onDraftRecipient(
            selection.contact,
            selection.packageName,
        )
        is RecentConversationSelection.DirectOpen -> {
            if (actions.launchLauncherTarget(selection.shortcut)) {
                onDirectOpen()
            } else {
                val displayLabel = launcherDiscoveryLabel(selection.shortcut, actions::appLabel)
                Toast.makeText(
                    context,
                    context.getString(R.string.launcher_app_unavailable, displayLabel),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }
}

/** Reports a guarded shortcut-draft handoff without adding provider plumbing to MagicBox. */
internal fun handleRecentConversationDraftSend(
    context: Context,
    draft: ConversationShortcutDraft,
    actions: DeviceActions,
    onFailure: () -> Unit,
) {
    when (actions.openConversationShortcutDraft(draft.shortcut, draft.body)) {
        ConversationShortcutDraftResult.DRAFT_OPENED -> Unit
        ConversationShortcutDraftResult.CONVERSATION_OPENED -> Toast.makeText(
            context,
            context.getString(R.string.conversation_draft_fallback),
            Toast.LENGTH_LONG,
        ).show()
        ConversationShortcutDraftResult.FAILED -> {
            val label = launcherDiscoveryLabel(draft.shortcut, actions::appLabel)
            Toast.makeText(
                context,
                context.getString(R.string.launcher_app_unavailable, label),
                Toast.LENGTH_LONG,
            ).show()
            onFailure()
        }
    }
}
