package com.katoaapps.openminilaunch.ui.settings

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.SearchFolder
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsRow
import com.katoaapps.openminilaunch.ui.components.SettingsSwitchRow
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust
import com.katoaapps.openminilaunch.ui.theme.Sage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow

@Composable
internal fun MagicBoxSettingsPage(
    store: LauncherStore,
    actions: DeviceActions,
    mediaGranted: Boolean,
    onPickWeb: () -> Unit,
    onPickAi: () -> Unit,
    onOpenFileSearch: () -> Unit,
    goBack: () -> Unit,
) {
    SettingsPage(stringResource(R.string.magic_box), goBack) {
        SectionLabel(stringResource(R.string.keyboard))
        SettingsSwitchRow(
            title = stringResource(R.string.open_keyboard_on_home),
            subtitle = stringResource(R.string.open_keyboard_on_home_description),
            checked = store.openSoftwareKeyboardOnHome,
            onCheckedChange = store::updateOpenSoftwareKeyboardOnHome,
        )
        HorizontalDivider(color = Sage)
        SectionLabel(stringResource(R.string.search))
        SettingsRow(
            stringResource(R.string.web_app),
            store.preferredWebPackage?.let(actions::appLabel) ?: stringResource(R.string.system_browser),
            Icons.Default.Public,
            onClick = onPickWeb,
        )
        SettingsRow(
            stringResource(R.string.ai_app),
            store.preferredAiPackage?.let(actions::appLabel) ?: stringResource(R.string.choose_on_first_use),
            Icons.Default.AutoAwesome,
            onClick = onPickAi,
        )
        Text(stringResource(R.string.ai_provider_privacy_description), color = Muted, fontSize = Dimens.sp13)
        HorizontalDivider(color = Sage)
        SectionLabel(stringResource(R.string.files_section))
        SettingsRow(
            stringResource(R.string.file_search),
            stringResource(
                R.string.file_search_status,
                folderCountLabel(store.searchFolders.size),
                stringResource(if (mediaGranted) R.string.status_on else R.string.status_off),
            ),
            Icons.Default.FolderOpen,
            onClick = onOpenFileSearch,
        )
    }
}

@Composable
internal fun MinkAssistantSettingsPage(
    assistantRoleHeld: Boolean,
    showAssistantDisclosure: () -> Unit,
    goBack: () -> Unit,
) {
    SettingsPage(stringResource(R.string.mink_assistant), goBack) {
        Text(stringResource(R.string.assistant_settings_description), color = Muted, fontSize = Dimens.sp13)
        SettingsRow(
            title = stringResource(
                if (assistantRoleHeld) R.string.manage_mink_assistant else R.string.choose_mink_assistant,
            ),
            subtitle = stringResource(
                if (assistantRoleHeld) R.string.assistant_active_description else R.string.assistant_inactive_description,
            ),
            icon = Icons.Default.Assistant,
            onClick = showAssistantDisclosure,
        )
    }
}

@Composable
internal fun MessagingSettingsPage(
    store: LauncherStore,
    actions: DeviceActions,
    onPickMessagingApp: () -> Unit,
    goBack: () -> Unit,
) {
    SettingsPage(stringResource(R.string.messaging), goBack) {
        SettingsRow(
            stringResource(R.string.preferred_messaging_app),
            store.preferredMessagingPackage?.let(actions::appLabel) ?: actions.defaultMessagingAppLabel(),
            Icons.AutoMirrored.Filled.Chat,
            onClick = onPickMessagingApp,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.send_messages_automatically),
            subtitle = stringResource(R.string.send_messages_automatically_description),
            checked = store.sendMessagesAutomatically,
            onCheckedChange = store::updateSendMessagesAutomatically,
        )
        Text(stringResource(R.string.messaging_behavior_description), color = Muted, fontSize = Dimens.sp13)
    }
}

@Composable
internal fun FileSearchSettingsPage(
    store: LauncherStore,
    mediaGranted: Boolean,
    onOpenPermissions: () -> Unit,
    onAddFolder: () -> Unit,
    onRemoveFolder: (SearchFolder) -> Unit,
    goBack: () -> Unit,
) {
    val appName = stringResource(R.string.app_name)
    SettingsPage(stringResource(R.string.file_search), goBack) {
        Text(stringResource(R.string.file_search_description, appName), color = Muted, fontSize = Dimens.sp13)
        SettingsRow(
            stringResource(R.string.photos_videos_audio),
            stringResource(if (mediaGranted) R.string.media_access_active else R.string.media_access_open_permissions),
            Icons.Default.PhotoLibrary,
            onClick = onOpenPermissions,
        )
        OutlinedButton(onClick = onAddFolder, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CreateNewFolder, null)
            Text(stringResource(R.string.add_search_folder), Modifier.padding(start = Dimens.dp8))
        }
        if (store.searchFolders.isEmpty()) {
            Text(stringResource(R.string.no_document_folders), color = Muted, fontSize = Dimens.sp12)
        } else {
            store.searchFolders.forEach { folder ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp14))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(start = Dimens.dp14, end = Dimens.dp4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Folder, null, tint = Rust)
                    Text(
                        folder.label,
                        Modifier.weight(1f).padding(horizontal = Dimens.dp10),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(onClick = { onRemoveFolder(folder) }) {
                        Icon(Icons.Default.Close, stringResource(R.string.remove_folder, folder.label))
                    }
                }
            }
        }
    }
}
