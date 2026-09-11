@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.magic

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.*
import com.katoaapps.openminilaunch.model.*
import com.katoaapps.openminilaunch.platform.*
import com.katoaapps.openminilaunch.features.apps.launcherLibraryTargets
import com.katoaapps.openminilaunch.features.apps.launcherDiscoveryLabel
import com.katoaapps.openminilaunch.features.apps.launcherDiscoveryMatches
import com.katoaapps.openminilaunch.features.ai.*
import com.katoaapps.openminilaunch.features.calendar.*
import com.katoaapps.openminilaunch.features.conversations.*
import com.katoaapps.openminilaunch.features.files.*
import com.katoaapps.openminilaunch.features.magic.*
import com.katoaapps.openminilaunch.features.messaging.*
import com.katoaapps.openminilaunch.features.todos.*
import com.katoaapps.openminilaunch.features.wellbeing.MinkAppAccessState
import com.katoaapps.openminilaunch.ui.components.*
import com.katoaapps.openminilaunch.ui.theme.*
import com.katoaapps.openminilaunch.ui.launcher.hasMediaReadAccess
import com.katoaapps.openminilaunch.ui.launcher.isPermanentlyDenied
import com.katoaapps.openminilaunch.ui.launcher.mediaPermissionPermanentlyDenied
import com.katoaapps.openminilaunch.ui.launcher.mediaReadPermissions
import com.katoaapps.openminilaunch.ui.launcher.supportsDirectCalls
import com.katoaapps.openminilaunch.ui.launcher.supportsDirectSms
import com.katoaapps.openminilaunch.ui.wellbeing.rememberMinkAppAccessState

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun MagicBox(
    store: LauncherStore,
    actions: DeviceActions,
    modifier: Modifier = Modifier,
    collapsedModifier: Modifier = Modifier,
    keyboardInputEnabled: Boolean = true,
    initiallyExpanded: Boolean = false,
    showSoftwareKeyboardOnStart: Boolean = false,
    autoOpenSoftwareKeyboardOnHome: Boolean = false,
    homeRequestToken: Int = 0,
    onTodoAdded: (String) -> Unit = {},
    onExpandedChange: (Boolean) -> Unit = {},
    onSessionComplete: () -> Unit = {},
    appAccessState: MinkAppAccessState? = null,
) {
    val magicBoxMinimumHeight = Dimens.dp64
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val useDirectHardwareInput = hasUsableHardwareKeyboard(
        configuration.keyboard,
        configuration.hardKeyboardHidden,
    )
    val shouldAutoOpenOnHome = shouldAutoOpenSoftwareKeyboard(
        autoOpenSoftwareKeyboardOnHome,
        configuration.keyboard,
        configuration.hardKeyboardHidden,
    )
    val fileSearchRepository = remember { FileSearchRepository(context.applicationContext) }
    val launcherAppsRevision by actions.launcherAppsRevision.collectAsState()
    val launcherShortcutsRevision by actions.launcherShortcutsRevision.collectAsState()
    val localAppAccessState = if (appAccessState == null) rememberMinkAppAccessState(store) else null
    val effectiveAppAccessState = appAccessState ?: checkNotNull(localAppAccessState).value
    var text by remember { mutableStateOf(TextFieldValue()) }
    val initialHardwareKeyCorrection = remember { InitialHardwareKeyCorrection() }
    var selectedRecipient by remember { mutableStateOf<SelectedMessageRecipient?>(null) }
    var lockedPrefix by remember { mutableStateOf<Char?>(null) }
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    var showKeyboardWhileCollapsed by remember {
        mutableStateOf(shouldAutoOpenOnHome && !initiallyExpanded)
    }
    var hasContacts by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    var hasMediaAccess by remember { mutableStateOf(hasMediaReadAccess(context)) }
    var showFileScopeChoice by remember { mutableStateOf(false) }
    var showAiPicker by remember { mutableStateOf(false) }
    var showAllAiApps by remember { mutableStateOf(false) }
    var pendingAiQuery by remember { mutableStateOf<String?>(null) }
    var pendingMessagingChoice by remember { mutableStateOf<MessageDraft?>(null) }
    var showNoteDeleteConfirmation by remember { mutableStateOf(false) }
    var showMessageDiscardConfirmation by remember { mutableStateOf(false) }
    var showCommandDiscardConfirmation by remember { mutableStateOf(false) }
    val callFlow = rememberMagicCallFlow(actions, onSessionComplete)
    val smsFlow = rememberMagicSmsFlow(actions, onSessionComplete)
    var aiProvidersLoaded by remember { mutableStateOf(false) }
    val aiProviders by produceState<List<AiProviderOption>>(
        initialValue = emptyList(),
        key1 = showAiPicker,
        key2 = launcherAppsRevision,
    ) {
        if (showAiPicker) {
            aiProvidersLoaded = false
            value = withContext(Dispatchers.IO) { actions.aiProviderOptions() }
            aiProvidersLoaded = true
        }
    }
    var allAiAppsLoaded by remember { mutableStateOf(false) }
    val allAiApps by produceState<List<LaunchableApp>>(
        initialValue = emptyList(),
        key1 = showAllAiApps,
        key2 = launcherAppsRevision,
    ) {
        if (showAllAiApps) {
            allAiAppsLoaded = false
            value = withContext(Dispatchers.IO) { actions.compatibleAiApps() }
            allAiAppsLoaded = true
        }
    }
    val messagingProviders by produceState(
        initialValue = MessagingOptionsLoadState(),
        key1 = pendingMessagingChoice,
    ) {
        if (pendingMessagingChoice != null) {
            value = MessagingOptionsLoadState(
                options = withContext(Dispatchers.IO) { actions.messagingProviderOptions() },
                loaded = true,
            )
        }
    }
    DisposableEffect(context) {
        val lifecycle = (context as? ComponentActivity)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                hasMediaAccess = hasMediaReadAccess(context)
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasContacts = granted
        if (!granted && isPermanentlyDenied(context, Manifest.permission.READ_CONTACTS)) actions.openAppSettings()
    }
    val focusRequester = remember { FocusRequester() }
    val armedFocusRequester = remember { FocusRequester() }
    val inputSurfaceInteractionSource = remember { MutableInteractionSource() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var focusRequestSerial by remember { mutableIntStateOf(0) }
    var focusRequestShowsKeyboard by remember { mutableStateOf(true) }
    var textFieldPlaced by remember { mutableStateOf(false) }
    var armedTargetPlaced by remember { mutableStateOf(false) }

    fun refocus(showSoftwareKeyboard: Boolean = true) {
        focusRequestShowsKeyboard = showSoftwareKeyboard
        focusRequestSerial += 1
    }

    fun chooseMessagingApp(draft: MessageDraft) {
        val opened = actions.chooseMessagingApp(draft.body)
        if (!opened) {
            Toast.makeText(
                context,
                context.getString(R.string.no_compatible_messaging_app),
                Toast.LENGTH_LONG,
            ).show()
            refocus()
        }
    }

    fun openMessagingProvider(draft: MessageDraft, providerPackage: String?) {
        val result = actions.openPreferredMessageDraft(
            draft.recipient,
            draft.body,
            providerPackage,
        )
        when (result) {
            PreferredMessageDraftResult.OPENED -> Unit
            PreferredMessageDraftResult.OPENED_WITH_RECIPIENT_PICKER -> Toast.makeText(
                context,
                context.getString(
                    R.string.choose_recipient_in_preferred_app,
                    draft.recipient.displayName,
                ),
                Toast.LENGTH_LONG,
            ).show()
            PreferredMessageDraftResult.FALLBACK_OPENED -> Toast.makeText(
                context,
                context.getString(R.string.preferred_messaging_fallback),
                Toast.LENGTH_LONG,
            ).show()
            PreferredMessageDraftResult.FAILED -> Toast.makeText(
                context,
                context.getString(R.string.no_compatible_messaging_app),
                Toast.LENGTH_LONG,
            ).show()
        }
        if (result == PreferredMessageDraftResult.FAILED) refocus()
    }
    val mediaPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasMediaAccess = hasMediaReadAccess(context)
        if (!hasMediaAccess && mediaPermissionPermanentlyDenied(context)) actions.openAppSettings()
        showFileScopeChoice = true
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            store.addSearchFolder(it.toString(), fileSearchRepository.folderLabel(it))
            fileSearchRepository.invalidateFolders()
        }
    }
    val magicResultsScroll = rememberScrollState()
    val parsedInput = parseMagicBoxInput(text.text, lockedPrefix)
    val prefix = parsedInput.prefix
    val searchTerm = parsedInput.searchTerm
    val canSearchContacts = hasContacts || store.demoSearchDataEnabled
    val contactResults = remember(prefix, searchTerm, canSearchContacts, store.demoSearchDataEnabled) {
        if (
            prefix in listOf('@', '#') &&
            canSearchContacts &&
            selectedRecipient == null
        ) {
            actions.searchContacts(searchTerm, useDemoData = store.demoSearchDataEnabled).take(5)
        } else emptyList()
    }
    val recentConversations = rememberRecentConversationResults(
        prefix = prefix,
        query = searchTerm,
        hasSelectedRecipient = selectedRecipient != null,
        launcherShortcutsRevision = launcherShortcutsRevision,
        sendAutomatically = store.sendMessagesAutomatically,
        preferredMessagingPackage = store.preferredMessagingPackage,
        demoModeEnabled = store.demoSearchDataEnabled,
        actions = actions,
    )
    val visibleRecentConversations = remember(recentConversations, effectiveAppAccessState) {
        if (effectiveAppAccessState.isResolved) {
            recentConversations.filterNot(effectiveAppAccessState::isPaused)
        } else {
            emptyList()
        }
    }
    val alwaysVisibleTargetKeys = store.pinnedLauncherSelectionKeys
    val appResults = remember(
        prefix,
        searchTerm,
        launcherAppsRevision,
        launcherShortcutsRevision,
        alwaysVisibleTargetKeys,
        store.includeAppShortcutsInDiscovery,
    ) {
        if (prefix == '?' && searchTerm.isNotBlank()) {
            launcherLibraryTargets(
                apps = actions.installedApps(),
                alwaysVisibleTargetKeys = alwaysVisibleTargetKeys,
                appShortcuts = actions.installedShortcuts(),
                includeAppShortcuts = store.includeAppShortcutsInDiscovery,
                resolveTarget = actions::resolveLauncherSelection,
            ).filter {
                launcherDiscoveryMatches(it, searchTerm, actions::appLabel)
            }.take(5)
        } else emptyList()
    }
    val visibleAppResults = remember(appResults, effectiveAppAccessState) {
        if (effectiveAppAccessState.isResolved) {
            appResults.filterNot(effectiveAppAccessState::isPaused)
        } else {
            emptyList()
        }
    }
    val plainQuery = parsedInput.plainQuery
    val noteMode = expanded && prefix == MAGIC_NOTE_PREFIX
    val hasTextDraft = expanded && hasMagicBoxDraftText(text.text, lockedPrefix)
    val hasNoteDraft = noteMode && hasTextDraft
    val hasMessageDraft = expanded &&
        lockedPrefix == '@' &&
        selectedRecipient != null &&
        text.text.isNotBlank()
    val fileSearchState = rememberMagicFileSearchState(
        query = plainQuery,
        store = store,
        repository = fileSearchRepository,
        hasMediaAccess = hasMediaAccess,
        resultsScroll = magicResultsScroll,
    )
    val actionVisuals = when (prefix) {
        '@' -> MagicActionVisuals(MagicTextColor, Icons.AutoMirrored.Filled.Send)
        '#' -> MagicActionVisuals(MagicCallColor, Icons.Default.Phone)
        '-' -> MagicActionVisuals(MagicTodoColor, Icons.Default.Checklist)
        MAGIC_NOTE_PREFIX -> MagicActionVisuals(MagicNoteColor, Icons.AutoMirrored.Filled.NoteAdd)
        '+' -> MagicActionVisuals(MagicEventColor, Icons.Default.Event)
        '?' -> MagicActionVisuals(MagicAppColor, Icons.Default.Apps)
        else -> MagicActionVisuals(
            MaterialTheme.colorScheme.primary,
            Icons.Default.Search,
        )
    }
    val actionContentColor = when (prefix) {
        '-' -> LightInk
        null -> MaterialTheme.colorScheme.onPrimary
        else -> MinkWhite
    }
    fun clearCommand() {
        fileSearchState.clear()
        initialHardwareKeyCorrection.clear()
        text = TextFieldValue()
        selectedRecipient = null
        lockedPrefix = null
    }

    fun requestClearMessageDraft() {
        if (text.text.isBlank()) {
            clearCommand()
            refocus()
        } else {
            keyboard?.hide()
            showMessageDiscardConfirmation = true
        }
    }

    fun dismiss() {
        clearCommand()
        keyboard?.hide()
        expanded = false
        onExpandedChange(false)
        onSessionComplete()
    }

    fun requestDismiss() {
        when {
            hasNoteDraft -> {
                keyboard?.hide()
                showNoteDeleteConfirmation = true
            }
            hasTextDraft -> {
                keyboard?.hide()
                showCommandDiscardConfirmation = true
            }
            else -> dismiss()
        }
    }

    fun collapseForDialog() {
        clearCommand()
        keyboard?.hide()
        expanded = false
        onExpandedChange(false)
    }

    fun completeAiHandoff(
        query: String,
        packageName: String,
        result: AiHandoffResult,
    ) {
        if (result == AiHandoffResult.COPIED_AND_OPENED) {
            Toast.makeText(
                context,
                context.getString(
                    R.string.ai_query_copied_paste_in_app,
                    actions.appLabel(packageName),
                ),
                Toast.LENGTH_LONG,
            ).show()
        }
        store.addSearchQuery(query)
        dismiss()
    }

    fun submitAi() {
        val query = plainQuery
        if (query.isBlank()) return
        val preferredPackage = store.preferredAiPackage
        if (!preferredPackage.isNullOrBlank()) {
            val result = actions.handoffQueryToAiApp(query, preferredPackage)
            if (result.succeeded) {
                completeAiHandoff(query, preferredPackage, result)
                return
            }
            store.resetPreferredAiApp()
        }
        pendingAiQuery = query
        showAiPicker = true
        keyboard?.hide()
    }

    fun selectAiPackage(packageName: String) {
        val query = pendingAiQuery ?: return
        val result = actions.handoffQueryToAiApp(query, packageName)
        if (!result.succeeded) return

        store.setPreferredAiApp(packageName)
        showAiPicker = false
        showAllAiApps = false
        pendingAiQuery = null
        completeAiHandoff(query, packageName, result)
    }

    fun selectAiApp(app: LaunchableApp) {
        selectAiPackage(app.packageName)
    }

    fun dismissAiPickers() {
        showAiPicker = false
        showAllAiApps = false
        pendingAiQuery = null
        refocus()
    }

    fun submit() {
        dispatchMagicCommand(
            prefix = prefix,
            lockedPrefix = lockedPrefix,
            rawText = text.text,
            selectedRecipient = selectedRecipient,
            store = store,
            actions = actions,
            onTodoAdded = onTodoAdded,
            onExternalDraftOpened = { keyboard?.hide() },
            onMessage = { draft, route ->
                val resolvedRoute = resolvedMessageSendRoute(
                    recipient = draft.recipient,
                    selectedConversationPackage = draft.conversationPackage,
                    defaultSmsPackage = actions.defaultMessagingPackage(),
                    sendAutomatically = store.sendMessagesAutomatically,
                    preferredPackage = store.preferredMessagingPackage,
                    fallbackRoute = route,
                )
                when (resolvedRoute) {
                    MessagingSendRoute.DIRECT_SMS -> {
                        collapseForDialog()
                        smsFlow.requestDirect(draft)
                    }
                    // A composer can be dismissed without sending. Keep the contact and body
                    // in Magic Mode just as note handoffs keep their unsaved text.
                    MessagingSendRoute.PREFERRED_DRAFT -> {
                        keyboard?.hide()
                        openMessagingProvider(
                            draft,
                            draft.conversationPackage ?: store.preferredMessagingPackage,
                        )
                    }
                    MessagingSendRoute.PROVIDER_PICKER -> {
                        keyboard?.hide()
                        pendingMessagingChoice = draft
                    }
                }
            },
            onConversationShortcutMessage = { draft ->
                keyboard?.hide()
                handleRecentConversationDraftSend(context, draft, actions) { refocus() }
            },
            onDismiss = ::dismiss,
        )
    }

    // Keep collapsed Home armed without focusing a text editor. This lets a physical
    // keyboard open Magic Mode with its first printable key without giving keyboard apps
    // an input connection (and therefore a suggestion toolbar) before typing begins.
    // The Home-keyboard preference still focuses the real field for touch-first devices.
    LaunchedEffect(
        keyboardInputEnabled,
        expanded,
        smsFlow.sentConfirmationVisible,
        showKeyboardWhileCollapsed,
    ) {
        if (!keyboardInputEnabled) {
            keyboard?.hide()
            focusManager.clearFocus(force = true)
        } else if (!expanded && !smsFlow.sentConfirmationVisible) {
            if (showKeyboardWhileCollapsed) {
                while (!textFieldPlaced) withFrameNanos { }
                withFrameNanos { }
                focusRequester.requestFocus()
                withFrameNanos { }
                keyboard?.show()
            } else {
                while (!armedTargetPlaced) withFrameNanos { }
                withFrameNanos { }
                armedFocusRequester.requestFocus()
                keyboard?.hide()
            }
        } else {
            armedTargetPlaced = false
            if (expanded) showKeyboardWhileCollapsed = false
            if (expanded && initiallyExpanded && focusRequestSerial == 0) {
                while (!textFieldPlaced) withFrameNanos { }
                withFrameNanos { }
                focusRequester.requestFocus()
                withFrameNanos { }
                if (showSoftwareKeyboardOnStart) keyboard?.show() else keyboard?.hide()
                onExpandedChange(true)
            }
        }
    }

    LaunchedEffect(homeRequestToken, shouldAutoOpenOnHome, keyboardInputEnabled) {
        if (homeRequestToken > 0 && shouldAutoOpenOnHome && keyboardInputEnabled) {
            clearCommand()
            expanded = false
            onExpandedChange(false)
            showKeyboardWhileCollapsed = true
        }
    }

    LaunchedEffect(shouldAutoOpenOnHome) {
        showKeyboardWhileCollapsed = shouldAutoOpenOnHome && !expanded
    }

    LaunchedEffect(focusRequestSerial) {
        if (focusRequestSerial > 0) {
            // TextField focus can request bring-into-view. Wait until its newly docked parent
            // has completed placement before requesting focus or showing the IME.
            while (!textFieldPlaced) withFrameNanos { }
            withFrameNanos { }
            focusRequester.requestFocus()
            if (focusRequestShowsKeyboard) {
                withFrameNanos { }
                keyboard?.show()
            }
        }
    }

    BackHandler(enabled = expanded) { requestDismiss() }

    Box(modifier) {
        if (expanded) {
            Box(Modifier.matchParentSize().background(MinkBlack.copy(alpha = .48f)).clickable(onClick = { requestDismiss() }))
        }

        Column(
            modifier = Modifier
                .matchParentSize()
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = Dimens.dp18, vertical = Dimens.dp18),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
        ) {
            if (!noteMode) {
                if (expanded) {
                    MagicResultsPanel(
                        store = store,
                        actions = actions,
                        rawText = text.text,
                        lockedPrefix = lockedPrefix,
                        prefix = prefix,
                        launcherAppsRevision = launcherAppsRevision,
                        launcherShortcutsRevision = launcherShortcutsRevision,
                        plainQuery = plainQuery,
                        fileSearchLoading = fileSearchState.loading,
                        fileResults = fileSearchState.results,
                        fileSearchRepository = fileSearchRepository,
                        hasMediaAccess = hasMediaAccess,
                        canSearchContacts = canSearchContacts,
                        contactResults = contactResults,
                        recentConversations = visibleRecentConversations,
                        forcedRecipient = searchTerm.takeIf {
                            prefix in listOf('@', '#') &&
                                it.isNotBlank() &&
                                selectedRecipient == null
                        },
                        appResults = visibleAppResults,
                        includeAppShortcuts = store.includeAppShortcutsInDiscovery,
                        showClearMessage = hasMessageDraft,
                        scrollState = magicResultsScroll,
                        onSelectHistory = { query ->
                            text = TextFieldValue(query, selection = TextRange(query.length))
                            refocus()
                        },
                        onOpenFile = { file ->
                            store.addSearchQuery(plainQuery)
                            actions.openFile(file)
                            dismiss()
                        },
                        onRequestMedia = { mediaPermissionLauncher.launch(mediaReadPermissions()) },
                        onRequestFolder = { showFileScopeChoice = true },
                        onSubmitWeb = ::submit,
                        onSubmitAi = ::submitAi,
                        onSelectContact = { contact ->
                            if (prefix == '#') {
                                callFlow.requestConfirmation(contact.toCommunicationRecipient())
                                collapseForDialog()
                            } else {
                                lockedPrefix = prefix
                                selectedRecipient = SelectedMessageRecipient.AddressRecipient(
                                    contact.toCommunicationRecipient(),
                                )
                                text = TextFieldValue()
                                refocus()
                            }
                        },
                        onSelectRecentConversation = { conversation ->
                            handleRecentConversationTap(
                                context = context,
                                shortcut = conversation,
                                canSearchContacts = canSearchContacts,
                                actions = actions,
                                onShortcutDraft = { shortcut ->
                                    lockedPrefix = '@'
                                    selectedRecipient =
                                        SelectedMessageRecipient.ConversationRecipient(shortcut)
                                    text = TextFieldValue()
                                    refocus()
                                },
                                onDraftRecipient = { contact, packageName ->
                                    lockedPrefix = '@'
                                    selectedRecipient = SelectedMessageRecipient.AddressRecipient(
                                        recipient = contact.toCommunicationRecipient(),
                                        providerPackage = packageName,
                                    )
                                    text = TextFieldValue()
                                    refocus()
                                },
                                onDirectOpen = ::dismiss,
                            )
                        },
                        onSelectForcedRecipient = { identifier ->
                            val recipient = userEnteredRecipient(identifier)
                            if (prefix == '#') {
                                callFlow.requestConfirmation(recipient)
                                collapseForDialog()
                            } else {
                                lockedPrefix = '@'
                                selectedRecipient = SelectedMessageRecipient.AddressRecipient(
                                    recipient = recipient,
                                    providerPackage = automaticMessagingPackage(
                                        sendAutomatically = store.sendMessagesAutomatically,
                                        preferredPackage = store.preferredMessagingPackage,
                                        defaultMessagingPackage = actions.defaultMessagingPackage(),
                                    ),
                                )
                                text = TextFieldValue()
                                refocus()
                            }
                        },
                        onSelectApp = { app ->
                            if (actions.launchLauncherTarget(app)) {
                                store.addSearchQuery("?${app.label}")
                                dismiss()
                            } else {
                                val displayLabel = launcherDiscoveryLabel(app, actions::appLabel)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.launcher_app_unavailable, displayLabel),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        },
                        onIncludeAppShortcutsChange = store::updateIncludeAppShortcutsInDiscovery,
                        onRequestContacts = {
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        },
                        onClearMessage = ::requestClearMessageDraft,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                } else {
                    Spacer(Modifier.fillMaxWidth().weight(1f))
                }
            }

            if (!noteMode && expanded && text.text.isBlank() && lockedPrefix == null) {
                MagicBoxLegend(prefix, enabled = true) { key ->
                    clearCommand()
                    text = TextFieldValue(key.toString(), selection = TextRange(1))
                    refocus()
                }
            }

            MagicEditorSurface(
                noteMode = noteMode,
                text = text,
                selectedRecipientLabel = selectedRecipient?.label,
                selectedRecipientDetail = selectedRecipient?.detail(actions::appLabel),
                selectedRecipientLeadingIcon = selectedRecipient?.providerPackage?.let { packageName ->
                    {
                        DrawableIcon(
                            drawable = actions.appIcon(packageName),
                            iconKey = "selected-conversation:$packageName:$launcherAppsRevision",
                            size = Dimens.dp18,
                        )
                    }
                },
                prefix = prefix,
                actionVisuals = actionVisuals,
                actionContentColor = actionContentColor,
                focusRequester = focusRequester,
                interactionSource = inputSurfaceInteractionSource,
                onRefocus = { refocus(showSoftwareKeyboard = !useDirectHardwareInput) },
                onTextChange = { value ->
                    val correctedValue = initialHardwareKeyCorrection.correct(value)
                    text = correctedValue
                    if (!expanded && correctedValue.text.isNotEmpty()) {
                        expanded = true
                        onExpandedChange(true)
                    }
                    if (!noteMode && lockedPrefix == null && correctedValue.text.firstOrNull() != prefix) {
                        selectedRecipient = null
                    }
                },
                onPlaced = { textFieldPlaced = true },
                onSubmit = ::submit,
                onHardwareKeyUp = initialHardwareKeyCorrection::onHardwareKeyUp,
                onClearMessage = ::requestClearMessageDraft,
                onDeleteNote = {
                    keyboard?.hide()
                    showNoteDeleteConfirmation = true
                },
                visible = expanded,
                modifier = Modifier.fillMaxWidth().then(
                    if (noteMode) Modifier.weight(1f) else Modifier.heightIn(min = magicBoxMinimumHeight),
                ),
            )
        }

        if (!expanded && !smsFlow.sentConfirmationVisible) {
            CollapsedMagicBar(
                modifier = collapsedModifier.align(Alignment.BottomCenter),
                minimumHeight = magicBoxMinimumHeight,
                armedFocusRequester = armedFocusRequester,
                onArmedPlaced = { armedTargetPlaced = true },
                onPrintableKeyDown = { typedText, keyCode ->
                    if (!expanded) {
                        clearCommand()
                        initialHardwareKeyCorrection.begin(typedText, keyCode)
                        text = TextFieldValue(typedText, selection = TextRange(typedText.length))
                        expanded = true
                        onExpandedChange(true)
                        refocus(showSoftwareKeyboard = false)
                    } else {
                        val updatedText = text.text + typedText
                        text = TextFieldValue(updatedText, selection = TextRange(updatedText.length))
                    }
                },
                onOpen = {
                    clearCommand()
                    expanded = true
                    onExpandedChange(true)
                    refocus(showSoftwareKeyboard = true)
                },
            )
        }

        SmsSentConfirmation(
            visible = smsFlow.sentConfirmationVisible,
            modifier = Modifier.align(Alignment.Center),
        )
    }
    MagicDraftDialogHost(
        showNoteDelete = showNoteDeleteConfirmation,
        showMessageDiscard = showMessageDiscardConfirmation,
        showCommandDiscard = showCommandDiscardConfirmation,
        showFileScopeChoice = showFileScopeChoice,
        showSmsAssistantDisclosure = smsFlow.assistantDisclosureVisible,
        onDeleteNote = {
            showNoteDeleteConfirmation = false
            dismiss()
        },
        onKeepNote = {
            showNoteDeleteConfirmation = false
            refocus()
        },
        onDiscardMessage = {
            showMessageDiscardConfirmation = false
            clearCommand()
            refocus()
        },
        onKeepMessage = {
            showMessageDiscardConfirmation = false
            refocus()
        },
        onDiscardCommand = {
            showCommandDiscardConfirmation = false
            dismiss()
        },
        onKeepCommand = {
            showCommandDiscardConfirmation = false
            refocus()
        },
        onChooseFolder = {
            showFileScopeChoice = false
            folderPicker.launch(null)
        },
        onSkipFolder = { showFileScopeChoice = false },
        onAssistantContinue = smsFlow::continueAssistantSelection,
        onAssistantDismiss = smsFlow::dismissAssistantDisclosure,
    )

    MagicPickerDialogHost(
        messageDraft = pendingMessagingChoice,
        messagingOptions = messagingProviders.options,
        messagingOptionsLoaded = messagingProviders.loaded,
        preferredMessagingPackage = store.preferredMessagingPackage,
        showAiPicker = showAiPicker,
        showAllAiApps = showAllAiApps,
        aiProviders = aiProviders,
        aiProvidersLoaded = aiProvidersLoaded,
        allAiApps = allAiApps,
        allAiAppsLoaded = allAiAppsLoaded,
        preferredAiPackage = store.preferredAiPackage,
        onMessagingProvider = { draft, option ->
            pendingMessagingChoice = null
            openMessagingProvider(
                draft,
                providerPackage = if (option.systemDefault) null else option.preferencePackageName,
            )
        },
        onSeeAllMessagingApps = { draft ->
            pendingMessagingChoice = null
            chooseMessagingApp(draft)
        },
        onDismissMessaging = {
            pendingMessagingChoice = null
            refocus()
        },
        onSeeAllAiApps = {
            showAiPicker = false
            showAllAiApps = true
        },
        onAiProvider = { option ->
            option.installedPackageName?.let(::selectAiPackage)
        },
        onInstallAiProvider = { actions.openAiProviderInstallPage(it) },
        onDismissCuratedAi = ::dismissAiPickers,
        onAllAiApp = ::selectAiApp,
        onDismissAllAi = ::dismissAiPickers,
    )

    MagicCommunicationDialogHost(
        callRecipient = callFlow.recipientToConfirm,
        smsDraft = smsFlow.draftToConfirm,
        assistantActive = actions.isAssistantRoleHeld(),
        onCallNow = { callFlow.callNow() },
        onChooseCallingApp = { callFlow.chooseCallingApp() },
        onDismissCall = callFlow::dismiss,
        onSendSms = { smsFlow.confirmDraft() },
        onChooseMessagingApp = { smsFlow.chooseMessagingApp { pendingMessagingChoice = it } },
        onDismissSms = { smsFlow.dismissDraft(onSessionComplete) },
    )
}
