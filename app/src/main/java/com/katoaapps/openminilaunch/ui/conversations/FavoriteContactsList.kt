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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.FavoriteAndroidContact
import com.katoaapps.openminilaunch.model.FavoriteContact
import com.katoaapps.openminilaunch.model.FavoriteContactId
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust

@Composable
internal fun FavoriteContactsList(
    contacts: List<FavoriteContact>,
    integratedAvailability: Map<FavoriteContactId.Integrated, FavoriteIntegratedContactAvailability>,
    onOpen: (FavoriteContact) -> Unit,
    onMessage: (FavoriteContact) -> Unit,
    onCall: (FavoriteAndroidContact) -> Unit,
    onInstallProvider: (FavoriteIntegratedContact) -> Unit,
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
                    availability = integratedAvailability[contact.id],
                    onOpen = onOpen,
                    onMessage = onMessage,
                    onInstallProvider = onInstallProvider,
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
    availability: FavoriteIntegratedContactAvailability?,
    onOpen: (FavoriteContact) -> Unit,
    onMessage: (FavoriteContact) -> Unit,
    onInstallProvider: (FavoriteIntegratedContact) -> Unit,
) {
    val resolvedAvailability = availability
        ?: FavoriteIntegratedContactAvailability.ConversationUnavailable(
            stringResource(R.string.integrated_contact),
        )
    val providerLabel = resolvedAvailability.providerLabel
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
                    when (resolvedAvailability) {
                        is FavoriteIntegratedContactAvailability.Available -> providerLabel
                        is FavoriteIntegratedContactAvailability.AppNotInstalled ->
                            stringResource(R.string.provider_not_installed, providerLabel)
                        is FavoriteIntegratedContactAvailability.ConversationUnavailable ->
                            stringResource(
                                R.string.provider_conversation_unavailable,
                                providerLabel,
                            )
                    },
                    color = if (
                        resolvedAvailability is FavoriteIntegratedContactAvailability.Available
                    ) {
                        Muted
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    fontSize = Dimens.sp11,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledTonalIconButton(
                onClick = {
                    when (resolvedAvailability) {
                        is FavoriteIntegratedContactAvailability.Available -> onMessage(contact)
                        is FavoriteIntegratedContactAvailability.AppNotInstalled -> {
                            if (resolvedAvailability.installAvailable) onInstallProvider(contact)
                            else onOpen(contact)
                        }
                        is FavoriteIntegratedContactAvailability.ConversationUnavailable ->
                            onOpen(contact)
                    }
                },
            ) {
                when (resolvedAvailability) {
                    is FavoriteIntegratedContactAvailability.Available -> Icon(
                        Icons.AutoMirrored.Filled.Message,
                        stringResource(R.string.message_contact, contact.displayName),
                        Modifier.size(Dimens.dp18),
                    )
                    is FavoriteIntegratedContactAvailability.AppNotInstalled -> Icon(
                        if (resolvedAvailability.installAvailable) {
                            Icons.Default.Download
                        } else {
                            Icons.Default.Refresh
                        },
                        if (resolvedAvailability.installAvailable) {
                            stringResource(R.string.install_provider, providerLabel)
                        } else {
                            stringResource(R.string.reconnect_conversation)
                        },
                        Modifier.size(Dimens.dp18),
                    )
                    is FavoriteIntegratedContactAvailability.ConversationUnavailable -> Icon(
                        Icons.Default.Refresh,
                        stringResource(R.string.reconnect_conversation),
                        Modifier.size(Dimens.dp18),
                    )
                }
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
            textAlign = TextAlign.Center,
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
