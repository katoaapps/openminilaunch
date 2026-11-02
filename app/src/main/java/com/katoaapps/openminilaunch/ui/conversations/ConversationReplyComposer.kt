package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust
import com.katoaapps.openminilaunch.ui.theme.Sage

@Composable
internal fun ConversationReplyBox(
    replyText: String,
    replyStatus: Int?,
    replyAvailable: Boolean,
    onReplyTextChange: (String) -> Unit,
    onSendReply: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Dimens.dp18, vertical = Dimens.dp10)) {
        replyStatus?.let { status ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = Dimens.dp6),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (status == R.string.sent) {
                    Icon(Icons.Default.Check, null, Modifier.size(Dimens.dp16), tint = Sage)
                }
                Text(
                    stringResource(status),
                    color = if (status == R.string.sent) Sage else Rust,
                    fontSize = Dimens.sp12,
                    modifier = Modifier.padding(start = Dimens.dp4),
                )
            }
        }
        if (replyAvailable) {
            OutlinedTextField(
                value = replyText,
                onValueChange = onReplyTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.reply)) },
                minLines = 1,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = { if (replyText.isNotBlank()) onSendReply() },
                ),
                trailingIcon = {
                    FilledIconButton(onClick = onSendReply, enabled = replyText.isNotBlank()) {
                        Icon(Icons.AutoMirrored.Filled.Reply, stringResource(R.string.send_reply))
                    }
                },
            )
        } else {
            Text(stringResource(R.string.inline_reply_unavailable), color = Muted, fontSize = Dimens.sp12)
        }
    }
}
