package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.FavoriteAndroidContact
import com.katoaapps.openminilaunch.model.FavoriteContact
import com.katoaapps.openminilaunch.model.FavoriteContactId
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.ui.components.moveItem
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@Composable
internal fun FavoriteContactsList(
    contacts: List<FavoriteContact>,
    integratedAvailability: Map<FavoriteContactId.Integrated, FavoriteIntegratedContactAvailability>,
    integratedCallAvailability: Map<FavoriteContactId.Integrated, FavoriteIntegratedCallAvailability?>,
    onOpen: (FavoriteContact) -> Unit,
    onAddPerson: () -> Unit,
    onReorder: (List<FavoriteContactId>) -> Unit,
) {
    val visibleContacts = remember { mutableStateListOf<FavoriteContact>() }
    var draggedContact by remember { mutableStateOf<FavoriteContactId?>(null) }
    val gridState = rememberLazyGridState()
    val hapticFeedback = LocalHapticFeedback.current
    val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
        if (visibleContacts.moveItem(from.index, to.index)) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    LaunchedEffect(contacts, draggedContact) {
        if (draggedContact == null && visibleContacts.toList() != contacts) {
            visibleContacts.clear()
            visibleContacts.addAll(contacts)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.dp18, vertical = Dimens.dp4),
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
                Icon(Icons.Default.Add, stringResource(R.string.add_person))
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimens.dp18,
                end = Dimens.dp18,
                bottom = Dimens.dp28,
            ),
            horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
        ) {
            items(visibleContacts, key = { it.id.storageKey }) { contact ->
                ReorderableItem(reorderableState, key = contact.id.storageKey) { isDragging ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val scale by animateFloatAsState(
                        targetValue = if (isDragging) 1.04f else 1f,
                        label = "favorite contact drag scale",
                    )
                    val elevation by animateFloatAsState(
                        targetValue = if (isDragging) 12f else 0f,
                        label = "favorite contact drag elevation",
                    )
                    val dragHandle: @Composable () -> Unit = {
                        Icon(
                            Icons.Default.DragHandle,
                            stringResource(R.string.drag_to_reorder),
                            Modifier
                                .size(Dimens.dp28)
                                .draggableHandle(
                                    interactionSource = interactionSource,
                                    onDragStarted = {
                                        draggedContact = contact.id
                                        hapticFeedback.performHapticFeedback(
                                            HapticFeedbackType.LongPress,
                                        )
                                    },
                                    onDragStopped = {
                                        onReorder(visibleContacts.map(FavoriteContact::id))
                                        draggedContact = null
                                    },
                                )
                                .padding(Dimens.dp4),
                            tint = if (isDragging) MaterialTheme.colorScheme.primary else Muted,
                        )
                    }
                    val cardModifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            shadowElevation = elevation
                        }

                    when (contact) {
                        is FavoriteAndroidContact -> FavoriteAndroidContactCard(
                            contact = contact,
                            modifier = cardModifier,
                            dragHandle = dragHandle,
                            onOpen = onOpen,
                        )
                        is FavoriteIntegratedContact -> FavoriteIntegratedContactCard(
                            contact = contact,
                            availability = integratedAvailability[contact.id],
                            callAvailability = integratedCallAvailability[contact.id],
                            modifier = cardModifier,
                            dragHandle = dragHandle,
                            onOpen = onOpen,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteAndroidContactCard(
    contact: FavoriteAndroidContact,
    modifier: Modifier,
    dragHandle: @Composable () -> Unit,
    onOpen: (FavoriteContact) -> Unit,
) {
    FavoriteContactGridCard(
        contact = contact,
        modifier = modifier,
        dragHandle = dragHandle,
        photoUri = contact.photoUri,
        detail = contact.organization
            ?: contact.phones.firstOrNull()?.label
            ?: stringResource(R.string.no_phone_numbers),
        error = false,
        onOpen = onOpen,
    )
}

@Composable
private fun FavoriteIntegratedContactCard(
    contact: FavoriteIntegratedContact,
    availability: FavoriteIntegratedContactAvailability?,
    callAvailability: FavoriteIntegratedCallAvailability?,
    modifier: Modifier,
    dragHandle: @Composable () -> Unit,
    onOpen: (FavoriteContact) -> Unit,
) {
    val hasMessage = contact.endpoints.isNotEmpty()
    val resolvedAvailability = availability
        ?: FavoriteIntegratedContactAvailability.ConversationUnavailable(
            stringResource(R.string.integrated_contact),
        )
    val providerLabel = if (hasMessage) {
        resolvedAvailability.providerLabel
    } else {
        callAvailability?.appLabel ?: stringResource(R.string.use_system_calling_app)
    }
    val unavailable = if (hasMessage) {
        resolvedAvailability !is FavoriteIntegratedContactAvailability.Available
    } else {
        callAvailability?.available == false
    }
    val detail = if (!hasMessage) {
        if (unavailable) {
            stringResource(R.string.calling_app_unavailable, providerLabel)
        } else {
            providerLabel
        }
    } else when (resolvedAvailability) {
        is FavoriteIntegratedContactAvailability.Available -> providerLabel
        is FavoriteIntegratedContactAvailability.AppNotInstalled ->
            stringResource(R.string.provider_not_installed, providerLabel)
        is FavoriteIntegratedContactAvailability.ConversationUnavailable ->
            stringResource(R.string.provider_conversation_unavailable, providerLabel)
    }

    FavoriteContactGridCard(
        contact = contact,
        modifier = modifier,
        dragHandle = dragHandle,
        photoUri = null,
        detail = detail,
        error = unavailable,
        onOpen = onOpen,
    )
}

@Composable
private fun FavoriteContactGridCard(
    contact: FavoriteContact,
    modifier: Modifier,
    dragHandle: @Composable () -> Unit,
    photoUri: String?,
    detail: String,
    error: Boolean,
    onOpen: (FavoriteContact) -> Unit,
) {
    Card(
        modifier = modifier.clickable { onOpen(contact) },
        shape = RoundedCornerShape(Dimens.dp18),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 124.dp).padding(Dimens.dp8),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth()) {
                Box(Modifier.align(Alignment.CenterStart)) { dragHandle() }
                Box(Modifier.align(Alignment.Center)) {
                    FavoriteContactAvatar(contact.displayName, photoUri, size = Dimens.dp40)
                }
            }
            Text(
                contact.displayName,
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp8),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                detail,
                modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.dp30),
                color = if (error) MaterialTheme.colorScheme.error else Muted,
                fontSize = Dimens.sp11,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
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
