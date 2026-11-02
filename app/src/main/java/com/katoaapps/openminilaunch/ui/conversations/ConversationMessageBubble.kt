package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.features.conversations.ConversationMessage
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.theme.Dimens
import java.text.DateFormat
import java.util.Date

@Composable
internal fun ConversationBubble(
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
