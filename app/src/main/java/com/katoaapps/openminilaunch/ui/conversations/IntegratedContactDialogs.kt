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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.contacts.FAVORITE_INTEGRATED_CONTACT_NAME_MAX_CHARS
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.DrawableIcon
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import java.text.Collator

@Composable
internal fun AddIntegratedContactDialog(
    shortcuts: List<LauncherShortcutTarget>,
    actions: DeviceActions,
    onOpenContacts: () -> Unit,
    onAdd: (LauncherShortcutTarget, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val collator = remember { Collator.getInstance() }
    val providers = remember(shortcuts) {
        shortcuts
            .groupBy(LauncherShortcutTarget::packageName)
            .map { (packageName, providerShortcuts) ->
                IntegratedContactProvider(
                    packageName = packageName,
                    label = actions.appLabel(packageName),
                    shortcuts = providerShortcuts.sortedWith { first, second ->
                        collator.compare(first.label, second.label)
                    },
                )
            }
            .sortedWith { first, second -> collator.compare(first.label, second.label) }
    }
    var selectedProviderPackage by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<LauncherShortcutTarget?>(null) }
    var displayName by remember { mutableStateOf("") }
    val selectedProvider = providers.firstOrNull {
        it.packageName == selectedProviderPackage
    }
    val returnToProviders = {
        selectedProviderPackage = null
        selected = null
        displayName = ""
    }

    Dialog(
        onDismissRequest = {
            if (selectedProvider != null) returnToProviders() else onDismiss()
        },
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
                    if (selectedProvider != null) {
                        IconButton(onClick = returnToProviders) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(R.string.back),
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.add_person),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            selectedProvider?.label
                                ?: stringResource(R.string.add_person_description),
                            color = Muted,
                            maxLines = if (selectedProvider == null) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = Dimens.dp4),
                        )
                    }
                }
                if (selectedProvider == null) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(top = Dimens.dp12),
                        verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
                    ) {
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
                                    Text(
                                        provider.shortcuts.size.toString(),
                                        color = Muted,
                                        style = MaterialTheme.typography.labelLarge,
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
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(top = Dimens.dp12),
                        verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
                    ) {
                        items(
                            selectedProvider.shortcuts,
                            key = LauncherShortcutTarget::selectionKey,
                        ) { shortcut ->
                            val isSelected = selected?.selectionKey == shortcut.selectionKey
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selected = shortcut
                                    displayName = shortcut.label
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
                selected?.let {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { value ->
                            displayName = value.take(FAVORITE_INTEGRATED_CONTACT_NAME_MAX_CHARS)
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
                        onClick = { selected?.let { onAdd(it, displayName) } },
                        enabled = selected != null && displayName.isNotBlank(),
                    ) {
                        Icon(Icons.Default.PersonAdd, null, Modifier.size(Dimens.dp18))
                        Text(stringResource(R.string.add), Modifier.padding(start = Dimens.dp6))
                    }
                }
            }
        }
    }
}

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

private data class IntegratedContactProvider(
    val packageName: String,
    val label: String,
    val shortcuts: List<LauncherShortcutTarget>,
)

@Composable
internal fun FavoriteIntegratedContactActionDialog(
    contact: FavoriteIntegratedContact,
    providerLabel: String?,
    onMessage: () -> Unit,
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
                    providerLabel ?: stringResource(R.string.integrated_contact),
                    color = Muted,
                    modifier = Modifier.padding(top = Dimens.dp4, bottom = Dimens.dp18),
                )
                Button(onClick = onMessage, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.Message, null, Modifier.size(Dimens.dp18))
                    Text(stringResource(R.string.message), Modifier.padding(start = Dimens.dp6))
                }
                TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DeleteOutline, null, Modifier.size(Dimens.dp18))
                    Text(stringResource(R.string.remove_from_favorites), Modifier.padding(start = Dimens.dp6))
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}
