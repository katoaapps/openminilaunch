package com.katoaapps.openminilaunch.ui.conversations

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.Lifecycle
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.conversations.ConversationMessage
import com.katoaapps.openminilaunch.features.conversations.HubConversation
import com.katoaapps.openminilaunch.features.conversations.HubNotification
import com.katoaapps.openminilaunch.features.conversations.NotificationHub
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.PageHeader
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust
import com.katoaapps.openminilaunch.ui.theme.Sage
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ConversationWindow(
    conversation: HubConversation,
    actions: DeviceActions,
    goBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    var replyText by remember(conversation.id) { mutableStateOf("") }
    var replyStatus by remember(conversation.id) { mutableStateOf<Int?>(null) }
    val replyTarget = conversation.replyTarget
    val messageListState = rememberLazyListState()

    fun sendReply() {
        val target = replyTarget ?: return
        if (NotificationHub.reply(target, replyText)) {
            replyText = ""
            replyStatus = R.string.sent
        } else {
            replyStatus = R.string.reply_unavailable
        }
    }

    LaunchedEffect(replyText) {
        if (replyText.isNotBlank()) replyStatus = null
    }
    LaunchedEffect(conversation.messages.size) {
        if (conversation.messages.isNotEmpty()) messageListState.scrollToItem(conversation.messages.size + 1)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        PageHeader(conversation.name, goBack)
        ConversationWindowHeader(conversation, actions, activity)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = Dimens.dp18),
            state = messageListState,
            verticalArrangement = Arrangement.spacedBy(Dimens.dp10),
        ) {
            item { Spacer(Modifier.height(Dimens.dp8)) }
            items(conversation.messages, key = ConversationMessage::id) { message ->
                ConversationBubble(message, conversation.sourcePackages.size > 1, actions)
            }
            item { Spacer(Modifier.height(Dimens.dp8)) }
        }
        ConversationReplyBox(
            replyText = replyText,
            replyStatus = replyStatus,
            replyAvailable = replyTarget != null,
            onReplyTextChange = { replyText = it },
            onSendReply = ::sendReply,
        )
    }
}

@Composable
private fun ConversationWindowHeader(
    conversation: HubConversation,
    actions: DeviceActions,
    activity: ComponentActivity,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.dp20, vertical = Dimens.dp6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConversationSourceIcons(conversation.sourcePackages, actions)
        Text(
            conversation.notifications.map(HubNotification::appName).distinct()
                .joinToString(stringResource(R.string.middle_dot_separator)),
            Modifier.weight(1f).padding(horizontal = Dimens.dp10),
            color = Muted,
            fontSize = Dimens.sp12,
            maxLines = 2,
        )
        OutlinedButton(
            onClick = {
                val target = conversation.openTarget
                if (!NotificationHub.open(context, target)) {
                    if (!actions.launchPackage(target.packageName)) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.could_not_open_app, target.appName),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                } else {
                    scope.launch {
                        delay(500)
                        if (
                            activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                            !actions.launchPackage(target.packageName)
                        ) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.could_not_open_app, target.appName),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
            },
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(Dimens.dp17))
            Text(stringResource(R.string.view_full_conversation), Modifier.padding(start = Dimens.dp6))
        }
    }
}

@Composable
private fun ConversationReplyBox(
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

@Composable
private fun ConversationBubble(
    message: ConversationMessage,
    showSource: Boolean,
    actions: DeviceActions,
) {
    val bubbleColor = if (message.isOutgoing) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val bubbleContentColor = if (message.isOutgoing) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isOutgoing) Arrangement.End else Arrangement.Start,
    ) {
        CompositionLocalProvider(LocalContentColor provides bubbleContentColor) {
            Column(
                Modifier.fillMaxWidth(.82f)
                    .background(bubbleColor, RoundedCornerShape(Dimens.dp18))
                    .padding(horizontal = Dimens.dp14, vertical = Dimens.dp10),
            ) {
                if (!message.isOutgoing && !message.senderName.isNullOrBlank()) {
                    Text(message.senderName, fontSize = Dimens.sp11, fontWeight = FontWeight.Bold)
                }
                Text(
                    message.text,
                    modifier = Modifier.padding(
                        top = if (message.senderName.isNullOrBlank()) Dimens.dp0 else Dimens.dp2,
                    ),
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = Dimens.dp5),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showSource) {
                        ConversationSourceIcon(message.packageName, actions, Dimens.dp14)
                        Text(
                            message.appName,
                            color = bubbleContentColor.copy(alpha = .68f),
                            fontSize = Dimens.sp10,
                            modifier = Modifier.padding(start = Dimens.dp4),
                        )
                        Spacer(Modifier.weight(1f))
                    }
                    Text(
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.timestamp)),
                        color = bubbleContentColor.copy(alpha = .68f),
                        fontSize = Dimens.sp10,
                    )
                }
            }
        }
    }
}
