package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.contacts.FAVORITE_INTEGRATED_CONTACT_NAME_MAX_CHARS
import com.katoaapps.openminilaunch.features.contacts.FAVORITE_PROVIDER_ADDRESS_MAX_CHARS
import com.katoaapps.openminilaunch.features.contacts.CallingAppOption
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderOption
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.model.ProviderAddressType
import com.katoaapps.openminilaunch.model.looksLikePhoneRecipient
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.DrawableIcon
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import java.text.Collator

@Composable
internal fun AddIntegratedContactDialog(
    shortcuts: List<LauncherShortcutTarget>,
    addressProviders: List<MessagingProviderOption>,
    callingApps: List<CallingAppOption>,
    actions: DeviceActions,
    onOpenContacts: () -> Unit,
    onAdd: (LauncherShortcutTarget, String) -> Unit,
    onAddAddress: (MessagingProviderOption, ProviderAddressType, String, String) -> Unit,
    onAddCall: (CallingAppOption?, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    IntegratedContactPickerDialog(
        shortcuts = shortcuts,
        addressProviders = addressProviders,
        callingApps = callingApps,
        actions = actions,
        mode = IntegratedContactPickerMode.ADD,
        displayName = "",
        onOpenContacts = onOpenContacts,
        onSelect = onAdd,
        onSelectAddress = onAddAddress,
        onSelectCall = onAddCall,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun ReconnectIntegratedContactDialog(
    contact: FavoriteIntegratedContact,
    shortcuts: List<LauncherShortcutTarget>,
    addressProviders: List<MessagingProviderOption>,
    actions: DeviceActions,
    onReconnect: (LauncherShortcutTarget) -> Unit,
    onReconnectAddress: (MessagingProviderOption, ProviderAddressType, String) -> Unit,
    onDismiss: () -> Unit,
) {
    IntegratedContactPickerDialog(
        shortcuts = shortcuts,
        addressProviders = addressProviders,
        callingApps = emptyList(),
        actions = actions,
        mode = IntegratedContactPickerMode.RECONNECT,
        displayName = contact.displayName,
        onOpenContacts = null,
        onSelect = { shortcut, _ -> onReconnect(shortcut) },
        onSelectAddress = { provider, addressType, address, _ ->
            onReconnectAddress(provider, addressType, address)
        },
        onSelectCall = { _, _, _ -> },
        onDismiss = onDismiss,
    )
}

@Composable
private fun IntegratedContactPickerDialog(
    shortcuts: List<LauncherShortcutTarget>,
    addressProviders: List<MessagingProviderOption>,
    callingApps: List<CallingAppOption>,
    actions: DeviceActions,
    mode: IntegratedContactPickerMode,
    displayName: String,
    onOpenContacts: (() -> Unit)?,
    onSelect: (LauncherShortcutTarget, String) -> Unit,
    onSelectAddress: (MessagingProviderOption, ProviderAddressType, String, String) -> Unit,
    onSelectCall: (CallingAppOption?, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val collator = remember { Collator.getInstance() }
    val providers = remember(shortcuts, addressProviders) {
        val shortcutsByPackage = shortcuts.groupBy(LauncherShortcutTarget::packageName)
        val addressProvidersByPackage = addressProviders
            .mapNotNull { option -> option.installedPackageName?.let { it to option } }
            .toMap()
        (shortcutsByPackage.keys + addressProvidersByPackage.keys)
            .map { packageName ->
                IntegratedContactProvider(
                    packageName = packageName,
                    label = addressProvidersByPackage[packageName]?.label
                        ?: actions.appLabel(packageName),
                    shortcuts = shortcutsByPackage[packageName].orEmpty().sortedWith { first, second ->
                        collator.compare(first.label, second.label)
                    },
                    addressProvider = addressProvidersByPackage[packageName],
                )
            }
            .sortedWith { first, second -> collator.compare(first.label, second.label) }
    }
    var selectedProviderPackage by remember { mutableStateOf<String?>(null) }
    var showingMessagingApps by remember(mode) {
        mutableStateOf(mode == IntegratedContactPickerMode.RECONNECT)
    }
    var showingCallingApps by remember { mutableStateOf(false) }
    var selectedCallingRoute by remember { mutableStateOf<CompatibleCallingRoute?>(null) }
    var selectedPhoneNumber by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<LauncherShortcutTarget?>(null) }
    var selectedAddressType by remember { mutableStateOf<ProviderAddressType?>(null) }
    var selectedAddress by remember { mutableStateOf("") }
    var selectedDisplayName by remember(displayName) { mutableStateOf(displayName) }
    val selectedProvider = providers.firstOrNull {
        it.packageName == selectedProviderPackage
    }
    val returnToRoot = {
        selectedProviderPackage = null
        showingMessagingApps = mode == IntegratedContactPickerMode.RECONNECT
        showingCallingApps = false
        selectedCallingRoute = null
        selectedPhoneNumber = ""
        selected = null
        selectedAddressType = null
        selectedAddress = ""
        selectedDisplayName = displayName
    }
    val navigateBack = {
        when {
            selectedProvider != null -> {
                selectedProviderPackage = null
                selected = null
                selectedAddressType = null
                selectedAddress = ""
                selectedDisplayName = displayName
            }
            showingMessagingApps && mode == IntegratedContactPickerMode.ADD -> returnToRoot()
            showingCallingApps -> returnToRoot()
            else -> onDismiss()
        }
    }

    Dialog(
        onDismissRequest = navigateBack,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(.96f)
                .widthIn(max = 720.dp)
                .fillMaxHeight(.90f),
            shape = RoundedCornerShape(Dimens.dp24),
            tonalElevation = Dimens.dp6,
        ) {
            Column(Modifier.fillMaxSize().padding(Dimens.dp18)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectedProvider != null || showingMessagingApps || showingCallingApps) {
                        IconButton(onClick = navigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(R.string.back),
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(
                                if (mode == IntegratedContactPickerMode.ADD) {
                                    R.string.add_person
                                } else {
                                    R.string.reconnect_conversation
                                },
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            selectedProvider?.label
                                ?: if (showingCallingApps) {
                                    stringResource(R.string.compatible_calling_apps)
                                } else if (showingMessagingApps) {
                                    stringResource(R.string.messages)
                                } else null
                                ?: stringResource(
                                    if (mode == IntegratedContactPickerMode.ADD) {
                                        R.string.add_person_description
                                    } else {
                                        R.string.reconnect_conversation_description
                                    },
                                ),
                            color = Muted,
                            maxLines = if (selectedProvider == null) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = Dimens.dp4),
                        )
                    }
                }
                if (selectedProvider == null && !showingMessagingApps && !showingCallingApps) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(top = Dimens.dp12),
                        verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
                    ) {
                        if (onOpenContacts != null) {
                            item(key = "android-contacts") {
                                AddPersonSourceRow(
                                    icon = {
                                        Icon(
                                            Icons.Default.ContactPage,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimens.dp38),
                                        )
                                    },
                                    label = stringResource(R.string.contacts),
                                    trailing = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = stringResource(R.string.open_contacts),
                                        )
                                    },
                                    onClick = onOpenContacts,
                                )
                            }
                        }
                        if (mode == IntegratedContactPickerMode.ADD) {
                            item(key = "communication-actions") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
                                ) {
                                    AddPersonActionCard(
                                        icon = {
                                            Icon(
                                                Icons.Default.AlternateEmail,
                                                contentDescription = null,
                                                modifier = Modifier.size(Dimens.dp30),
                                            )
                                        },
                                        label = stringResource(R.string.message),
                                        modifier = Modifier.weight(1f),
                                        onClick = { showingMessagingApps = true },
                                    )
                                    AddPersonActionCard(
                                        icon = {
                                            Icon(
                                                Icons.Default.Phone,
                                                contentDescription = null,
                                                modifier = Modifier.size(Dimens.dp30),
                                            )
                                        },
                                        label = stringResource(R.string.call),
                                        modifier = Modifier.weight(1f),
                                        onClick = { showingCallingApps = true },
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedProvider == null && showingMessagingApps) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(top = Dimens.dp12),
                        verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
                    ) {
                        items(providers, key = IntegratedContactProvider::packageName) { provider ->
                            AddPersonSourceRow(
                                icon = {
                                    DrawableIcon(
                                        drawable = actions.appIcon(provider.packageName),
                                        iconKey = "integrated-provider:${provider.packageName}",
                                        size = Dimens.dp38,
                                    )
                                },
                                label = provider.label,
                                trailing = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                    )
                                },
                                onClick = { selectedProviderPackage = provider.packageName },
                            )
                        }
                        if (providers.isEmpty()) {
                            item(key = "no-integrations") {
                                Text(
                                    stringResource(R.string.no_integrated_contacts_available),
                                    color = Muted,
                                    modifier = Modifier.fillMaxWidth().padding(Dimens.dp12),
                                )
                            }
                        }
                    }
                } else if (showingCallingApps) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(top = Dimens.dp12),
                        verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
                    ) {
                        item(key = "system-calling-app") {
                            CallingAppRow(
                                label = stringResource(R.string.use_system_calling_app),
                                selected = selectedCallingRoute?.option == null &&
                                    selectedCallingRoute != null,
                                icon = {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = null,
                                        modifier = Modifier.size(Dimens.dp34),
                                    )
                                },
                                onClick = {
                                    selectedCallingRoute = CompatibleCallingRoute(null)
                                    selectedPhoneNumber = ""
                                    selectedDisplayName = ""
                                },
                            )
                        }
                        items(callingApps, key = CallingAppOption::packageName) { app ->
                            CallingAppRow(
                                label = app.label,
                                selected = selectedCallingRoute?.option?.packageName ==
                                    app.packageName,
                                icon = {
                                    DrawableIcon(
                                        drawable = actions.appIcon(app.packageName),
                                        iconKey = "compatible-calling:${app.packageName}",
                                        size = Dimens.dp34,
                                    )
                                },
                                onClick = {
                                    selectedCallingRoute = CompatibleCallingRoute(app)
                                    selectedPhoneNumber = ""
                                    selectedDisplayName = ""
                                },
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(top = Dimens.dp12),
                        verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
                    ) {
                        selectedProvider?.addressProvider?.addressTypes
                            ?.sortedBy(ProviderAddressType::ordinal)
                            ?.forEach { addressType ->
                                    item(key = "manual:${addressType.name}") {
                                        AddPersonSourceRow(
                                            icon = {
                                                Icon(
                                                    if (addressType == ProviderAddressType.PHONE) {
                                                        Icons.Default.Phone
                                                    } else {
                                                        Icons.Default.AlternateEmail
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(Dimens.dp34),
                                                )
                                            },
                                            label = stringResource(
                                                if (addressType == ProviderAddressType.PHONE) {
                                                    R.string.add_by_phone_number
                                                } else {
                                                    R.string.add_by_username
                                                },
                                            ),
                                            trailing = {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                )
                                            },
                                            onClick = {
                                                selected = null
                                                selectedAddressType = addressType
                                                selectedAddress = ""
                                                selectedDisplayName = if (
                                                    mode == IntegratedContactPickerMode.ADD
                                                ) {
                                                    ""
                                                } else {
                                                    displayName
                                                }
                                            },
                                        )
                                    }
                                }
                        items(
                            selectedProvider?.shortcuts.orEmpty(),
                            key = LauncherShortcutTarget::selectionKey,
                        ) { shortcut ->
                            val isSelected = selected?.selectionKey == shortcut.selectionKey
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selected = shortcut
                                    selectedAddressType = null
                                    selectedAddress = ""
                                    if (mode == IntegratedContactPickerMode.ADD) {
                                        selectedDisplayName = shortcut.label
                                    }
                                },
                                shape = RoundedCornerShape(Dimens.dp14),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(Dimens.dp10),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    DrawableIcon(
                                        drawable = actions.launcherTargetIcon(shortcut)
                                            ?: actions.appIcon(shortcut.packageName),
                                        iconKey = "integrated-picker:${shortcut.selectionKey}",
                                        size = Dimens.dp34,
                                    )
                                    Column(Modifier.weight(1f).padding(start = Dimens.dp10)) {
                                        Text(
                                            shortcut.label,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (selectedCallingRoute != null) {
                    OutlinedTextField(
                        value = selectedPhoneNumber,
                        onValueChange = {
                            selectedPhoneNumber = it.take(FAVORITE_PROVIDER_ADDRESS_MAX_CHARS)
                        },
                        label = { Text(stringResource(R.string.phone_number)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp12),
                    )
                }
                if (selectedAddressType != null) {
                    OutlinedTextField(
                        value = selectedAddress,
                        onValueChange = {
                            selectedAddress = it.take(FAVORITE_PROVIDER_ADDRESS_MAX_CHARS)
                        },
                        label = {
                            Text(
                                stringResource(
                                    if (selectedAddressType == ProviderAddressType.PHONE) {
                                        R.string.phone_number
                                    } else {
                                        R.string.username
                                    },
                                ),
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (
                                selectedAddressType == ProviderAddressType.PHONE
                            ) {
                                KeyboardType.Phone
                            } else {
                                KeyboardType.Text
                            },
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp12),
                    )
                }
                if (mode == IntegratedContactPickerMode.ADD &&
                    (selected != null || selectedAddressType != null || selectedCallingRoute != null)
                ) {
                    OutlinedTextField(
                        value = selectedDisplayName,
                        onValueChange = { value ->
                            selectedDisplayName =
                                value.take(FAVORITE_INTEGRATED_CONTACT_NAME_MAX_CHARS)
                        },
                        label = { Text(stringResource(R.string.display_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp12),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = Dimens.dp14),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                    Button(
                        onClick = {
                            selected?.let { onSelect(it, selectedDisplayName) }
                                ?: selectedAddressType?.let { addressType ->
                                    selectedProvider?.addressProvider?.let { provider ->
                                        onSelectAddress(
                                            provider,
                                            addressType,
                                            selectedAddress,
                                            selectedDisplayName,
                                        )
                                    }
                                }
                                ?: selectedCallingRoute?.let { route ->
                                    onSelectCall(
                                        route.option,
                                        selectedPhoneNumber,
                                        selectedDisplayName,
                                    )
                                }
                        },
                        enabled = selectedDisplayName.isNotBlank() && (
                            selected != null || selectedAddressType?.let {
                                selectedAddress.isValidProviderAddress(it)
                            } == true || selectedCallingRoute?.let {
                                selectedPhoneNumber.trim().looksLikePhoneRecipient()
                            } == true
                        ),
                    ) {
                        Icon(
                            if (mode == IntegratedContactPickerMode.ADD) {
                                Icons.Default.PersonAdd
                            } else {
                                Icons.Default.Refresh
                            },
                            null,
                            Modifier.size(Dimens.dp18),
                        )
                        Text(
                            stringResource(
                                if (mode == IntegratedContactPickerMode.ADD) {
                                    R.string.add
                                } else {
                                    R.string.reconnect
                                },
                            ),
                            Modifier.padding(start = Dimens.dp6),
                        )
                    }
                }
            }
        }
    }
}

private enum class IntegratedContactPickerMode { ADD, RECONNECT }

@Composable
private fun AddPersonSourceRow(
    icon: @Composable () -> Unit,
    label: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.dp14),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(Dimens.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Text(
                label,
                modifier = Modifier.weight(1f).padding(horizontal = Dimens.dp12),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            trailing()
        }
    }
}

@Composable
private fun AddPersonActionCard(
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.dp14),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(vertical = Dimens.dp16, horizontal = Dimens.dp10),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
        ) {
            icon()
            Text(
                label,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class IntegratedContactProvider(
    val packageName: String,
    val label: String,
    val shortcuts: List<LauncherShortcutTarget>,
    val addressProvider: MessagingProviderOption?,
)

private data class CompatibleCallingRoute(val option: CallingAppOption?)

@Composable
private fun CallingAppRow(
    label: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.dp14),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(Dimens.dp10),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Text(
                label,
                modifier = Modifier.weight(1f).padding(start = Dimens.dp10),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun String.isValidProviderAddress(type: ProviderAddressType): Boolean = when (type) {
    ProviderAddressType.PHONE -> trim().looksLikePhoneRecipient()
    ProviderAddressType.USERNAME -> trim().removePrefix("@").let { username ->
        username.isNotBlank() && username.none(Char::isWhitespace)
    }
}
