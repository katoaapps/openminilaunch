@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.contacts.CallingAppOption
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderOption
import com.katoaapps.openminilaunch.model.AndroidContactPhone
import com.katoaapps.openminilaunch.model.ContactAppRoute
import com.katoaapps.openminilaunch.model.FavoriteAndroidContact
import com.katoaapps.openminilaunch.model.FavoriteContactActionRoutes
import com.katoaapps.openminilaunch.model.uniquePhones
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

internal enum class FavoriteContactAction {
    MESSAGE,
    CALL,
}

@Composable
internal fun FavoriteAndroidContactActionDialog(
    contact: FavoriteAndroidContact,
    routes: FavoriteContactActionRoutes,
    messagingOptions: List<MessagingProviderOption>,
    callingOptions: List<CallingAppOption>,
    preferredPhone: AndroidContactPhone?,
    attentionColorArgb: Int?,
    onAction: (FavoriteContactAction) -> Unit,
    onPreferredPhone: (AndroidContactPhone?) -> Unit,
    onMessageRoute: (ContactAppRoute) -> Unit,
    onCallRoute: (ContactAppRoute) -> Unit,
    onAttentionColor: (Int?) -> Unit,
    onOpenContact: () -> Unit,
    onDismiss: () -> Unit,
) {
    val availablePhones = contact.uniquePhones()
    Dialog(onDismissRequest = onDismiss, properties = MinkDialogDefaults.properties) {
        Surface(
            modifier = Modifier.minkDialogWidth(),
            shape = RoundedCornerShape(Dimens.dp24),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(Dimens.dp18)) {
                FavoriteContactIdentityHeader(
                    contact = contact,
                    phones = availablePhones,
                    preferredPhone = preferredPhone,
                    onDismiss = onDismiss,
                )
                if (availablePhones.isEmpty()) {
                    Text(
                        stringResource(R.string.no_phone_numbers_description),
                        modifier = Modifier.padding(top = Dimens.dp16),
                        color = Muted,
                    )
                }
                if (availablePhones.size > 1) {
                    PreferredPhonePicker(
                        phones = availablePhones,
                        preferredPhone = preferredPhone,
                        onPhone = onPreferredPhone,
                        modifier = Modifier.padding(top = Dimens.dp14),
                    )
                }
                ManageContactRow(
                    onClick = onOpenContact,
                    modifier = Modifier.padding(top = Dimens.dp18),
                )
                ContactActionAppsSection(
                    routes = routes,
                    messagingOptions = messagingOptions,
                    callingOptions = callingOptions,
                    attentionColorArgb = attentionColorArgb,
                    showActionRoutes = availablePhones.isNotEmpty(),
                    onMessageRoute = onMessageRoute,
                    onCallRoute = onCallRoute,
                    onAttentionColor = onAttentionColor,
                    modifier = Modifier.padding(top = Dimens.dp10),
                )
                FavoriteContactPrimaryActions(
                    enabled = availablePhones.isNotEmpty(),
                    onCall = { onAction(FavoriteContactAction.CALL) },
                    onMessage = { onAction(FavoriteContactAction.MESSAGE) },
                    modifier = Modifier.padding(top = Dimens.dp18),
                )
            }
        }
    }
}
@Composable
private fun FavoriteContactIdentityHeader(
    contact: FavoriteAndroidContact,
    phones: List<AndroidContactPhone>,
    preferredPhone: AndroidContactPhone?,
    onDismiss: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FavoriteContactAvatar(contact.displayName, contact.photoUri, Dimens.dp54)
        Column(Modifier.weight(1f).padding(horizontal = Dimens.dp12)) {
            Text(
                contact.displayName,
                fontWeight = FontWeight.Black,
                fontSize = Dimens.sp20,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                (preferredPhone ?: phones.singleOrNull())?.let { phone ->
                    stringResource(
                        R.string.phone_type_and_number,
                        phone.label,
                        phone.rawNumber,
                    )
                } ?: if (phones.isEmpty()) {
                    stringResource(R.string.no_phone_numbers)
                } else {
                    stringResource(R.string.numbers_available, phones.size)
                },
                color = Muted,
                fontSize = Dimens.sp12,
                maxLines = 2,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, stringResource(R.string.close))
        }
    }
}

@Composable
private fun PreferredPhonePicker(
    phones: List<AndroidContactPhone>,
    preferredPhone: AndroidContactPhone?,
    onPhone: (AndroidContactPhone?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = preferredPhone?.let { phone ->
        stringResource(R.string.phone_type_and_number, phone.label, phone.rawNumber)
    } ?: stringResource(R.string.ask_every_time)

    PersonRouteDropdown(
        label = stringResource(R.string.preferred_number),
        value = currentLabel,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.ask_every_time)) },
            onClick = {
                onPhone(null)
                expanded = false
            },
        )
        phones.forEach { phone ->
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            R.string.phone_type_and_number,
                            phone.label,
                            phone.rawNumber,
                        ),
                    )
                },
                onClick = {
                    onPhone(phone)
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun ManageContactRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.dp16),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.dp14, vertical = Dimens.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Contacts, null)
            Column(Modifier.weight(1f).padding(horizontal = Dimens.dp12)) {
                Text(stringResource(R.string.open_contact), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.change_favorite_in_contacts),
                    color = Muted,
                    fontSize = Dimens.sp11,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
}

@Composable
private fun ContactActionAppsSection(
    routes: FavoriteContactActionRoutes,
    messagingOptions: List<MessagingProviderOption>,
    callingOptions: List<CallingAppOption>,
    attentionColorArgb: Int?,
    showActionRoutes: Boolean,
    onMessageRoute: (ContactAppRoute) -> Unit,
    onCallRoute: (ContactAppRoute) -> Unit,
    onAttentionColor: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FavoriteContactQuickSettings(modifier) {
        if (showActionRoutes) {
            Column {
                MessageRoutePicker(
                    route = routes.message,
                    options = messagingOptions,
                    onRoute = onMessageRoute,
                )
                CallRoutePicker(
                    route = routes.call,
                    options = callingOptions,
                    onRoute = onCallRoute,
                    modifier = Modifier.padding(top = Dimens.dp8),
                )
            }
        }
        FavoriteAttentionColorSetting(
            colorArgb = attentionColorArgb,
            onColorChange = onAttentionColor,
            modifier = Modifier.padding(top = if (showActionRoutes) Dimens.dp10 else Dimens.dp0),
        )
    }
}

@Composable
private fun FavoriteContactPrimaryActions(
    enabled: Boolean,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
    ) {
        Button(
            onClick = onMessage,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.AutoMirrored.Filled.Message, null)
            Text(stringResource(R.string.message), Modifier.padding(start = Dimens.dp6))
        }
        OutlinedButton(
            onClick = onCall,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.Phone, null)
            Text(stringResource(R.string.call), Modifier.padding(start = Dimens.dp6))
        }
    }
}

@Composable
private fun MessageRoutePicker(
    route: ContactAppRoute,
    options: List<MessagingProviderOption>,
    onRoute: (ContactAppRoute) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val systemOption = options.firstOrNull(MessagingProviderOption::systemDefault)
    val packageOptions = options.filter { !it.systemDefault && it.installedPackageName != null }
    val currentLabel = when (route) {
        ContactAppRoute.Inherit -> stringResource(R.string.use_messaging_setting)
        ContactAppRoute.SystemDefault -> systemOption?.label
            ?: stringResource(R.string.system_messages)
        is ContactAppRoute.Package -> options.firstOrNull {
            it.installedPackageName == route.packageName
        }?.label ?: stringResource(R.string.app_unavailable)
    }
    PersonRouteDropdown(
        label = stringResource(R.string.message_with),
        value = currentLabel,
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.use_messaging_setting)) },
            onClick = { onRoute(ContactAppRoute.Inherit); expanded = false },
        )
        DropdownMenuItem(
            text = { Text(systemOption?.label ?: stringResource(R.string.system_messages)) },
            onClick = { onRoute(ContactAppRoute.SystemDefault); expanded = false },
        )
        packageOptions.forEach { option ->
            val packageName = checkNotNull(option.installedPackageName)
            DropdownMenuItem(
                text = { Text(option.label) },
                onClick = {
                    onRoute(ContactAppRoute.Package(packageName))
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun CallRoutePicker(
    route: ContactAppRoute,
    options: List<CallingAppOption>,
    onRoute: (ContactAppRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = when (route) {
        ContactAppRoute.Inherit -> stringResource(R.string.use_calling_setting)
        ContactAppRoute.SystemDefault -> stringResource(R.string.use_system_calling_app)
        is ContactAppRoute.Package -> options.firstOrNull {
            it.packageName == route.packageName
        }?.label ?: stringResource(R.string.app_unavailable)
    }
    PersonRouteDropdown(
        label = stringResource(R.string.call_with),
        value = currentLabel,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.use_calling_setting)) },
            onClick = { onRoute(ContactAppRoute.Inherit); expanded = false },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.use_system_calling_app)) },
            onClick = { onRoute(ContactAppRoute.SystemDefault); expanded = false },
        )
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.label) },
                onClick = {
                    onRoute(ContactAppRoute.Package(option.packageName))
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun PersonRouteDropdown(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.material3.ExposedDropdownMenuBoxScope.() -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            content()
        }
    }
}
