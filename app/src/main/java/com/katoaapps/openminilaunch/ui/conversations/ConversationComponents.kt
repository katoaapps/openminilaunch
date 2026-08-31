package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.conversations.DemoConversationData
import com.katoaapps.openminilaunch.features.conversations.HubConversation
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.AppIcon
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkWhite
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust
import java.text.DateFormat
import java.util.Date

@Composable
internal fun ConversationCard(
    conversation: HubConversation,
    actions: DeviceActions,
    onOpen: () -> Unit,
) {
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
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(
                            Date(latest?.timestamp ?: conversation.latestNotification.postedAt),
                        ),
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
internal fun ConversationSourceIcons(packages: List<String>, actions: DeviceActions) {
    Box(Modifier.size(if (packages.size > 1) Dimens.dp46 else Dimens.dp38)) {
        packages.take(3).forEachIndexed { index, packageName ->
            Box(
                Modifier.align(
                    when (index) {
                        0 -> Alignment.TopStart
                        1 -> Alignment.Center
                        else -> Alignment.BottomEnd
                    },
                ).background(MaterialTheme.colorScheme.surface, CircleShape).padding(Dimens.dp2),
            ) {
                ConversationSourceIcon(packageName, actions, if (index == 0) Dimens.dp34 else Dimens.dp22)
            }
        }
    }
}

@Composable
internal fun ConversationSourceIcon(packageName: String, actions: DeviceActions, size: Dp) {
    val installedIconAvailable = remember(packageName, actions) { actions.appIcon(packageName) != null }
    if (installedIconAvailable) {
        AppIcon(packageName, actions, size)
        return
    }
    when (packageName) {
        DemoConversationData.SLACK_PACKAGE -> DemoSourceIcon(
            Icons.Default.Forum,
            colorResource(R.color.demo_slack),
            size,
        )
        DemoConversationData.GMAIL_PACKAGE -> DemoSourceIcon(
            Icons.Default.Email,
            colorResource(R.color.demo_gmail),
            size,
        )
        DemoConversationData.GOOGLE_MESSAGES_PACKAGE -> DemoSourceIcon(
            Icons.Default.Forum,
            colorResource(R.color.demo_google_messages),
            size,
        )
        else -> AppIcon(packageName, actions, size)
    }
}

@Composable
private fun DemoSourceIcon(icon: ImageVector, background: Color, size: Dp) {
    Box(
        Modifier.size(size).background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(size * .56f), tint = MinkWhite)
    }
}
