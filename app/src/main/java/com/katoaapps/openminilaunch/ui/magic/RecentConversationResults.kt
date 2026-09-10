package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.apps.launcherDiscoveryLabel
import com.katoaapps.openminilaunch.features.messaging.filteredRecentConversations
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.DrawableIcon
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun rememberRecentConversationResults(
    prefix: Char?,
    query: String,
    hasSelectedRecipient: Boolean,
    launcherShortcutsRevision: Long,
    sendAutomatically: Boolean,
    preferredMessagingPackage: String?,
    demoModeEnabled: Boolean,
    actions: DeviceActions,
): List<LauncherShortcutTarget> {
    val defaultMessagingPackage = actions.defaultMessagingPackage()
    return remember(
        prefix,
        query,
        hasSelectedRecipient,
        launcherShortcutsRevision,
        sendAutomatically,
        preferredMessagingPackage,
        defaultMessagingPackage,
        demoModeEnabled,
    ) {
        if (prefix != '@' || hasSelectedRecipient || demoModeEnabled) {
            return@remember emptyList()
        }
        filteredRecentConversations(
            shortcuts = actions.recentConversationShortcuts(
                sendAutomatically = sendAutomatically,
                preferredMessagingPackage = preferredMessagingPackage,
            ),
            query = query,
            appLabel = actions::appLabel,
            prioritizedPackage = preferredMessagingPackage ?: defaultMessagingPackage,
        )
    }
}

@Composable
internal fun MagicResultSectionTitle(text: String) {
    Text(
        text = text,
        color = Muted,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = Dimens.dp4, top = Dimens.dp4),
    )
}

@Composable
internal fun RecentConversationResults(
    conversations: List<LauncherShortcutTarget>,
    actions: DeviceActions,
    appsRevision: Long,
    shortcutsRevision: Long,
    onOpen: (LauncherShortcutTarget) -> Unit,
) {
    if (conversations.isEmpty()) return

    MagicResultSectionTitle(stringResource(R.string.recent_conversations))
    conversations.forEach { conversation ->
        SuggestionRow(
            text = launcherDiscoveryLabel(conversation, actions::appLabel),
            leadingContent = {
                RecentConversationIcon(
                    conversation = conversation,
                    actions = actions,
                    shortcutsRevision = shortcutsRevision,
                    appsRevision = appsRevision,
                )
            },
        ) { onOpen(conversation) }
    }
}

@Composable
private fun RecentConversationIcon(
    conversation: LauncherShortcutTarget,
    actions: DeviceActions,
    shortcutsRevision: Long,
    appsRevision: Long,
) {
    Box(Modifier.size(Dimens.dp34)) {
        DrawableIcon(
            drawable = actions.launcherTargetIcon(conversation)
                ?: actions.appIcon(conversation.packageName),
            iconKey = "conversation:${conversation.selectionKey}:$shortcutsRevision",
            size = Dimens.dp30,
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).size(Dimens.dp15),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Box(contentAlignment = Alignment.Center) {
                DrawableIcon(
                    drawable = actions.appIcon(conversation.packageName),
                    iconKey = "conversation-app:${conversation.packageName}:$appsRevision",
                    size = Dimens.dp11,
                )
            }
        }
    }
}
