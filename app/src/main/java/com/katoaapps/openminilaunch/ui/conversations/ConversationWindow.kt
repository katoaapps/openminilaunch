package com.katoaapps.openminilaunch.ui.conversations

import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.Lifecycle
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.conversations.ConversationMessage
import com.katoaapps.openminilaunch.features.conversations.ConversationNotification
import com.katoaapps.openminilaunch.features.conversations.ConversationNotificationStore
import com.katoaapps.openminilaunch.features.conversations.NotificationConversation
import com.katoaapps.openminilaunch.model.FavoriteContact
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.PageHeader
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ConversationWindow(
    conversation: NotificationConversation,
    displayName: String,
    favoriteContact: FavoriteContact?,
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
        if (ConversationNotificationStore.sendReply(target, replyText)) {
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
        PageHeader(displayName, goBack)
        ConversationWindowHeader(conversation, favoriteContact, actions, activity)
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
        ConversationActionRow(
            conversationActions = conversation.actions,
            dismissAvailable = conversation.canDismiss,
            actions = actions,
            onAction = { action ->
                if (!ConversationNotificationStore.performAction(context, action)) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.conversation_action_unavailable),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            onDismiss = {
                if (!ConversationNotificationStore.dismissConversation(conversation)) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.conversation_action_unavailable),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
        )
        ConversationReplyBox(
            focusSessionKey = conversation.id,
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
    conversation: NotificationConversation,
    favoriteContact: FavoriteContact?,
    actions: DeviceActions,
    activity: ComponentActivity,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.dp20, vertical = Dimens.dp6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConversationParticipantIcon(
            packages = conversation.sourcePackages,
            favoriteContact = favoriteContact,
            actions = actions,
        )
        Text(
            conversation.notifications.map(ConversationNotification::appName).distinct()
                .joinToString(stringResource(R.string.middle_dot_separator)),
            Modifier.weight(1f).padding(horizontal = Dimens.dp10),
            color = Muted,
            fontSize = Dimens.sp12,
            maxLines = 2,
        )
        OutlinedButton(
            onClick = {
                val target = conversation.openTarget
                if (!ConversationNotificationStore.openConversation(context, target)) {
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
