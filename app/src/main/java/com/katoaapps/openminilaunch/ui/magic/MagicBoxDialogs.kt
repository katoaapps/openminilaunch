package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.messaging.MessageDraft
import com.katoaapps.openminilaunch.model.CommunicationRecipient
import com.katoaapps.openminilaunch.model.looksLikePhoneRecipient
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MagicCallColor
import com.katoaapps.openminilaunch.ui.theme.MagicTextColor
import com.katoaapps.openminilaunch.ui.theme.MinkWhite
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun MagicDiscardDialog(
    title: String,
    description: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onDismiss,
        properties = MinkDialogDefaults.properties,
        icon = { Icon(Icons.Default.DeleteOutline, null) },
        title = { Text(title) },
        text = { Text(description) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
internal fun CallConfirmationDialog(
    recipient: CommunicationRecipient,
    onCallNow: () -> Unit,
    onChooseCallingApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    val requiresCallingApp = recipient.userEntered &&
        !recipient.address.looksLikePhoneRecipient()
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onDismiss,
        properties = MinkDialogDefaults.properties,
        icon = { Icon(Icons.Default.Phone, null, tint = MagicCallColor) },
        title = { Text(stringResource(R.string.call_recipient, recipient.displayName)) },
        text = { Text(stringResource(R.string.phone_type_and_number, recipient.detail, recipient.address)) },
        confirmButton = {
            Button(
                onClick = onCallNow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MagicCallColor,
                    contentColor = MinkWhite,
                ),
            ) {
                Text(
                    stringResource(
                        if (requiresCallingApp) R.string.choose_calling_app else R.string.call_now,
                    ),
                )
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                if (!requiresCallingApp) {
                    TextButton(onClick = onChooseCallingApp) {
                        Text(stringResource(R.string.choose_calling_app))
                    }
                }
            }
        },
    )
}

@Composable
internal fun DirectSmsConfirmationDialog(
    draft: MessageDraft,
    assistantActive: Boolean,
    onSend: () -> Unit,
    onChooseMessagingApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onDismiss,
        properties = MinkDialogDefaults.properties,
        icon = { Icon(Icons.AutoMirrored.Filled.Send, null, tint = MagicTextColor) },
        title = { Text(stringResource(R.string.send_message_to_recipient, draft.recipient.displayName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp10)) {
                Text(
                    stringResource(
                        R.string.phone_type_and_number,
                        draft.recipient.detail,
                        draft.recipient.address,
                    ),
                )
                Text(
                    draft.body,
                    Modifier.heightIn(max = Dimens.dp180).verticalScroll(rememberScrollState()),
                )
                Text(stringResource(R.string.sms_carrier_notice), color = Muted, fontSize = Dimens.sp12)
                if (!assistantActive) {
                    Text(stringResource(R.string.assistant_sms_restriction), color = Muted, fontSize = Dimens.sp12)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSend,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MagicTextColor,
                    contentColor = MinkWhite,
                ),
            ) {
                Text(stringResource(if (assistantActive) R.string.send_sms_now else R.string.choose_assistant))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                TextButton(onClick = onChooseMessagingApp) {
                    Text(stringResource(R.string.choose_messaging_app))
                }
            }
        },
    )
}
