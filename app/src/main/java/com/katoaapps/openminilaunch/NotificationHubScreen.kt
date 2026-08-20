package com.katoaapps.openminilaunch

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
internal fun NotificationHubScreen(actions: DeviceActions, goBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as androidx.activity.ComponentActivity
    var accessGranted by remember { mutableStateOf(NotificationHub.hasAccess(context)) }
    var showAccessDisclosure by remember { mutableStateOf(false) }
    var selectedConversationId by remember { mutableStateOf<String?>(null) }
    val conversations = NotificationHub.conversations()
    val selectedConversation = conversations.firstOrNull { it.id == selectedConversationId }

    DisposableEffect(activity, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessGranted = NotificationHub.hasAccess(context)
                if (accessGranted) NotificationHub.requestReconnect(context)
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = selectedConversation != null) { selectedConversationId = null }

    if (selectedConversation != null) {
        ConversationWindow(
            conversation = selectedConversation,
            actions = actions,
            goBack = { selectedConversationId = null },
        )
    } else {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            PageHeader(stringResource(R.string.conversations), goBack)
            when {
                !accessGranted -> ConversationAccessEmptyState { showAccessDisclosure = true }
                conversations.isEmpty() -> NoConversationsEmptyState()
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = Dimens.dp18),
                    verticalArrangement = Arrangement.spacedBy(Dimens.dp10),
                ) {
                    item {
                        Text(
                            stringResource(R.string.active_conversations),
                            Modifier.padding(top = Dimens.dp14, bottom = Dimens.dp2),
                            letterSpacing = Dimens.sp1,
                            fontWeight = FontWeight.Black,
                            fontSize = Dimens.sp12,
                        )
                    }
                    items(conversations, key = HubConversation::id) { conversation ->
                        ConversationCard(conversation, actions) { selectedConversationId = conversation.id }
                    }
                    item { Spacer(Modifier.height(Dimens.dp28)) }
                }
            }
        }
    }

    if (showAccessDisclosure) {
        NotificationAccessDisclosureDialog(
            onContinue = {
                showAccessDisclosure = false
                actions.openNotificationAccessSettings()
            },
            onDismiss = { showAccessDisclosure = false },
        )
    }
}

@Composable
private fun ConversationAccessEmptyState(onEnable: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(Dimens.dp28),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.NotificationsOff, null, Modifier.size(Dimens.dp54), tint = Rust)
        Text(stringResource(R.string.conversation_access_off), fontSize = Dimens.sp22, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = Dimens.dp18))
        Text(
            stringResource(R.string.conversation_access_local_description, stringResource(R.string.app_name)),
            color = Muted,
            modifier = Modifier.padding(vertical = Dimens.dp14),
        )
        Button(onClick = onEnable) { Text(stringResource(R.string.open_notification_access)) }
    }
}

@Composable
private fun NoConversationsEmptyState() {
    Column(
        Modifier.fillMaxSize().padding(Dimens.dp28),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Forum, null, Modifier.size(Dimens.dp54), tint = Sage)
        Text(stringResource(R.string.no_active_conversations), fontSize = Dimens.sp22, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = Dimens.dp18))
        Text(stringResource(R.string.no_active_conversations_description), color = Muted)
    }
}

@Composable
private fun ConversationCard(conversation: HubConversation, actions: DeviceActions, onOpen: () -> Unit) {
    val latest = conversation.latestMessage
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(Dimens.dp18),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.fillMaxWidth().padding(Dimens.dp14), verticalAlignment = Alignment.CenterVertically) {
            ConversationSourceIcons(conversation.sourcePackages, actions)
            Column(Modifier.weight(1f).padding(horizontal = Dimens.dp12)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(conversation.name, Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(latest?.timestamp ?: conversation.latestNotification.postedAt)),
                        color = Muted,
                        fontSize = Dimens.sp11,
                    )
                }
                Text(
                    latest?.text.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f),
                    maxLines = 2,
                    modifier = Modifier.padding(top = Dimens.dp4),
                )
                if (conversation.messages.size > 1) {
                    Text(
                        pluralStringResource(
                            R.plurals.message_count,
                            conversation.messages.size,
                            conversation.messages.size,
                        ),
                        color = Rust,
                        fontSize = Dimens.sp11,
                        modifier = Modifier.padding(top = Dimens.dp4),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationSourceIcons(packages: List<String>, actions: DeviceActions) {
    Box(Modifier.size(if (packages.size > 1) Dimens.dp46 else Dimens.dp38)) {
        packages.take(3).forEachIndexed { index, packageName ->
            Box(
                Modifier.align(
                    when (index) {
                        0 -> Alignment.TopStart
                        1 -> Alignment.Center
                        else -> Alignment.BottomEnd
                    },
                )
                    .background(MaterialTheme.colorScheme.surface, CircleShape).padding(Dimens.dp2),
            ) {
                AppIcon(packageName, actions, if (index == 0) Dimens.dp34 else Dimens.dp22)
            }
        }
    }
}

@Composable
private fun ConversationWindow(conversation: HubConversation, actions: DeviceActions, goBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as androidx.activity.ComponentActivity
    val scope = rememberCoroutineScope()
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
                            Toast.makeText(context, context.getString(R.string.could_not_open_app, target.appName), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        scope.launch {
                            delay(500)
                            if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                                !actions.launchPackage(target.packageName)
                            ) {
                                Toast.makeText(context, context.getString(R.string.could_not_open_app, target.appName), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(Dimens.dp17))
                Text(stringResource(R.string.view_full_conversation), Modifier.padding(start = Dimens.dp6))
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = Dimens.dp18),
            state = messageListState,
            verticalArrangement = Arrangement.spacedBy(Dimens.dp10),
        ) {
            item { Spacer(Modifier.height(Dimens.dp8)) }
            items(conversation.messages, key = ConversationMessage::id) { message ->
                ConversationBubble(message, showSource = conversation.sourcePackages.size > 1, actions = actions)
            }
            item { Spacer(Modifier.height(Dimens.dp8)) }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = Dimens.dp18, vertical = Dimens.dp10)) {
            replyStatus?.let { status ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = Dimens.dp6),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (status == R.string.sent) Icon(Icons.Default.Check, null, Modifier.size(Dimens.dp16), tint = Sage)
                    Text(stringResource(status), color = if (status == R.string.sent) Sage else Rust, fontSize = Dimens.sp12, modifier = Modifier.padding(start = Dimens.dp4))
                }
            }
            if (replyTarget != null) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.reply)) },
                    minLines = 1,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (replyText.isNotBlank()) sendReply() }),
                    trailingIcon = {
                        FilledIconButton(onClick = ::sendReply, enabled = replyText.isNotBlank()) {
                            Icon(Icons.AutoMirrored.Filled.Reply, stringResource(R.string.send_reply))
                        }
                    },
                )
            } else {
                Text(stringResource(R.string.inline_reply_unavailable), color = Muted, fontSize = Dimens.sp12)
            }
        }
    }
}

@Composable
private fun ConversationBubble(message: ConversationMessage, showSource: Boolean, actions: DeviceActions) {
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
                Text(message.text, modifier = Modifier.padding(top = if (message.senderName.isNullOrBlank()) Dimens.dp0 else Dimens.dp2))
                Row(
                    Modifier.fillMaxWidth().padding(top = Dimens.dp5),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showSource) {
                        AppIcon(message.packageName, actions, Dimens.dp14)
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
