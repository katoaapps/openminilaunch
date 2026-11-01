package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.FavoriteAndroidContact
import com.katoaapps.openminilaunch.model.FavoriteContact
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust

@Composable
internal fun FavoriteContactsList(
    contacts: List<FavoriteContact>,
    actions: DeviceActions,
    onOpen: (FavoriteContact) -> Unit,
    onMessage: (FavoriteContact) -> Unit,
    onCall: (FavoriteAndroidContact) -> Unit,
    onAddPerson: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = Dimens.dp18),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp10),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = Dimens.dp4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.starred_contacts),
                    Modifier.weight(1f),
                    letterSpacing = Dimens.sp1,
                    fontWeight = FontWeight.Black,
                    fontSize = Dimens.sp12,
                )
                IconButton(onClick = onAddPerson) {
                    Icon(
                        Icons.Default.Add,
                        stringResource(R.string.add_person),
                    )
                }
            }
        }
        items(contacts, key = { it.id.storageKey }) { contact ->
            when (contact) {
                is FavoriteAndroidContact -> FavoriteAndroidContactCard(
                    contact = contact,
                    onOpen = onOpen,
                    onMessage = onMessage,
                    onCall = onCall,
                )
                is FavoriteIntegratedContact -> FavoriteIntegratedContactCard(
                    contact = contact,
                    actions = actions,
                    onOpen = onOpen,
                    onMessage = onMessage,
                )
            }
        }
        item { Spacer(Modifier.height(Dimens.dp28)) }
    }
}

@Composable
private fun FavoriteAndroidContactCard(
    contact: FavoriteAndroidContact,
    onOpen: (FavoriteContact) -> Unit,
    onMessage: (FavoriteContact) -> Unit,
    onCall: (FavoriteAndroidContact) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(contact) },
        shape = RoundedCornerShape(Dimens.dp18),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(Dimens.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FavoriteContactAvatar(contact.displayName, contact.photoUri)
            Column(Modifier.weight(1f).padding(horizontal = Dimens.dp12)) {
                Text(
                    contact.displayName,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    contact.organization
                        ?: contact.phones.firstOrNull()?.label
                        ?: stringResource(R.string.no_phone_numbers),
                    color = Muted,
                    fontSize = Dimens.sp11,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledTonalIconButton(
                onClick = { onMessage(contact) },
                enabled = contact.phones.isNotEmpty(),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Message,
                    stringResource(R.string.message_contact, contact.displayName),
                    Modifier.size(Dimens.dp18),
                )
            }
            FilledTonalIconButton(
                onClick = { onCall(contact) },
                enabled = contact.phones.isNotEmpty(),
                modifier = Modifier.padding(start = Dimens.dp4),
            ) {
                Icon(
                    Icons.Default.Phone,
                    stringResource(R.string.call_contact, contact.displayName),
                    Modifier.size(Dimens.dp18),
                )
            }
        }
    }
}

@Composable
private fun FavoriteIntegratedContactCard(
    contact: FavoriteIntegratedContact,
    actions: DeviceActions,
    onOpen: (FavoriteContact) -> Unit,
    onMessage: (FavoriteContact) -> Unit,
) {
    val providerLabel = contact.endpoints.firstOrNull()?.packageName?.let(actions::appLabel)
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(contact) },
        shape = RoundedCornerShape(Dimens.dp18),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(Dimens.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FavoriteContactAvatar(contact.displayName, null)
            Column(Modifier.weight(1f).padding(horizontal = Dimens.dp12)) {
                Text(
                    contact.displayName,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    providerLabel ?: stringResource(R.string.integrated_contact),
                    color = Muted,
                    fontSize = Dimens.sp11,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledTonalIconButton(onClick = { onMessage(contact) }) {
                Icon(
                    Icons.AutoMirrored.Filled.Message,
                    stringResource(R.string.message_contact, contact.displayName),
                    Modifier.size(Dimens.dp18),
                )
            }
        }
    }
}

@Composable
internal fun FavoriteContactsEmptyState(
    openContacts: () -> Unit,
    onAddPerson: () -> Unit,
    contactsPermissionGranted: Boolean,
    onRequestContacts: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.dp28),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.ContactPage, null, Modifier.size(Dimens.dp54), tint = Rust)
        Text(
            stringResource(R.string.no_favorite_contacts),
            modifier = Modifier.padding(top = Dimens.dp18),
            fontSize = Dimens.sp22,
            fontWeight = FontWeight.Black,
        )
        Text(
            stringResource(R.string.no_favorite_contacts_description),
            modifier = Modifier.padding(vertical = Dimens.dp14),
            color = Muted,
        )
        Button(onClick = onAddPerson) { Text(stringResource(R.string.add_person)) }
        TextButton(
            onClick = if (contactsPermissionGranted) openContacts else onRequestContacts,
        ) {
            Text(
                stringResource(
                    if (contactsPermissionGranted) {
                        R.string.open_contacts
                    } else {
                        R.string.allow_contact_access
                    },
                ),
            )
        }
    }
}

@Composable
internal fun FavoriteContactsLoadingState() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            stringResource(R.string.loading_favorite_contacts),
            modifier = Modifier.padding(top = Dimens.dp12),
            color = Muted,
        )
    }
}
