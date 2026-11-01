package com.katoaapps.openminilaunch.ui.conversations

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.contacts.FavoriteContactsRepository
import com.katoaapps.openminilaunch.features.contacts.PersonActionRouteStore
import com.katoaapps.openminilaunch.model.CommunicationRecipient
import com.katoaapps.openminilaunch.model.FavoriteContact
import com.katoaapps.openminilaunch.model.FavoritePhone
import com.katoaapps.openminilaunch.model.FavoritePhoneSelection
import com.katoaapps.openminilaunch.model.PersonAppRoute
import com.katoaapps.openminilaunch.model.androidContactPersonKey
import com.katoaapps.openminilaunch.model.favoritePhoneFingerprint
import com.katoaapps.openminilaunch.model.phoneSelection
import com.katoaapps.openminilaunch.model.preferredPhone
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.platform.isPermanentlyDenied
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun FavoriteContactsPane(
    actions: DeviceActions,
    onComposeMessage: (CommunicationRecipient, PersonAppRoute) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionRequested = true
        permissionGranted = granted
    }

    DisposableEffect(activity, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CONTACTS,
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    if (!permissionGranted) {
        FavoriteContactsPermissionState(
            openSettings = permissionRequested &&
                isPermanentlyDenied(context, Manifest.permission.READ_CONTACTS),
            onAction = {
                if (
                    permissionRequested &&
                    isPermanentlyDenied(context, Manifest.permission.READ_CONTACTS)
                ) {
                    actions.openAppPermissionSettings()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            },
        )
        return
    }

    GrantedFavoriteContactsPane(actions, onComposeMessage)
}

@Composable
private fun GrantedFavoriteContactsPane(
    actions: DeviceActions,
    onComposeMessage: (CommunicationRecipient, PersonAppRoute) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val repository = remember(context) { FavoriteContactsRepository(context.applicationContext) }
    val routeStore = remember(context) { PersonActionRouteStore(context.applicationContext) }
    val revision by repository.revision.collectAsState()
    var resumeRevision by remember { mutableLongStateOf(0L) }
    var selectedContact by remember { mutableStateOf<FavoriteContact?>(null) }
    var pendingPhoneChoice by remember { mutableStateOf<PendingPhoneChoice?>(null) }
    var routeRevision by remember { mutableIntStateOf(0) }
    val actionAppOptions by produceState(
        initialValue = FavoriteActionAppOptions(),
        key1 = selectedContact?.lookupKey,
    ) {
        value = if (selectedContact == null) {
            FavoriteActionAppOptions()
        } else {
            withContext(Dispatchers.IO) {
                FavoriteActionAppOptions(
                    messaging = actions.messagingProviderOptions().filter { it.installed },
                    calling = actions.callingAppOptions(),
                )
            }
        }
    }
    val contacts by produceState<List<FavoriteContact>?>(
        initialValue = null,
        key1 = repository,
        key2 = revision,
        key3 = resumeRevision,
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching(repository::favorites).getOrDefault(emptyList())
        }
    }

    DisposableEffect(repository, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeRevision++
        }
        activity.lifecycle.addObserver(observer)
        onDispose {
            activity.lifecycle.removeObserver(observer)
            repository.close()
        }
    }

    fun perform(contact: FavoriteContact, phone: FavoritePhone, action: FavoriteContactAction) {
        val personKey = androidContactPersonKey(contact.lookupKey)
        val routes = routeStore.routes(personKey)
        when (action) {
            FavoriteContactAction.MESSAGE -> {
                onComposeMessage(
                    CommunicationRecipient(
                        address = phone.rawNumber,
                        displayName = contact.displayName,
                        detail = phone.label,
                    ),
                    routes.message,
                )
            }
            FavoriteContactAction.CALL -> if (!actions.dial(
                    phone.rawNumber,
                    (routes.call as? PersonAppRoute.Package)?.packageName,
                )
            ) {
                Toast.makeText(
                    context,
                    context.getString(R.string.could_not_open_calling_app),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
        selectedContact = null
        pendingPhoneChoice = null
    }

    fun requestAction(contact: FavoriteContact, action: FavoriteContactAction) {
        val personKey = androidContactPersonKey(contact.lookupKey)
        val routes = routeStore.routes(personKey)
        val preferredPhone = contact.preferredPhone(
            personKey,
            routes.preferredPhoneFingerprint,
        )
        when (val selection = contact.phoneSelection(preferredPhone)) {
            FavoritePhoneSelection.None -> selectedContact = contact
            is FavoritePhoneSelection.Resolved -> perform(contact, selection.phone, action)
            is FavoritePhoneSelection.Choose -> {
                selectedContact = null
                pendingPhoneChoice = PendingPhoneChoice(contact, selection.phones, action)
            }
        }
    }

    when {
        contacts == null -> FavoriteContactsLoadingState()
        contacts.orEmpty().isEmpty() -> FavoriteContactsEmptyState { actions.openContactsApp() }
        else -> FavoriteContactsList(
            contacts = contacts.orEmpty(),
            onOpen = { selectedContact = it },
            onMessage = { requestAction(it, FavoriteContactAction.MESSAGE) },
            onCall = { requestAction(it, FavoriteContactAction.CALL) },
            openContacts = actions::openContactsApp,
        )
    }

    selectedContact?.let { contact ->
        val personKey = androidContactPersonKey(contact.lookupKey)
        val contactRoutes = remember(personKey, routeRevision) {
            routeStore.routes(personKey)
        }
        val preferredPhone = remember(contact.phones, personKey, contactRoutes) {
            contact.preferredPhone(personKey, contactRoutes.preferredPhoneFingerprint)
        }
        FavoriteContactActionDialog(
            contact = contact,
            routes = contactRoutes,
            messagingOptions = actionAppOptions.messaging,
            callingOptions = actionAppOptions.calling,
            preferredPhone = preferredPhone,
            onAction = { requestAction(contact, it) },
            onPreferredPhone = { phone ->
                routeStore.updatePreferredPhone(
                    personKey,
                    phone?.let { favoritePhoneFingerprint(personKey, it.normalizedNumber) },
                )
                routeRevision++
            },
            onMessageRoute = { route ->
                routeStore.updateMessage(personKey, route)
                routeRevision++
            },
            onCallRoute = { route ->
                routeStore.updateCall(personKey, route)
                routeRevision++
            },
            onOpenContact = {
                if (!actions.openContact(contact.contactUri)) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.could_not_open_contact),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                selectedContact = null
            },
            onDismiss = { selectedContact = null },
        )
    }
    pendingPhoneChoice?.let { pending ->
        FavoritePhonePickerDialog(
            contact = pending.contact,
            phones = pending.phones,
            action = pending.action,
            onPhone = { perform(pending.contact, it, pending.action) },
            onDismiss = { pendingPhoneChoice = null },
        )
    }
}

@Composable
private fun FavoriteContactsList(
    contacts: List<FavoriteContact>,
    onOpen: (FavoriteContact) -> Unit,
    onMessage: (FavoriteContact) -> Unit,
    onCall: (FavoriteContact) -> Unit,
    openContacts: () -> Unit,
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
                TextButton(onClick = openContacts) {
                    Text(stringResource(R.string.manage_favorites))
                }
            }
        }
        items(contacts, key = FavoriteContact::lookupKey) { contact ->
            FavoriteContactCard(contact, onOpen, onMessage, onCall)
        }
        item { Spacer(Modifier.height(Dimens.dp28)) }
    }
}

@Composable
private fun FavoriteContactCard(
    contact: FavoriteContact,
    onOpen: (FavoriteContact) -> Unit,
    onMessage: (FavoriteContact) -> Unit,
    onCall: (FavoriteContact) -> Unit,
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
private fun FavoriteContactsPermissionState(openSettings: Boolean, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.dp28),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Contacts, null, Modifier.size(Dimens.dp54), tint = Rust)
        Text(
            stringResource(R.string.show_favorite_contacts),
            modifier = Modifier.padding(top = Dimens.dp18),
            fontSize = Dimens.sp22,
            fontWeight = FontWeight.Black,
        )
        Text(
            stringResource(R.string.favorite_contacts_permission_description),
            modifier = Modifier.padding(vertical = Dimens.dp14),
            color = Muted,
        )
        Button(onClick = onAction) {
            Text(
                stringResource(
                    if (openSettings) {
                        R.string.open_app_permissions
                    } else {
                        R.string.allow_contact_access
                    },
                ),
            )
        }
    }
}

@Composable
private fun FavoriteContactsEmptyState(openContacts: () -> Unit) {
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
        Button(onClick = openContacts) { Text(stringResource(R.string.open_contacts)) }
    }
}

@Composable
private fun FavoriteContactsLoadingState() {
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

private data class PendingPhoneChoice(
    val contact: FavoriteContact,
    val phones: List<FavoritePhone>,
    val action: FavoriteContactAction,
)

private data class FavoriteActionAppOptions(
    val messaging: List<com.katoaapps.openminilaunch.features.messaging.MessagingProviderOption> =
        emptyList(),
    val calling: List<com.katoaapps.openminilaunch.features.contacts.CallingAppOption> = emptyList(),
)
