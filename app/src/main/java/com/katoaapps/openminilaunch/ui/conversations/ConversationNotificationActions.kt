package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.conversations.ConversationNotificationAction
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

/** Source-app notification actions followed by Mink's distinct notification-dismiss control. */
@Composable
internal fun ConversationActionRow(
    conversationActions: List<ConversationNotificationAction>,
    dismissAvailable: Boolean,
    actions: DeviceActions,
    onAction: (ConversationNotificationAction) -> Unit,
    onDismiss: () -> Unit,
) {
    if (conversationActions.isEmpty() && !dismissAvailable) return
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Dimens.dp18, vertical = Dimens.dp4),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(conversationActions, key = ConversationNotificationAction::id) { action ->
            OutlinedButton(
                onClick = { onAction(action) },
                modifier = Modifier
                    .widthIn(max = Dimens.dp200)
                    .semantics {
                        contentDescription = "${action.title}, ${action.appName}"
                    },
                contentPadding = PaddingValues(horizontal = Dimens.dp12, vertical = Dimens.dp7),
            ) {
                ConversationSourceIcon(action.packageName, actions, Dimens.dp17)
                Text(
                    text = action.title,
                    modifier = Modifier.padding(start = Dimens.dp6),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (conversationActions.isNotEmpty() && dismissAvailable) {
            item(key = "dismiss-divider") {
                VerticalDivider(
                    modifier = Modifier.height(Dimens.dp32),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
        if (dismissAvailable) {
            item(key = "dismiss") {
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Muted),
                    contentPadding = PaddingValues(horizontal = Dimens.dp12, vertical = Dimens.dp7),
                ) {
                    Icon(Icons.Default.ClearAll, null, Modifier.size(Dimens.dp17))
                    Text(stringResource(R.string.dismiss), Modifier.padding(start = Dimens.dp6))
                }
            }
        }
    }
}
