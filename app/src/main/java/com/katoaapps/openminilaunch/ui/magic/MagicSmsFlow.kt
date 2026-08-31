package com.katoaapps.openminilaunch.ui.magic

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.messaging.MessageDraft
import com.katoaapps.openminilaunch.platform.DirectSmsResult
import com.katoaapps.openminilaunch.features.messaging.PreferredMessageDraftResult
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.launcher.isPermanentlyDenied
import com.katoaapps.openminilaunch.ui.launcher.supportsDirectSms
import kotlinx.coroutines.delay

@Stable
internal class MagicSmsFlow internal constructor() {
    var draftToConfirm by mutableStateOf<MessageDraft?>(null)
        internal set
    var assistantDisclosureVisible by mutableStateOf(false)
        internal set
    var sentConfirmationVisible by mutableStateOf(false)
        internal set

    internal var pendingPermissionDraft: MessageDraft? = null
    internal var pendingAssistantDraft: MessageDraft? = null
    internal var sentConfirmationToken by mutableIntStateOf(0)
    internal var requestDirectAction: (MessageDraft) -> Unit = {}
    internal var continueAssistantAction: () -> Unit = {}
    internal var dismissAssistantAction: () -> Unit = {}

    fun requestDirect(draft: MessageDraft) = requestDirectAction(draft)

    fun confirmDraft() {
        draftToConfirm?.let { draft ->
            draftToConfirm = null
            requestDirect(draft)
        }
    }

    fun chooseMessagingApp(onDraft: (MessageDraft) -> Unit) {
        draftToConfirm?.let { draft ->
            draftToConfirm = null
            onDraft(draft)
        }
    }

    fun dismissDraft(onSessionComplete: () -> Unit) {
        draftToConfirm = null
        onSessionComplete()
    }

    fun continueAssistantSelection() = continueAssistantAction()

    fun dismissAssistantDisclosure() = dismissAssistantAction()
}

/** Owns the direct-SMS permission and Assistant-role handoff state machine. */
@Composable
internal fun rememberMagicSmsFlow(
    actions: DeviceActions,
    onSessionComplete: () -> Unit,
): MagicSmsFlow {
    val context = LocalContext.current
    val currentOnSessionComplete by rememberUpdatedState(onSessionComplete)
    val flow = remember { MagicSmsFlow() }

    fun completeSmsAttempt(draft: MessageDraft) {
        fun openComposerFallback(message: String) {
            val result = actions.openPreferredMessageDraft(
                draft.contact,
                draft.body,
                preferredPackage = null,
            )
            Toast.makeText(
                context,
                if (result != PreferredMessageDraftResult.FAILED) {
                    message
                } else {
                    context.getString(R.string.sms_no_compatible_fallback)
                },
                Toast.LENGTH_LONG,
            ).show()
        }

        when (actions.sendSmsDirect(draft.contact.phone, draft.body)) {
            DirectSmsResult.QUEUED -> {
                flow.sentConfirmationVisible = true
                flow.sentConfirmationToken++
            }
            DirectSmsResult.NO_DEFAULT_SUBSCRIPTION -> {
                openComposerFallback(context.getString(R.string.choose_sim_in_messaging_app))
                currentOnSessionComplete()
            }
            DirectSmsResult.NOT_AUTHORIZED,
            DirectSmsResult.UNSUPPORTED,
            DirectSmsResult.FAILED,
            -> {
                openComposerFallback(context.getString(R.string.direct_sms_unavailable))
                currentOnSessionComplete()
            }
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val draft = flow.pendingPermissionDraft
        flow.pendingPermissionDraft = null
        if (granted && draft != null) {
            completeSmsAttempt(draft)
        } else {
            if (!granted && isPermanentlyDenied(context, Manifest.permission.SEND_SMS)) {
                actions.openAppSettings()
            }
            currentOnSessionComplete()
        }
    }
    val assistantSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        val draft = flow.pendingAssistantDraft
        flow.pendingAssistantDraft = null
        if (draft != null && actions.isAssistantRoleHeld()) {
            if (
                ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                completeSmsAttempt(draft)
            } else {
                flow.pendingPermissionDraft = draft
                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            }
        } else if (draft != null) {
            flow.draftToConfirm = draft
            Toast.makeText(
                context,
                context.getString(R.string.choose_assistant_for_sms),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    flow.requestDirectAction = { draft ->
        when {
            !supportsDirectSms(context) -> completeSmsAttempt(draft)
            !actions.isAssistantRoleHeld() -> {
                flow.pendingAssistantDraft = draft
                flow.assistantDisclosureVisible = true
            }
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED -> completeSmsAttempt(draft)
            else -> {
                flow.pendingPermissionDraft = draft
                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            }
        }
    }
    flow.continueAssistantAction = {
        flow.assistantDisclosureVisible = false
        assistantSettingsLauncher.launch(actions.assistantRoleSelectionIntent())
    }
    flow.dismissAssistantAction = {
        flow.assistantDisclosureVisible = false
        flow.pendingAssistantDraft?.let { draft ->
            flow.pendingAssistantDraft = null
            flow.draftToConfirm = draft
        } ?: currentOnSessionComplete()
    }

    LaunchedEffect(flow.sentConfirmationToken) {
        if (flow.sentConfirmationToken > 0) {
            delay(1_100)
            flow.sentConfirmationVisible = false
            delay(350)
            currentOnSessionComplete()
        }
    }
    return flow
}
