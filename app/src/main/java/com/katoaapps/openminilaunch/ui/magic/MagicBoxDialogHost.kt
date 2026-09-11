package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderCatalog
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderOption
import com.katoaapps.openminilaunch.features.ai.AiProviderOption
import com.katoaapps.openminilaunch.features.messaging.MessageDraft
import com.katoaapps.openminilaunch.model.CommunicationRecipient
import com.katoaapps.openminilaunch.model.LaunchableApp
import com.katoaapps.openminilaunch.ui.onboarding.FileSearchScopeDialog
import com.katoaapps.openminilaunch.ui.settings.AiProviderPickerDialog
import com.katoaapps.openminilaunch.ui.settings.AppPickerDialog
import com.katoaapps.openminilaunch.ui.settings.AssistantDisclosureDialog
import com.katoaapps.openminilaunch.ui.settings.MessagingProviderPickerDialog

@Composable
internal fun MagicDraftDialogHost(
    showNoteDelete: Boolean,
    showMessageDiscard: Boolean,
    showCommandDiscard: Boolean,
    showFileScopeChoice: Boolean,
    showSmsAssistantDisclosure: Boolean,
    onDeleteNote: () -> Unit,
    onKeepNote: () -> Unit,
    onDiscardMessage: () -> Unit,
    onKeepMessage: () -> Unit,
    onDiscardCommand: () -> Unit,
    onKeepCommand: () -> Unit,
    onChooseFolder: () -> Unit,
    onSkipFolder: () -> Unit,
    onAssistantContinue: () -> Unit,
    onAssistantDismiss: () -> Unit,
) {
    if (showNoteDelete) {
        MagicDiscardDialog(
            title = stringResource(R.string.delete_note_draft_title),
            description = stringResource(R.string.delete_note_draft_description),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = onDeleteNote,
            onDismiss = onKeepNote,
        )
    }
    if (showMessageDiscard) {
        MagicDiscardDialog(
            title = stringResource(R.string.discard_message_draft_title),
            description = stringResource(R.string.discard_message_draft_description),
            confirmLabel = stringResource(R.string.discard),
            onConfirm = onDiscardMessage,
            onDismiss = onKeepMessage,
        )
    }
    if (showCommandDiscard) {
        MagicDiscardDialog(
            title = stringResource(R.string.discard_magic_input_title),
            description = stringResource(R.string.discard_magic_input_description),
            confirmLabel = stringResource(R.string.discard),
            onConfirm = onDiscardCommand,
            onDismiss = onKeepCommand,
        )
    }
    if (showFileScopeChoice) {
        FileSearchScopeDialog(onChooseFolder = onChooseFolder, onSkip = onSkipFolder)
    }
    if (showSmsAssistantDisclosure) {
        AssistantDisclosureDialog(
            active = false,
            onContinue = onAssistantContinue,
            onDismiss = onAssistantDismiss,
        )
    }
}

@Composable
internal fun MagicPickerDialogHost(
    messageDraft: MessageDraft?,
    messagingOptions: List<MessagingProviderOption>,
    messagingOptionsLoaded: Boolean,
    preferredMessagingPackage: String?,
    showAiPicker: Boolean,
    showAllAiApps: Boolean,
    aiProviders: List<AiProviderOption>,
    aiProvidersLoaded: Boolean,
    allAiApps: List<LaunchableApp>,
    allAiAppsLoaded: Boolean,
    preferredAiPackage: String?,
    onMessagingProvider: (MessageDraft, MessagingProviderOption) -> Unit,
    onSeeAllMessagingApps: (MessageDraft) -> Unit,
    onDismissMessaging: () -> Unit,
    onSeeAllAiApps: () -> Unit,
    onAiProvider: (AiProviderOption) -> Unit,
    onInstallAiProvider: (AiProviderOption) -> Unit,
    onDismissCuratedAi: () -> Unit,
    onAllAiApp: (LaunchableApp) -> Unit,
    onDismissAllAi: () -> Unit,
) {
    messageDraft?.let { draft ->
        MessagingProviderPickerDialog(
            title = stringResource(R.string.send_with),
            options = messagingOptions,
            loading = !messagingOptionsLoaded,
            selectedProviderId = MessagingProviderCatalog.providerForPackage(
                preferredMessagingPackage,
            )?.id ?: MessagingProviderCatalog.SYSTEM_DEFAULT_PROVIDER_ID,
            showUnavailable = false,
            onProvider = { onMessagingProvider(draft, it) },
            onSeeAllApps = { onSeeAllMessagingApps(draft) },
            onDismiss = onDismissMessaging,
        )
    }
    if (showAiPicker) {
        AiProviderPickerDialog(
            title = stringResource(R.string.choose_ai_app),
            options = aiProviders,
            selectedPackage = preferredAiPackage,
            loading = !aiProvidersLoaded,
            showUnavailable = true,
            onProvider = onAiProvider,
            onInstall = onInstallAiProvider,
            onSeeAllApps = onSeeAllAiApps,
            onDismiss = onDismissCuratedAi,
        )
    }
    if (showAllAiApps) {
        AppPickerDialog(
            title = stringResource(R.string.other_compatible_apps),
            apps = allAiApps,
            selected = setOfNotNull(preferredAiPackage),
            loading = !allAiAppsLoaded,
            emptyMessage = stringResource(R.string.no_other_text_apps),
            onApp = onAllAiApp,
            onDismiss = onDismissAllAi,
        )
    }
}

@Composable
internal fun MagicCommunicationDialogHost(
    callRecipient: CommunicationRecipient?,
    smsDraft: MessageDraft?,
    assistantActive: Boolean,
    onCallNow: (CommunicationRecipient) -> Unit,
    onChooseCallingApp: (CommunicationRecipient) -> Unit,
    onDismissCall: () -> Unit,
    onSendSms: (MessageDraft) -> Unit,
    onChooseMessagingApp: (MessageDraft) -> Unit,
    onDismissSms: () -> Unit,
) {
    callRecipient?.let { recipient ->
        CallConfirmationDialog(
            recipient = recipient,
            onCallNow = { onCallNow(recipient) },
            onChooseCallingApp = { onChooseCallingApp(recipient) },
            onDismiss = onDismissCall,
        )
    }
    smsDraft?.let { draft ->
        DirectSmsConfirmationDialog(
            draft = draft,
            assistantActive = assistantActive,
            onSend = { onSendSms(draft) },
            onChooseMessagingApp = { onChooseMessagingApp(draft) },
            onDismiss = onDismissSms,
        )
    }
}

internal data class MessagingOptionsLoadState(
    val options: List<MessagingProviderOption> = emptyList(),
    val loaded: Boolean = false,
)
