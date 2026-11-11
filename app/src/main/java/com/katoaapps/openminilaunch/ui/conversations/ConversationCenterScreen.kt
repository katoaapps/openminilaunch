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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.conversations.ConversationNotificationStore
import com.katoaapps.openminilaunch.features.conversations.NotificationConversation
import com.katoaapps.openminilaunch.features.conversations.conversationActivityAt
import com.katoaapps.openminilaunch.features.conversations.matchFavoriteConversations
import com.katoaapps.openminilaunch.features.conversations.prioritizeFavoriteConversations
import com.katoaapps.openminilaunch.features.conversations.shouldShowConversationAttention
import com.katoaapps.openminilaunch.model.CommunicationRecipient
import com.katoaapps.openminilaunch.model.ContactAppRoute
import com.katoaapps.openminilaunch.model.FavoriteContact
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.PageHeader
import com.katoaapps.openminilaunch.ui.settings.NotificationAccessDisclosureDialog
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.MinkFeatureColors
import com.katoaapps.openminilaunch.ui.theme.Rust
import com.katoaapps.openminilaunch.ui.theme.Sage

private const val CONVERSATION_FEATURE_TOUR_ID = "conversation_feature_tour_v1"

/** Conversations surface containing active messages and favorite people. */
@Composable
internal fun ConversationCenterScreen(
    store: LauncherStore,
    actions: DeviceActions,
    onComposeMessage: (CommunicationRecipient, ContactAppRoute) -> Unit,
    onComposeConversationMessage: (LauncherShortcutTarget) -> Unit,
    goBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val shortcutsRevision by actions.launcherShortcutsRevision.collectAsState()
    val demoModeEnabled = store.demoModeEnabled
    var accessGranted by remember {
        mutableStateOf(ConversationNotificationStore.hasNotificationAccess(context))
    }
    var showAccessDisclosure by remember { mutableStateOf(false) }
    var showFeatureTour by rememberSaveable(demoModeEnabled) {
        mutableStateOf(!demoModeEnabled && !store.hasSeenUpdate(CONVERSATION_FEATURE_TOUR_ID))
    }
    var selectedConversationId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSection by rememberSaveable { mutableStateOf(ConversationSection.MESSAGES) }
    var acknowledgementRevision by remember { mutableIntStateOf(0) }
    val conversations = ConversationNotificationStore.currentConversations(
        useDemoData = demoModeEnabled,
        demoScenario = store.demoScenario,
        demoConversationState = store.demoConversationState,
        demoSessionStartedAt = store.demoSessionStartedAt,
    )
    val favoriteContacts = rememberOrderedConversationFavorites(
        store = store,
        refreshKey = selectedSection to shortcutsRevision,
        actions = actions,
    )
    val orderedConversations = remember(conversations, favoriteContacts) {
        prioritizeFavoriteConversations(conversations, favoriteContacts)
    }
    val favoriteMatches = remember(conversations, favoriteContacts) {
        matchFavoriteConversations(conversations, favoriteContacts)
    }
    val favoriteContactsByConversation = remember(favoriteMatches, favoriteContacts) {
        val favoriteContactsById = favoriteContacts.associateBy(FavoriteContact::id)
        favoriteMatches.mapNotNull { (conversationId, match) ->
            favoriteContactsById[match.favoriteId]?.let { conversationId to it }
        }.toMap()
    }
    val conversationsById = remember(conversations) {
        conversations.associateBy(NotificationConversation::id)
    }
    val acknowledgedActivityByConversationId = remember(conversations, acknowledgementRevision) {
        conversations.associate { conversation ->
            conversation.id to if (demoModeEnabled) {
                0L
            } else {
                store.acknowledgedConversationActivityAt(conversation.id)
            }
        }
    }
    val favoriteRowStyles = favoriteMatches.mapValues { (conversationId, match) ->
        ConversationFavoriteRowStyle(
            color = (if (demoModeEnabled) {
                store.demoFavoriteAttentionColor(match.favoriteId)
            } else {
                store.favoriteAttentionColorStore.color(match.favoriteId)
            })?.let(::Color)
                ?: MinkFeatureColors.ConversationAttention,
            isUnread = shouldShowConversationAttention(
                latestActivityAt = conversationsById[conversationId]
                    ?.let(::conversationActivityAt),
                acknowledgedThrough = acknowledgedActivityByConversationId[conversationId] ?: 0L,
            ),
        )
    }
    val selectedConversation = conversations.firstOrNull { it.id == selectedConversationId }
    val selectedActivityAt = selectedConversation?.let(::conversationActivityAt)

    LaunchedEffect(context, accessGranted, demoModeEnabled) {
        if (accessGranted && !demoModeEnabled) {
            ConversationNotificationStore.refreshActiveNotifications(context)
        }
    }

    LaunchedEffect(selectedConversation?.id, selectedActivityAt, demoModeEnabled) {
        if (!demoModeEnabled && selectedConversation != null) {
            store.acknowledgeConversationActivity(
                conversationId = selectedConversation.id,
                activityAt = selectedActivityAt,
            )
            acknowledgementRevision++
        }
    }

    DisposableEffect(activity, context, demoModeEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessGranted = ConversationNotificationStore.hasNotificationAccess(context)
                if (accessGranted && !demoModeEnabled) {
                    ConversationNotificationStore.refreshActiveNotifications(context)
                }
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
                    displayName = favoriteContactsByConversation[selectedConversation.id]
                        ?.displayName
                        ?: selectedConversation.name,
                    favoriteContact = favoriteContactsByConversation[selectedConversation.id],
                    actions = actions,
                    goBack = { selectedConversationId = null },
                )
            } else {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    PageHeader(stringResource(R.string.conversations), goBack)
                    ConversationSectionTabs(selectedSection) { selectedSection = it }
                    when (selectedSection) {
                        ConversationSection.FAVORITES -> FavoriteContactsPane(
                            store = store,
                            actions = actions,
                            onComposeMessage = onComposeMessage,
                            onComposeConversationMessage = onComposeConversationMessage,
                        )
                        ConversationSection.MESSAGES -> when {
                            !accessGranted && !demoModeEnabled -> {
                                ConversationAccessEmptyState { showAccessDisclosure = true }
                            }
                            conversations.isEmpty() -> NoConversationsEmptyState()
                            else -> ConversationList(
                                conversations = orderedConversations,
                                favoriteRowStyles = favoriteRowStyles,
                                favoriteContacts = favoriteContactsByConversation,
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
            onOpenAppInfo = actions::openAppInfo,
            onOpenNotificationAccess = {
                showAccessDisclosure = false
                actions.openNotificationAccessSettings()
            },
            onDismiss = { showAccessDisclosure = false },
        )
    }
    if (showFeatureTour) {
        ConversationFeatureTour(
            onDismiss = {
                store.markUpdateSeen(CONVERSATION_FEATURE_TOUR_ID)
                showFeatureTour = false
            },
        )
    }
}

@Composable
private fun ConversationList(
    conversations: List<NotificationConversation>,
    favoriteRowStyles: Map<String, ConversationFavoriteRowStyle>,
    favoriteContacts: Map<String, FavoriteContact>,
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
            ConversationCard(
                conversation = conversation,
                favoriteContact = favoriteContacts[conversation.id],
                actions = actions,
                favoriteStyle = favoriteRowStyles[conversation.id],
                onOpen = { onConversation(conversation) },
            )
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
            textAlign = TextAlign.Center,
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
        Text(
            stringResource(R.string.no_active_conversations_description),
            color = Muted,
            textAlign = TextAlign.Center,
        )
    }
}
