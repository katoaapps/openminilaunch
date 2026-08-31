package com.katoaapps.openminilaunch.ui.conversations

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.conversations.HubConversation
import com.katoaapps.openminilaunch.features.conversations.NotificationHub
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.PageHeader
import com.katoaapps.openminilaunch.ui.settings.NotificationAccessDisclosureDialog
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust
import com.katoaapps.openminilaunch.ui.theme.Sage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
internal fun NotificationHubScreen(store: LauncherStore, actions: DeviceActions, goBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as androidx.activity.ComponentActivity
    var accessGranted by remember { mutableStateOf(NotificationHub.hasAccess(context)) }
    var showAccessDisclosure by remember { mutableStateOf(false) }
    var selectedConversationId by remember { mutableStateOf<String?>(null) }
    val demoModeEnabled = store.demoSearchDataEnabled
    val conversations = NotificationHub.conversations(useDemoData = demoModeEnabled)
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
                !accessGranted && !demoModeEnabled -> ConversationAccessEmptyState { showAccessDisclosure = true }
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
            onOpenAppInfo = actions::openAppSettings,
            onOpenNotificationAccess = {
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
