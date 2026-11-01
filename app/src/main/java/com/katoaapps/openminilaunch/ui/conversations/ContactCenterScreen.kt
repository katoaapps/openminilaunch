package com.katoaapps.openminilaunch.ui.conversations

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.conversations.ConversationNotificationStore
import com.katoaapps.openminilaunch.features.conversations.NotificationConversation
import com.katoaapps.openminilaunch.model.CommunicationRecipient
import com.katoaapps.openminilaunch.model.ContactAppRoute
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.PageHeader
import com.katoaapps.openminilaunch.ui.settings.NotificationAccessDisclosureDialog
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust
import com.katoaapps.openminilaunch.ui.theme.Sage

/** Contact-focused surface containing notification conversations and Android favorites. */
@Composable
internal fun ContactCenterScreen(
    store: LauncherStore,
    actions: DeviceActions,
    onComposeMessage: (CommunicationRecipient, ContactAppRoute) -> Unit,
    onComposeConversationMessage: (LauncherShortcutTarget) -> Unit,
    goBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    var accessGranted by remember {
        mutableStateOf(ConversationNotificationStore.hasNotificationAccess(context))
    }
    var showAccessDisclosure by remember { mutableStateOf(false) }
    var selectedConversationId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSection by rememberSaveable { mutableStateOf(ContactSection.CONVERSATIONS) }
    val demoModeEnabled = store.demoSearchDataEnabled
    val conversations = ConversationNotificationStore.currentConversations(
        useDemoData = demoModeEnabled,
    )
    val selectedConversation = conversations.firstOrNull { it.id == selectedConversationId }

    DisposableEffect(activity, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessGranted = ConversationNotificationStore.hasNotificationAccess(context)
                if (accessGranted) ConversationNotificationStore.requestListenerReconnect(context)
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = selectedConversation != null) { selectedConversationId = null }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.widthIn(max = Dimens.dp720).fillMaxWidth().fillMaxHeight()) {
            if (selectedConversation != null) {
                ConversationWindow(
                    conversation = selectedConversation,
                    actions = actions,
                    goBack = { selectedConversationId = null },
                )
            } else {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    PageHeader(stringResource(R.string.contacts), goBack)
                    ContactSectionTabs(selectedSection) { selectedSection = it }
                    when (selectedSection) {
                        ContactSection.FAVORITES -> FavoriteContactsPane(
                            actions = actions,
                            onComposeMessage = onComposeMessage,
                            onComposeConversationMessage = onComposeConversationMessage,
                        )
                        ContactSection.CONVERSATIONS -> when {
                            !accessGranted && !demoModeEnabled -> {
                                ConversationAccessEmptyState { showAccessDisclosure = true }
                            }
                            conversations.isEmpty() -> NoConversationsEmptyState()
                            else -> ConversationList(
                                conversations = conversations,
                                actions = actions,
                                onConversation = { selectedConversationId = it.id },
                            )
                        }
                    }
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
private fun ConversationList(
    conversations: List<NotificationConversation>,
    actions: DeviceActions,
    onConversation: (NotificationConversation) -> Unit,
) {
    LazyColumn(
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
        items(conversations, key = NotificationConversation::id) { conversation ->
            ConversationCard(conversation, actions) { onConversation(conversation) }
        }
        item { Spacer(Modifier.height(Dimens.dp28)) }
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
        Text(
            stringResource(R.string.conversation_access_off),
            modifier = Modifier.padding(top = Dimens.dp18),
            fontSize = Dimens.sp22,
            fontWeight = FontWeight.Black,
        )
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
        Text(
            stringResource(R.string.no_active_conversations),
            modifier = Modifier.padding(top = Dimens.dp18),
            fontSize = Dimens.sp22,
            fontWeight = FontWeight.Black,
        )
        Text(stringResource(R.string.no_active_conversations_description), color = Muted)
    }
}
