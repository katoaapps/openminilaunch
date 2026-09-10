package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.files.FileSearchRepository
import com.katoaapps.openminilaunch.features.apps.launcherDiscoveryLabel
import com.katoaapps.openminilaunch.features.magic.MAGIC_COMMAND_PREFIXES
import com.katoaapps.openminilaunch.model.ContactResult
import com.katoaapps.openminilaunch.model.FileSearchResult
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MagicTodoColor
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun MagicResultsPanel(
    store: LauncherStore,
    actions: DeviceActions,
    rawText: String,
    lockedPrefix: Char?,
    prefix: Char?,
    launcherAppsRevision: Long,
    launcherShortcutsRevision: Long,
    plainQuery: String,
    fileSearchLoading: Boolean,
    fileResults: List<FileSearchResult>,
    fileSearchRepository: FileSearchRepository,
    hasMediaAccess: Boolean,
    canSearchContacts: Boolean,
    contactResults: List<ContactResult>,
    recentConversations: List<LauncherShortcutTarget>,
    forcedRecipient: String?,
    appResults: List<LauncherTarget>,
    includeAppShortcuts: Boolean,
    showClearMessage: Boolean,
    scrollState: ScrollState,
    onSelectHistory: (String) -> Unit,
    onOpenFile: (FileSearchResult) -> Unit,
    onRequestMedia: () -> Unit,
    onRequestFolder: () -> Unit,
    onSubmitWeb: () -> Unit,
    onSubmitAi: () -> Unit,
    onSelectContact: (ContactResult) -> Unit,
    onSelectRecentConversation: (LauncherShortcutTarget) -> Unit,
    onSelectForcedRecipient: (String) -> Unit,
    onSelectApp: (LauncherTarget) -> Unit,
    onIncludeAppShortcutsChange: (Boolean) -> Unit,
    onRequestContacts: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
        ) {
            MagicResultsContent(
                store = store,
                actions = actions,
                rawText = rawText,
                lockedPrefix = lockedPrefix,
                prefix = prefix,
                launcherAppsRevision = launcherAppsRevision,
                launcherShortcutsRevision = launcherShortcutsRevision,
                plainQuery = plainQuery,
                fileSearchLoading = fileSearchLoading,
                fileResults = fileResults,
                fileSearchRepository = fileSearchRepository,
                hasMediaAccess = hasMediaAccess,
                canSearchContacts = canSearchContacts,
                contactResults = contactResults,
                recentConversations = recentConversations,
                forcedRecipient = forcedRecipient,
                appResults = appResults,
                includeAppShortcuts = includeAppShortcuts,
                onSelectHistory = onSelectHistory,
                onOpenFile = onOpenFile,
                onRequestMedia = onRequestMedia,
                onRequestFolder = onRequestFolder,
                onSubmitWeb = onSubmitWeb,
                onSubmitAi = onSubmitAi,
                onSelectContact = onSelectContact,
                onSelectRecentConversation = onSelectRecentConversation,
                onSelectForcedRecipient = onSelectForcedRecipient,
                onSelectApp = onSelectApp,
                onIncludeAppShortcutsChange = onIncludeAppShortcutsChange,
                onRequestContacts = onRequestContacts,
            )
        }
        if (showClearMessage) {
            FilledTonalIconButton(
                onClick = onClearMessage,
                modifier = Modifier.align(Alignment.TopEnd),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Icon(Icons.Default.DeleteOutline, stringResource(R.string.clear_message_draft))
            }
        }
    }
}

/** Search and command suggestions shown above the Magic Box input. */
@Composable
internal fun MagicResultsContent(
    store: LauncherStore,
    actions: DeviceActions,
    rawText: String,
    lockedPrefix: Char?,
    prefix: Char?,
    launcherAppsRevision: Long,
    launcherShortcutsRevision: Long,
    plainQuery: String,
    fileSearchLoading: Boolean,
    fileResults: List<FileSearchResult>,
    fileSearchRepository: FileSearchRepository,
    hasMediaAccess: Boolean,
    canSearchContacts: Boolean,
    contactResults: List<ContactResult>,
    recentConversations: List<LauncherShortcutTarget>,
    forcedRecipient: String?,
    appResults: List<LauncherTarget>,
    includeAppShortcuts: Boolean,
    onSelectHistory: (String) -> Unit,
    onOpenFile: (FileSearchResult) -> Unit,
    onRequestMedia: () -> Unit,
    onRequestFolder: () -> Unit,
    onSubmitWeb: () -> Unit,
    onSubmitAi: () -> Unit,
    onSelectContact: (ContactResult) -> Unit,
    onSelectRecentConversation: (LauncherShortcutTarget) -> Unit,
    onSelectForcedRecipient: (String) -> Unit,
    onSelectApp: (LauncherTarget) -> Unit,
    onIncludeAppShortcutsChange: (Boolean) -> Unit,
    onRequestContacts: () -> Unit,
) {
    if (rawText.isBlank() && lockedPrefix == null && store.searchHistory.isNotEmpty()) {
        SearchHistoryList(
            queries = store.searchHistory,
            onSelect = onSelectHistory,
            onDelete = store::removeSearchQuery,
            onClearAll = store::clearSearchHistory,
        )
    }
    if (plainQuery.isNotBlank() && fileSearchLoading) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp6)) {
            Text(
                text = stringResource(R.string.searching_this_device),
                style = MaterialTheme.typography.labelMedium,
                color = Muted,
            )
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
    if (fileResults.isNotEmpty()) {
        FileResultsGrid(fileResults, fileSearchRepository, onOpenFile)
    }
    if (plainQuery.length >= 2 && !hasMediaAccess && !store.demoSearchDataEnabled) {
        FilledTonalButton(onClick = onRequestMedia) {
            Icon(Icons.Default.PhotoLibrary, null, Modifier.size(Dimens.dp18))
            Text(stringResource(R.string.search_media_filenames), Modifier.padding(start = Dimens.dp8))
        }
    }
    if (plainQuery.isNotBlank() && store.searchFolders.isEmpty() && !store.demoSearchDataEnabled) {
        Surface(
            onClick = onRequestFolder,
            shape = RoundedCornerShape(Dimens.dp14),
            color = MagicTodoColor.copy(alpha = .18f),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(Dimens.dp12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.FolderOff, null, tint = MagicTodoColor)
                Column(Modifier.weight(1f).padding(start = Dimens.dp10)) {
                    Text(stringResource(R.string.document_search_not_set_up), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.choose_document_search_folder), color = Muted, fontSize = Dimens.sp12)
                }
                Icon(Icons.Default.ChevronRight, null)
            }
        }
    }
    if (prefix !in MAGIC_COMMAND_PREFIXES && rawText.isNotBlank()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
            SearchDestinationButton(
                label = stringResource(R.string.web),
                detail = stringResource(R.string.search_browser),
                icon = Icons.Default.Public,
                modifier = Modifier.weight(1f),
                onClick = onSubmitWeb,
            )
            SearchDestinationButton(
                label = stringResource(R.string.ai),
                detail = store.preferredAiPackage?.let(actions::appLabel) ?: stringResource(R.string.choose_an_app),
                icon = Icons.Default.AutoAwesome,
                modifier = Modifier.weight(1f),
                onClick = onSubmitAi,
            )
        }
    }
    if (prefix == '@' && contactResults.isNotEmpty()) {
        MagicResultSectionTitle(stringResource(R.string.contacts))
    }
    contactResults.forEach { contact ->
        SuggestionRow(
            stringResource(R.string.two_part_label, contact.name, contact.phoneLabel),
            Icons.Default.Person,
        ) { onSelectContact(contact) }
    }
    RecentConversationResults(
        conversations = recentConversations,
        actions = actions,
        appsRevision = launcherAppsRevision,
        shortcutsRevision = launcherShortcutsRevision,
        onOpen = onSelectRecentConversation,
    )
    forcedRecipient?.let { identifier ->
        val isMessage = prefix == '@'
        SuggestionRow(
            text = stringResource(
                if (isMessage) R.string.try_message_recipient else R.string.try_call_recipient,
                identifier,
            ),
            supportingText = stringResource(
                if (isMessage) {
                    R.string.try_message_recipient_beta
                } else {
                    R.string.try_call_recipient_beta
                },
            ),
            icon = Icons.Default.Person,
        ) { onSelectForcedRecipient(identifier) }
    }
    if (prefix == '?') {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.include_app_shortcuts),
                modifier = Modifier.padding(end = Dimens.dp10),
            )
            Switch(
                checked = includeAppShortcuts,
                onCheckedChange = onIncludeAppShortcutsChange,
            )
        }
    }
    appResults.forEach { app ->
        SuggestionRow(
            text = launcherDiscoveryLabel(app, actions::appLabel),
            leadingContent = { LauncherTargetIcon(app, actions, Dimens.dp26) },
        ) { onSelectApp(app) }
    }
    if (prefix in listOf('@', '#') && !canSearchContacts) {
        FilledTonalButton(onClick = onRequestContacts) {
            Text(stringResource(R.string.allow_contacts_to_search_people))
        }
    }
}
