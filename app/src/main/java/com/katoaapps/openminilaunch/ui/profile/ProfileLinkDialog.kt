@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.katoaapps.openminilaunch.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.profile.ProfileLink
import com.katoaapps.openminilaunch.features.profile.ProfileLinkType
import com.katoaapps.openminilaunch.features.profile.ProfileVCard
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Rust

@Composable
internal fun ProfileLinkDialog(
    existing: ProfileLink?,
    onDismiss: () -> Unit,
    onSave: (ProfileLink?, ProfileLinkType, String, String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var type by remember(existing?.id) { mutableStateOf(existing?.type ?: ProfileLinkType.PHONE) }
    var label by remember(existing?.id) { mutableStateOf(existing?.label.orEmpty()) }
    var value by remember(existing?.id) { mutableStateOf(existing?.value.orEmpty()) }
    var expanded by remember { mutableStateOf(false) }
    var confirmDelete by remember(existing?.id) { mutableStateOf(false) }
    val candidate = ProfileLink(existing?.id.orEmpty(), type, label, value)
    val valid = value.isNotBlank() && runCatching { ProfileVCard.isValid(candidate) }.getOrDefault(false)
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_vcard_link_title)) },
            text = { Text(stringResource(R.string.delete_vcard_link_description)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete?.invoke() }) {
                    Text(stringResource(R.string.delete), color = Rust)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.add_profile_link else R.string.edit_profile_link)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp10)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = profileLinkTypeLabel(type),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.link_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        ProfileLinkType.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(profileLinkTypeLabel(option)) },
                                onClick = { type = option; expanded = false },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    label,
                    { label = it },
                    Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.custom_label_optional)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value,
                    { value = it },
                    Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.link_value)) },
                    supportingText = {
                        if (type == ProfileLinkType.SIGNAL) Text(stringResource(R.string.signal_link_requirement))
                        else if (value.isNotBlank() && !valid) Text(stringResource(R.string.profile_link_invalid))
                    },
                )
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.DeleteOutline, null, tint = Rust)
                        Text(stringResource(R.string.delete), color = Rust)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(existing, type, label, value) }, enabled = valid) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun profileLinkTypeLabel(type: ProfileLinkType): String = stringResource(
    when (type) {
        ProfileLinkType.PHONE -> R.string.profile_link_phone
        ProfileLinkType.EMAIL -> R.string.profile_link_email
        ProfileLinkType.WEBSITE -> R.string.profile_link_website
        ProfileLinkType.INSTAGRAM -> R.string.profile_link_instagram
        ProfileLinkType.LINKEDIN -> R.string.profile_link_linkedin
        ProfileLinkType.X -> R.string.profile_link_x
        ProfileLinkType.FACEBOOK -> R.string.profile_link_facebook
        ProfileLinkType.YOUTUBE -> R.string.profile_link_youtube
        ProfileLinkType.TIKTOK -> R.string.profile_link_tiktok
        ProfileLinkType.GITHUB -> R.string.profile_link_github
        ProfileLinkType.WHATSAPP -> R.string.profile_link_whatsapp
        ProfileLinkType.SIGNAL -> R.string.profile_link_signal
        ProfileLinkType.VENMO -> R.string.profile_link_venmo
        ProfileLinkType.PAYPAL -> R.string.profile_link_paypal
        ProfileLinkType.CASH_APP -> R.string.profile_link_cash_app
        ProfileLinkType.CUSTOM_URL -> R.string.profile_link_custom_url
    },
)
