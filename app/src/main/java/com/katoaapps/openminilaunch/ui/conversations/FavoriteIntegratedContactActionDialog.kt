package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun FavoriteIntegratedContactActionDialog(
    contact: FavoriteIntegratedContact,
    availability: FavoriteIntegratedContactAvailability,
    onMessage: () -> Unit,
    onInstallProvider: () -> Unit,
    onReconnect: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 620.dp),
            shape = RoundedCornerShape(Dimens.dp24),
            tonalElevation = Dimens.dp6,
        ) {
            Column(Modifier.padding(Dimens.dp18)) {
                FavoriteContactAvatar(contact.displayName, null)
                Text(
                    contact.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = Dimens.dp10),
                )
                Text(
                    availability.providerLabel,
                    color = Muted,
                    modifier = Modifier.padding(top = Dimens.dp4),
                )
                UnavailableReason(availability)
                AvailabilityAction(
                    availability = availability,
                    onMessage = onMessage,
                    onInstallProvider = onInstallProvider,
                    onReconnect = onReconnect,
                )
                TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DeleteOutline, null, Modifier.size(Dimens.dp18))
                    Text(
                        stringResource(R.string.remove_from_favorites),
                        Modifier.padding(start = Dimens.dp6),
                    )
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
private fun UnavailableReason(availability: FavoriteIntegratedContactAvailability) {
    if (availability is FavoriteIntegratedContactAvailability.Available) return
    val message = when (availability) {
        is FavoriteIntegratedContactAvailability.AppNotInstalled -> stringResource(
            R.string.provider_not_installed,
            availability.providerLabel,
        )
        is FavoriteIntegratedContactAvailability.ConversationUnavailable -> stringResource(
            R.string.provider_conversation_unavailable,
            availability.providerLabel,
        )
        is FavoriteIntegratedContactAvailability.Available -> return
    }
    Text(
        message,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = Dimens.dp4),
    )
}

@Composable
private fun AvailabilityAction(
    availability: FavoriteIntegratedContactAvailability,
    onMessage: () -> Unit,
    onInstallProvider: () -> Unit,
    onReconnect: () -> Unit,
) {
    when (availability) {
        is FavoriteIntegratedContactAvailability.Available -> PrimaryActionButton(
            label = stringResource(R.string.message),
            icon = { Icon(Icons.AutoMirrored.Filled.Message, null, Modifier.size(Dimens.dp18)) },
            onClick = onMessage,
        )
        is FavoriteIntegratedContactAvailability.AppNotInstalled -> {
            if (availability.installAvailable) {
                PrimaryActionButton(
                    label = stringResource(R.string.install_provider, availability.providerLabel),
                    icon = { Icon(Icons.Default.Download, null, Modifier.size(Dimens.dp18)) },
                    onClick = onInstallProvider,
                )
                TextButton(onClick = onReconnect, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(Dimens.dp18))
                    Text(
                        stringResource(R.string.reconnect_conversation),
                        Modifier.padding(start = Dimens.dp6),
                    )
                }
            } else {
                ReconnectConversationButton(onReconnect)
            }
        }
        is FavoriteIntegratedContactAvailability.ConversationUnavailable ->
            ReconnectConversationButton(onReconnect)
    }
}

@Composable
private fun ReconnectConversationButton(onReconnect: () -> Unit) {
    PrimaryActionButton(
        label = stringResource(R.string.reconnect_conversation),
        icon = { Icon(Icons.Default.Refresh, null, Modifier.size(Dimens.dp18)) },
        onClick = onReconnect,
    )
}

@Composable
private fun PrimaryActionButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp18),
    ) {
        icon()
        Text(label, Modifier.padding(start = Dimens.dp6))
    }
}
