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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.magic.hasUsableHardwareKeyboard
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust
import com.katoaapps.openminilaunch.ui.theme.Sage

@Composable
internal fun ConversationReplyBox(
    focusSessionKey: String,
    replyText: String,
    replyStatus: Int?,
    replyAvailable: Boolean,
    onReplyTextChange: (String) -> Unit,
    onSendReply: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember(focusSessionKey) { FocusRequester() }
    var replyFieldPlaced by remember(focusSessionKey) { mutableStateOf(false) }
    val hasHardwareKeyboard = hasUsableHardwareKeyboard(
        configuration.keyboard,
        configuration.hardKeyboardHidden,
    )

    LaunchedEffect(focusSessionKey, replyAvailable, replyFieldPlaced, hasHardwareKeyboard) {
        if (!replyAvailable || !replyFieldPlaced) return@LaunchedEffect

        withFrameNanos { }
        focusRequester.requestFocus()

        // Replies use the platform IME on slabs. Mink's launcher keyboard remains Magic Box-only.
        if (!hasHardwareKeyboard) {
            withFrameNanos { }
            keyboard?.show()
        }
    }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onGloballyPositioned { replyFieldPlaced = true },
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
