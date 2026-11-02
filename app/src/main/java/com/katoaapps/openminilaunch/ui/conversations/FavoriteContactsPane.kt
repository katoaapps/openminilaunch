package com.katoaapps.openminilaunch.ui.conversations

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.contacts.FavoriteAndroidContactsRepository
import com.katoaapps.openminilaunch.features.contacts.FavoriteContactCatalog
import com.katoaapps.openminilaunch.features.contacts.FavoriteContactRouteStore
import com.katoaapps.openminilaunch.features.contacts.FavoriteIntegratedContactAddResult
import com.katoaapps.openminilaunch.features.contacts.FavoriteIntegratedContactStore
import com.katoaapps.openminilaunch.model.CommunicationRecipient
import com.katoaapps.openminilaunch.model.AndroidContactPhone
import com.katoaapps.openminilaunch.model.AndroidPhoneSelection
import com.katoaapps.openminilaunch.model.ContactAppRoute
import com.katoaapps.openminilaunch.model.FavoriteAndroidContact
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.model.IntegratedContactEndpoint
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.model.favoritePhoneFingerprint
import com.katoaapps.openminilaunch.model.launcherShortcutSelectionKey
import com.katoaapps.openminilaunch.model.phoneSelection
import com.katoaapps.openminilaunch.model.preferredPhone
import com.katoaapps.openminilaunch.model.resolvePackage
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.platform.isPermanentlyDenied
import com.katoaapps.openminilaunch.platform.supportsDirectCalls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun FavoriteContactsPane(
    store: LauncherStore,
    actions: DeviceActions,
    onComposeMessage: (CommunicationRecipient, ContactAppRoute) -> Unit,
    onComposeConversationMessage: (LauncherShortcutTarget) -> Unit,
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

    val requestContacts = {
        if (
            permissionRequested &&
            isPermanentlyDenied(context, Manifest.permission.READ_CONTACTS)
        ) {
            actions.openAppPermissionSettings()
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
        Unit
    }
    GrantedFavoriteContactsPane(
        store = store,
        actions = actions,
        onComposeMessage = onComposeMessage,
        onComposeConversationMessage = onComposeConversationMessage,
        contactsPermissionGranted = permissionGranted,
        onRequestContacts = requestContacts,
    )
}

@Composable
private fun GrantedFavoriteContactsPane(
    store: LauncherStore,
    actions: DeviceActions,
    onComposeMessage: (CommunicationRecipient, ContactAppRoute) -> Unit,
    onComposeConversationMessage: (LauncherShortcutTarget) -> Unit,
    contactsPermissionGranted: Boolean,
    onRequestContacts: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val repository = remember(context) {
        FavoriteAndroidContactsRepository(context.applicationContext)
    }
    val routeStore = remember(context) { FavoriteContactRouteStore(context.applicationContext) }
    val integratedStore = remember(context) {
        FavoriteIntegratedContactStore(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    val revision by repository.revision.collectAsState()
    val shortcutsRevision by actions.launcherShortcutsRevision.collectAsState()
    var resumeRevision by remember { mutableLongStateOf(0L) }
    var selectedContact by remember { mutableStateOf<FavoriteAndroidContact?>(null) }
    var pendingPhoneChoice by remember { mutableStateOf<PendingPhoneChoice?>(null) }
    var routeRevision by remember { mutableIntStateOf(0) }
    var integratedRevision by remember { mutableIntStateOf(0) }
    var selectedIntegratedContact by remember { mutableStateOf<FavoriteIntegratedContact?>(null) }
    var reconnectingIntegratedContact by remember {
        mutableStateOf<FavoriteIntegratedContact?>(null)
    }
    var showAddPerson by remember { mutableStateOf(false) }
    var pendingAutomaticCall by remember {
        mutableStateOf<Pair<String, String?>?>(null)
    }
    fun openCall(phone: String, packageName: String?, automatically: Boolean) {
        if (!actions.openCall(phone, packageName, automatically)) {
            Toast.makeText(
                context,
                context.getString(R.string.could_not_open_calling_app),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val pending = pendingAutomaticCall
        pendingAutomaticCall = null
        if (pending != null) {
            if (granted) {
                openCall(pending.first, pending.second, automatically = true)
            } else {
                openCall(pending.first, pending.second, automatically = false)
            }
        }
    }
    val integratedContacts by produceState<List<FavoriteIntegratedContact>?>(
        initialValue = null,
        key1 = integratedStore,
        key2 = integratedRevision,
    ) {
        value = withContext(Dispatchers.IO) { integratedStore.contacts() }
    }
    val actionAppOptions by produceState(
        initialValue = FavoriteActionAppOptions(),
        key1 = selectedContact?.id,
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
    val integratedAddressProviders by produceState(
        initialValue = emptyList(),
        key1 = showAddPerson,
        key2 = reconnectingIntegratedContact?.id,
        key3 = resumeRevision,
    ) {
        value = if (!showAddPerson && reconnectingIntegratedContact == null) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) {
                actions.messagingProviderOptions().filter { option ->
                    option.installed && !option.systemDefault &&
                        option.installedPackageName != null && option.addressTypes.isNotEmpty()
                }
            }
        }
    }
    val contacts by produceState<List<FavoriteAndroidContact>?>(
        initialValue = null,
        repository,
        contactsPermissionGranted,
        revision,
        resumeRevision,
    ) {
        value = if (!contactsPermissionGranted) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) {
                runCatching(repository::favorites).getOrDefault(emptyList())
            }
        }
    }
    val locale = context.resources.configuration.locales[0]
    val favoriteContacts = remember(contacts, integratedContacts, locale) {
        contacts?.let { androidContacts -> integratedContacts?.let { ownedContacts ->
            FavoriteContactCatalog.combine(
                androidContacts = androidContacts,
                integratedContacts = ownedContacts,
                locale = locale,
            )
        } }
    }
    val integratedAvailability = remember(
        integratedContacts,
        shortcutsRevision,
        resumeRevision,
    ) {
        integratedContacts.orEmpty().associate { contact ->
            contact.id to actions.integratedContactAvailability(contact)
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

    fun perform(
        contact: FavoriteAndroidContact,
        phone: AndroidContactPhone,
        action: FavoriteContactAction,
    ) {
        val routes = routeStore.routes(contact.id)
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
            FavoriteContactAction.CALL -> {
                val packageName = routes.call.resolvePackage(store.preferredCallingPackage)
                if (
                    store.callAutomatically && supportsDirectCalls(context) &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    pendingAutomaticCall = phone.rawNumber to packageName
                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                } else {
                    openCall(phone.rawNumber, packageName, store.callAutomatically)
                }
            }
        }
        selectedContact = null
        pendingPhoneChoice = null
    }

    fun requestAction(contact: FavoriteAndroidContact, action: FavoriteContactAction) {
        val routes = routeStore.routes(contact.id)
        val preferredPhone = contact.preferredPhone(
            contact.id,
            routes.preferredPhoneFingerprint,
        )
        when (val selection = contact.phoneSelection(preferredPhone)) {
            AndroidPhoneSelection.None -> selectedContact = contact
            is AndroidPhoneSelection.Resolved -> perform(contact, selection.phone, action)
            is AndroidPhoneSelection.Choose -> {
                selectedContact = null
                pendingPhoneChoice = PendingPhoneChoice(contact, selection.phones, action)
            }
        }
    }

    fun composeIntegratedMessage(contact: FavoriteIntegratedContact) {
        when (val endpoint = contact.endpoints.firstOrNull()) {
            is IntegratedContactEndpoint.ConversationShortcut -> {
                val target = actions.resolveIntegratedContact(endpoint)
                if (target != null) {
                    onComposeConversationMessage(target)
                    return
                }
            }
            is IntegratedContactEndpoint.ProviderAddress -> {
                if (actions.messagingProviderAvailability(endpoint.packageName).installed) {
                    onComposeMessage(
                        CommunicationRecipient(
                            address = endpoint.address,
                            displayName = contact.displayName,
                            detail = actions.appLabel(endpoint.packageName),
                            userEntered = true,
                        ),
                        ContactAppRoute.Package(endpoint.packageName),
                    )
                    return
                }
            }
            null -> Unit
        }
        Toast.makeText(
            context,
            context.getString(R.string.integrated_contact_unavailable),
            Toast.LENGTH_LONG,
        ).show()
    }

    when {
        favoriteContacts == null -> FavoriteContactsLoadingState()
        favoriteContacts.isEmpty() -> FavoriteContactsEmptyState(
            openContacts = actions::openContactsApp,
            onAddPerson = { showAddPerson = true },
            contactsPermissionGranted = contactsPermissionGranted,
            onRequestContacts = onRequestContacts,
        )
        else -> FavoriteContactsList(
            contacts = favoriteContacts,
            integratedAvailability = integratedAvailability,
            onOpen = { contact ->
                when (contact) {
                    is FavoriteAndroidContact -> selectedContact = contact
                    is FavoriteIntegratedContact -> selectedIntegratedContact = contact
                }
            },
            onMessage = { contact ->
                when (contact) {
                    is FavoriteAndroidContact -> requestAction(contact, FavoriteContactAction.MESSAGE)
                    is FavoriteIntegratedContact -> composeIntegratedMessage(contact)
                }
            },
            onCall = { requestAction(it, FavoriteContactAction.CALL) },
            onInstallProvider = { contact ->
                val state = integratedAvailability[contact.id]
                val packageName = contact.endpoints.firstOrNull()?.packageName
                if (
                    state !is FavoriteIntegratedContactAvailability.AppNotInstalled ||
                    packageName == null ||
                    !actions.openMessagingProviderInstallPage(packageName)
                ) {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.could_not_open_app,
                            state?.providerLabel ?: contact.displayName,
                        ),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            onAddPerson = { showAddPerson = true },
        )
    }

    selectedContact?.let { contact ->
        val contactRoutes = remember(contact.id, routeRevision) {
            routeStore.routes(contact.id)
        }
        val preferredPhone = remember(contact.phones, contact.id, contactRoutes) {
            contact.preferredPhone(contact.id, contactRoutes.preferredPhoneFingerprint)
        }
        FavoriteAndroidContactActionDialog(
            contact = contact,
            routes = contactRoutes,
            messagingOptions = actionAppOptions.messaging,
            callingOptions = actionAppOptions.calling,
            preferredPhone = preferredPhone,
            onAction = { requestAction(contact, it) },
            onPreferredPhone = { phone ->
                routeStore.updatePreferredPhone(
                    contact.id,
                    phone?.let { favoritePhoneFingerprint(contact.id, it.normalizedNumber) },
                )
                routeRevision++
            },
            onMessageRoute = { route ->
                routeStore.updateMessage(contact.id, route)
                routeRevision++
            },
            onCallRoute = { route ->
                routeStore.updateCall(contact.id, route)
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

    selectedIntegratedContact?.let { contact ->
        val availability = integratedAvailability[contact.id]
            ?: actions.integratedContactAvailability(contact)
        FavoriteIntegratedContactActionDialog(
            contact = contact,
            availability = availability,
            onMessage = {
                selectedIntegratedContact = null
                composeIntegratedMessage(contact)
            },
            onInstallProvider = {
                val packageName = contact.endpoints.firstOrNull()?.packageName
                if (packageName != null && actions.openMessagingProviderInstallPage(packageName)) {
                    selectedIntegratedContact = null
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.could_not_open_app, availability.providerLabel),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            onReconnect = {
                selectedIntegratedContact = null
                reconnectingIntegratedContact = contact
            },
            onRemove = {
                selectedIntegratedContact = null
                scope.launch {
                    if (withContext(Dispatchers.IO) { integratedStore.remove(contact.id) }) {
                        integratedRevision++
                    }
                }
            },
            onDismiss = { selectedIntegratedContact = null },
        )
    }

    reconnectingIntegratedContact?.let { contact ->
        val shortcuts = remember(
            contact.id,
            shortcutsRevision,
            integratedRevision,
        ) {
            val endpointsUsedByOtherContacts = integratedContacts.orEmpty()
                .filterNot { it.id == contact.id }
                .flatMap(FavoriteIntegratedContact::endpoints)
                .filterIsInstance<IntegratedContactEndpoint.ConversationShortcut>()
                .map { launcherShortcutSelectionKey(it.userSerial, it.packageName, it.shortcutId) }
                .toSet()
            actions.recentConversationShortcuts(
                sendAutomatically = false,
                preferredMessagingPackage = null,
            ).filterNot { it.selectionKey in endpointsUsedByOtherContacts }
        }
        ReconnectIntegratedContactDialog(
            contact = contact,
            shortcuts = shortcuts,
            addressProviders = integratedAddressProviders,
            actions = actions,
            onReconnect = { shortcut ->
                scope.launch {
                    val replaced = withContext(Dispatchers.IO) {
                        integratedStore.replaceEndpoint(contact.id, shortcut)
                    }
                    if (replaced) {
                        integratedRevision++
                        reconnectingIntegratedContact = null
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.could_not_reconnect_conversation),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
            onReconnectAddress = { provider, addressType, address ->
                scope.launch {
                    val packageName = provider.installedPackageName
                    val replaced = packageName != null && withContext(Dispatchers.IO) {
                        integratedStore.replaceProviderAddress(
                            id = contact.id,
                            providerId = provider.id,
                            packageName = packageName,
                            addressType = addressType,
                            address = address,
                        )
                    }
                    if (replaced) {
                        integratedRevision++
                        reconnectingIntegratedContact = null
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.could_not_reconnect_conversation),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
            onDismiss = { reconnectingIntegratedContact = null },
        )
    }

    if (showAddPerson) {
        val shortcuts = remember(shortcutsRevision, integratedRevision) {
            val imported = integratedContacts.orEmpty()
                .flatMap(FavoriteIntegratedContact::endpoints)
                .filterIsInstance<IntegratedContactEndpoint.ConversationShortcut>()
                .map { launcherShortcutSelectionKey(it.userSerial, it.packageName, it.shortcutId) }
                .toSet()
            actions.recentConversationShortcuts(
                sendAutomatically = false,
                preferredMessagingPackage = null,
            ).filterNot { it.selectionKey in imported }
        }
        AddIntegratedContactDialog(
            shortcuts = shortcuts,
            addressProviders = integratedAddressProviders,
            actions = actions,
            onOpenContacts = {
                showAddPerson = false
                if (contactsPermissionGranted) {
                    actions.openContactsApp()
                } else {
                    onRequestContacts()
                }
            },
            onAdd = { shortcut, displayName ->
                scope.launch {
                    when (withContext(Dispatchers.IO) {
                        integratedStore.add(shortcut, displayName)
                    }) {
                        FavoriteIntegratedContactAddResult.ADDED -> {
                            integratedRevision++
                            showAddPerson = false
                        }
                        FavoriteIntegratedContactAddResult.DUPLICATE -> Toast.makeText(
                            context,
                            context.getString(R.string.favorite_already_added),
                            Toast.LENGTH_SHORT,
                        ).show()
                        FavoriteIntegratedContactAddResult.FAILED -> Toast.makeText(
                            context,
                            context.getString(R.string.could_not_add_favorite),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
            onAddAddress = { provider, addressType, address, displayName ->
                scope.launch {
                    val packageName = provider.installedPackageName
                    val result = if (packageName != null) {
                        withContext(Dispatchers.IO) {
                            integratedStore.addProviderAddress(
                                providerId = provider.id,
                                packageName = packageName,
                                addressType = addressType,
                                address = address,
                                displayName = displayName,
                            )
                        }
                    } else {
                        FavoriteIntegratedContactAddResult.FAILED
                    }
                    when (result) {
                        FavoriteIntegratedContactAddResult.ADDED -> {
                            integratedRevision++
                            showAddPerson = false
                        }
                        FavoriteIntegratedContactAddResult.DUPLICATE -> Toast.makeText(
                            context,
                            context.getString(R.string.favorite_already_added),
                            Toast.LENGTH_SHORT,
                        ).show()
                        FavoriteIntegratedContactAddResult.FAILED -> Toast.makeText(
                            context,
                            context.getString(R.string.could_not_add_favorite),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
            onDismiss = { showAddPerson = false },
        )
    }
}

private data class PendingPhoneChoice(
    val contact: FavoriteAndroidContact,
    val phones: List<AndroidContactPhone>,
    val action: FavoriteContactAction,
)

private data class FavoriteActionAppOptions(
    val messaging: List<com.katoaapps.openminilaunch.features.messaging.MessagingProviderOption> =
        emptyList(),
    val calling: List<com.katoaapps.openminilaunch.features.contacts.CallingAppOption> = emptyList(),
)
